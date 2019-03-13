"""
We represent values in this module at three levels:

* A GRPC ``Value`` message.
* An *intermediate level* with a dict with ``type`` and ``value`` fields
  where ``type`` can be one of the ``object``, ``dataset_uri``, ``csv_uri``,
  ``pickle_uri``, ``plasma_id``, ``error``. All values except for ``object``
  type are strings. Values for ``object`` type are raw Python values.
* A raw Python value.

One can use `decode_value` to convert from a GRPC ``Value`` message to
the value at the *intermediate level*. And `encode_value` to convert
from a value at the *intermediate level* to the GRPC ``Value`` message.

One can use `load_value` to convert from a value at the *intermediate level*
to the raw Python value. And `save_value` to convert from a ray Python value
to the value at the *intermediate level*.

The reason for three levels is that sometimes you want to pass a value
around Python codebase without loading the whole value into the memory.
So conversion from and to GRPC API can be done at the different place
than loading and saving raw Python values.
"""

import binascii
import datetime
import logging
import os.path
import pickle
import shutil
import sys
import tempfile
import uuid
from urllib import parse as url_parse

import frozendict
import pandas
from google.protobuf import timestamp_pb2

from d3m import container, exceptions, runtime as runtime_module, utils as d3m_utils
from d3m.container import dataset as dataset_module
from d3m.metadata import base as metadata_base, pipeline as pipeline_module, problem as problem_module

from . import core_pb2
from . import pipeline_pb2
from . import primitive_pb2
from . import problem_pb2
from . import value_pb2

logger = logging.getLogger(__name__)

MAX_WIRE_OBJECT_SIZE = 64 * 1024  # bytes


def _hex_to_binary(hex_identifier):
    return binascii.unhexlify(hex_identifier)


def _binary_to_hex(identifier):
    hex_identifier = binascii.hexlify(identifier)
    return hex_identifier.decode()


class ValueType(d3m_utils.Enum):
    """
    Enumeration of possible value types.

    Values are kept in sync with TA2-TA3 API's ``ValueType`` enumeration.
    """

    RAW = 1
    DATASET_URI = 2
    CSV_URI = 3
    PICKLE_URI = 4
    PICKLE_BLOB = 5
    PLASMA_ID = 6
    LARGE_RAW = 7
    LARGE_PICKLE_BLOB = 8


def _can_encode_raw(value):
    """
    Can the value be encoded as raw GRPC value for TA2-TA3 API?

    Parameters
    ----------
    value : Any
        Value to try to encode as raw GRPC value.

    Returns
    -------
    bool
        ``True`` if the value can be encoded as raw GRPC value.
    """

    try:
        encode_raw_value(value)
        return True
    except Exception:
        return False


def _can_pickle(value):
    """
    Can the value be pickled?

    Parameters
    ----------
    value : Any
        Value to try to pickle.

    Returns
    -------
    bool
        ``True`` if the value can be pickled.
    """

    try:
        pickle.dumps(value)
        return True
    except Exception:
        return False


def _fix_uri(uri):
    """
    Make a real URI from an absolute path. Some clients do not use proper URIs.

    Parameters
    ----------
    uri : str
        An input URI.

    Returns
    -------
    str
        A fixed URI.
    """

    if not uri.startswith('file://') and uri.startswith('/'):
        uri = 'file://{uri}'.format(uri=uri)

    return uri


def _open_file_uri(uri, mode='r', encoding=None, validate_uri=None):
    """
    Opens a file URI and returns a file object.

    Parameters
    ----------
    uri : str
        A file URI to the file.
    mode : str
        Mode to open a file with.
    encoding : str
        Encoding to use.

    Returns
    -------
    File
        A file object.
    """

    if validate_uri is not None:
        validate_uri(uri)

    parsed_uri = url_parse.urlparse(uri)

    return open(parsed_uri.path, mode=mode, encoding=encoding)


def _fix_file_uri_host(uri):
    """
    If ``uri`` is a file URI, make sure that it has a host set to ``localhost``.

    Parameters
    ----------
    uri : str
        URI to fix.

    Returns
    -------
    str
        Fixed URI.
    """

    parsed_uri = url_parse.urlparse(uri)

    if parsed_uri.scheme == 'file' and parsed_uri.netloc == '':
        parsed_uri = parsed_uri._replace(netloc='localhost')
        uri = url_parse.urlunparse(parsed_uri)

    return uri


def validate_uri(uri, data_directories=None):
    """
    Validates that the URI is a local file URI and that it points inside
    a directory listed in ``data_directories``.

    It raises an exception if validation fails.

    Parameters
    ----------
    uri : str
        URI to validate.
    data_directories : Sequence[str]
        A list of directories under which local URIs are allowed to be.
    """

    try:
        parsed_uri = url_parse.urlparse(uri)
    except Exception as error:
        raise exceptions.InvalidArgumentValueError("Invalid URI.") from error

    if parsed_uri.scheme != 'file':
        raise exceptions.InvalidArgumentValueError(
            "Invalid URI scheme: {scheme}".format(
                scheme=parsed_uri.scheme,
            ),
        )

    if parsed_uri.netloc not in ['', 'localhost']:
        raise exceptions.InvalidArgumentValueError(
            "Invalid URI location: {netloc}".format(
                netloc=parsed_uri.netloc,
            ),
        )

    if not parsed_uri.path.startswith('/'):
        raise exceptions.InvalidArgumentValueError(
            "Invalid URI path: {path}".format(
                path=parsed_uri.path,
            ),
        )

    normalized_path = os.path.normpath(parsed_uri.path)

    if data_directories is not None:
        if not any(os.path.commonpath([normalized_path, data_directory]) == data_directory for data_directory in data_directories):
            raise exceptions.InvalidArgumentValueError(
                "URI '{uri}' (path '{normalized_path}') outside data directories: {data_directories}".format(
                    uri=uri,
                    normalized_path=normalized_path,
                    data_directories=data_directories,
                ),
            )


def encode_primitive(primitive):
    """
    Encodes a primitive into a GRPC message.

    Parameters
    ----------
    primitive : Type[PrimitiveBase]
        A primitive class.

    Returns
    -------
    Primitive
        A GRPC message.
    """

    metadata = primitive.metadata.query()

    return primitive_pb2.Primitive(
        id=metadata['id'],
        version=metadata['version'],
        python_path=metadata['python_path'],
        name=metadata['name'],
        digest=metadata.get('digest', None),
    )


def encode_primitive_description(primitive_description):
    """
    Encodes a primitive description into a GRPC message.

    Parameters
    ----------
    primitive_description : Dict
        A primitive description.

    Returns
    -------
    Primitive
        A GRPC message.
    """

    return primitive_pb2.Primitive(
        id=primitive_description['id'],
        version=primitive_description['version'],
        python_path=primitive_description['python_path'],
        name=primitive_description['name'],
        digest=primitive_description.get('digest', None),
    )


def decode_primitive(primitive):
    """
    Decodes a GRPC message into a primitive description.

    Parameters
    ----------
    primitive : Primitive
        A GRPC message.

    Returns
    -------
    Dict
        A primitive description.
    """

    primitive_dict = {
        'id': primitive.id,
        'version': primitive.version,
        'python_path': primitive.python_path,
        'name': primitive.name,
    }

    if primitive.digest:
        primitive_dict['digest'] = primitive.digest

    return primitive_dict


def encode_problem_description(problem_description):
    """
    Encodes a problem description into a GRPC message.

    Parameters
    ----------
    problem_description : Dict
        A problem description.

    Returns
    -------
    ProblemDescription
        A GRPC message.
    """

    performance_metrics = []
    for performance_metric in problem_description['problem'].get('performance_metrics', []):
        performance_metrics.append(encode_performance_metric(performance_metric))

    problem = problem_pb2.Problem(
        # TODO: Remove deprecated fields in a future version.
        id=problem_description['problem'].get('id', None),
        version=problem_description['problem'].get('version', None),
        name=problem_description['problem'].get('name', None),
        description=problem_description['problem'].get('description', None),
        task_type=problem_description['problem']['task_type'].value,
        task_subtype=problem_description['problem']['task_subtype'].value,
        performance_metrics=performance_metrics,
    )

    inputs = []
    for problem_input in problem_description.get('inputs', []):
        targets = []
        for target in problem_input.get('targets', []):
            targets.append(
                problem_pb2.ProblemTarget(
                    target_index=target['target_index'],
                    resource_id=target['resource_id'],
                    column_index=target['column_index'],
                    column_name=target['column_name'],
                    clusters_number=target.get('clusters_number', None),
                ),
            )

        inputs.append(
            problem_pb2.ProblemInput(dataset_id=problem_input['dataset_id'], targets=targets),
        )

    data_augmentation = []
    for data in problem_description.get('data_augmentation', []):
        if data.get('domain', []) or data.get('keywords', []):
            problem_pb2.DataAugmentation(
                domain=data.get('domain', []),
                keywords=data.get('keywords', []),
            )

    return problem_pb2.ProblemDescription(
        problem=problem,
        inputs=inputs,
        id=problem_description['id'],
        version=problem_description.get('version', None),
        name=problem_description.get('name', None),
        description=problem_description.get('description', None),
        digest=problem_description.get('digest', None),
        data_augmentation=data_augmentation,
    )


def decode_problem_description(problem_description, *, strict_digest=False):
    """
    Decodes a GRPC message into a problem description.

    Parameters
    ----------
    problem_description : ProblemDescription
        A GRPC message.
    strict_digest : bool
        If computed digest does not match the one provided in message, raise an exception?

    Returns
    -------
    Union[Dict, None]
        A problem description, or ``None`` if problem is not defined.
    """

    if problem_description.problem.task_type == problem_pb2.TaskType.Value('TASK_TYPE_UNDEFINED') and problem_description.problem.task_subtype == problem_pb2.TaskSubtype.Value('TASK_SUBTYPE_UNDEFINED'):
        return None

    description = {
        # TODO: Remove deprecated fields in a future version.
        'id': problem_description.id or problem_description.problem.id,
        'version': problem_description.version or problem_description.problem.version,
        'name': problem_description.name or problem_description.problem.name,
        'schema': problem_module.PROBLEM_SCHEMA_VERSION,
        'problem': {
            'task_type': problem_module.TaskType(problem_description.problem.task_type),
            'task_subtype': problem_module.TaskSubtype(problem_description.problem.task_subtype),
        },
    }

    if problem_description.description:
        description['description'] = problem_description.description
    # TODO: Remove deprecated fields in a future version.
    elif problem_description.problem.description:
        description['description'] = problem_description.problem.description

    if problem_description.data_augmentation:
        description['data_augmentation'] = []

        for data in problem_description.data_augmentation:
            if data.domain or data.keywords:
                description['data_augmentation'].append({})

                if data.domain:
                    description['data_augmentation'][-1]['domain'] = data.domain

                if data.keywords:
                    description['data_augmentation'][-1]['keywords'] = data.keywords

        if not description['data_augmentation']:
            del description['data_augmentation']

    performance_metrics = []
    for performance_metric in problem_description.problem.performance_metrics:
        performance_metrics.append(decode_performance_metric(performance_metric))

    if performance_metrics:
        description['problem']['performance_metrics'] = performance_metrics

    inputs = []
    for problem_input in problem_description.inputs:
        targets = []
        for target in problem_input.targets:
            targets.append(
                {
                    'target_index': target.target_index,
                    'resource_id': target.resource_id,
                    'column_index': target.column_index,
                    'column_name': target.column_name,
                },
            )

            if target.clusters_number:
                targets[-1]['clusters_number'] = target.clusters_number

        problem_input = {
            'dataset_id': problem_input.dataset_id,
        }

        if targets:
            problem_input['targets'] = targets

        inputs.append(problem_input)

    if inputs:
        description['inputs'] = inputs

    description['digest'] = d3m_utils.compute_digest(d3m_utils.to_json_structure(description))

    problem_module.PROBLEM_SCHEMA_VALIDATOR.validate(description)

    if problem_description.digest:
        if description['digest'] != problem_description.digest:
            if strict_digest:
                raise exceptions.DigestMismatchError(
                    "Digest for problem '{problem_id}' does not match a computed one. Provided digest: {problem_digest}. Computed digest: {new_problem_digest}.".format(
                        problem_id=problem_description.id,
                        problem_digest=problem_description.digest,
                        new_problem_digest=description['digest'],
                    )
                )
            else:
                logger.warning(
                    "Digest for problem '%(problem_id)s' does not match a computed one. Provided digest: %(problem_digest)s. Computed digest: %(new_problem_digest)s.",
                    {
                        'problem_id': problem_description.id,
                        'problem_digest': problem_description.digest,
                        'new_problem_digest': description['digest'],
                    },
                )

    return description


def encode_pipeline_description(pipeline, allowed_value_types, scratch_dir, *, plasma_put=None, validate_uri=None):
    """
    Encodes a pipeline into a GRPC message.

    Parameters
    ----------
    pipeline : Pipeline
        A pipeline instance. Primitives do not have to be resolved. Sub-pipelines should be nested.
    allowed_value_types : Sequence[ValueType]
        A list of allowed value types to encode this value as. This
        list is tried in order until encoding succeeds.
    scratch_dir : str
        Path to a directory to store any temporary files needed during execution.
    plasma_put : Callable
        A function to store a value into a Plasma store.
        The function takes a value to store into Plasma store and should return
        stored object's ID as bytes.
    validate_uri : Callable
        A function which can validate that URI is a valid and supported file URI.
        The function takes an URI as a string and should throw an exception if URI is invalid.

    Returns
    -------
    PipelineDescription
        A GRPC message.
    """

    if pipeline.source is not None:
        source = pipeline_pb2.PipelineSource(
            name=pipeline.source.get('name', None),
            contact=pipeline.source.get('contact', None),
            pipelines=[p['id'] for p in pipeline.source.get('from', {}).get('pipelines', []) if pipeline.source.get('from', {}).get('type', None) == 'PIPELINE'],
        )
    else:
        source = None

    steps = []
    for step in pipeline.steps:
        if isinstance(step, pipeline_module.PrimitiveStep):
            arguments = {}
            for name, argument in step.arguments.items():
                if argument['type'] == metadata_base.ArgumentType.CONTAINER:
                    if d3m_utils.is_sequence(argument['data']):
                        arguments[name] = pipeline_pb2.PrimitiveStepArgument(
                            container_list=pipeline_pb2.ContainerArguments(
                                data=argument['data'],
                            ),
                        )
                    else:
                        arguments[name] = pipeline_pb2.PrimitiveStepArgument(
                            container=pipeline_pb2.ContainerArgument(
                                data=argument['data'],
                            ),
                        )
                elif argument['type'] == metadata_base.ArgumentType.DATA:
                    assert not d3m_utils.is_sequence(argument['data']), type(argument['data'])
                    arguments[name] = pipeline_pb2.PrimitiveStepArgument(
                        data=pipeline_pb2.DataArgument(
                            data=argument['data'],
                        ),
                    )
                else:
                    raise exceptions.UnexpectedValueError("Unknown argument type: {argument_type}".format(argument_type=argument['type']))

            hyperparams = {}
            for name, hyperparameter in step.hyperparams.items():
                if hyperparameter['type'] == metadata_base.ArgumentType.CONTAINER:
                    hyperparams[name] = pipeline_pb2.PrimitiveStepHyperparameter(
                        container=pipeline_pb2.ContainerArgument(
                            data=hyperparameter['data'],
                        ),
                    )
                elif hyperparameter['type'] == metadata_base.ArgumentType.DATA:
                    if d3m_utils.is_sequence(hyperparameter['data']):
                        hyperparams[name] = pipeline_pb2.PrimitiveStepHyperparameter(
                            data_set=pipeline_pb2.DataArguments(
                                data=hyperparameter['data'],
                            ),
                        )
                    else:
                        hyperparams[name] = pipeline_pb2.PrimitiveStepHyperparameter(
                            data=pipeline_pb2.DataArgument(
                                data=hyperparameter['data'],
                            ),
                        )
                elif hyperparameter['type'] == metadata_base.ArgumentType.PRIMITIVE:
                    if d3m_utils.is_sequence(hyperparameter['data']):
                        hyperparams[name] = pipeline_pb2.PrimitiveStepHyperparameter(
                            primitives_set=pipeline_pb2.PrimitiveArguments(
                                data=hyperparameter['data'],
                            ),
                        )
                    else:
                        hyperparams[name] = pipeline_pb2.PrimitiveStepHyperparameter(
                            primitive=pipeline_pb2.PrimitiveArgument(
                                data=hyperparameter['data'],
                            ),
                        )
                elif hyperparameter['type'] == metadata_base.ArgumentType.VALUE:
                    hyperparams[name] = pipeline_pb2.PrimitiveStepHyperparameter(
                        value=pipeline_pb2.ValueArgument(
                            data=encode_value({'type': 'object', 'value': hyperparameter['data']}, allowed_value_types, scratch_dir, plasma_put=plasma_put, validate_uri=validate_uri),
                        ),
                    )
                else:
                    raise exceptions.UnexpectedValueError("Unknown hyperparameter type: {hyperparameter_type}".format(hyperparameter_type=hyperparameter['type']))

            # If the primitive is not resolved.
            if step.primitive is None:
                primitive = encode_primitive_description(step.primitive_description)
            else:
                primitive = encode_primitive(step.primitive)

            steps.append(
                pipeline_pb2.PipelineDescriptionStep(
                    primitive=pipeline_pb2.PrimitivePipelineDescriptionStep(
                        primitive=primitive,
                        arguments=arguments,
                        outputs=[pipeline_pb2.StepOutput(id=output_id) for output_id in step.outputs],
                        hyperparams=hyperparams,
                        users=[pipeline_pb2.PipelineDescriptionUser(id=user['id'], reason=user.get('reason', None), rationale=user.get('rationale', None)) for user in step.users],
                    ),
                ),
            )
        elif isinstance(step, pipeline_module.SubpipelineStep):
            steps.append(
                pipeline_pb2.PipelineDescriptionStep(
                    pipeline=pipeline_pb2.SubpipelinePipelineDescriptionStep(
                        pipeline=encode_pipeline_description(step.pipeline, allowed_value_types, scratch_dir, plasma_put=plasma_put, validate_uri=validate_uri),
                        inputs=[pipeline_pb2.StepInput(data=input_data) for input_data in step.inputs],
                        outputs=[pipeline_pb2.StepOutput(id=output_id) for output_id in step.outputs],
                    ),
                ),
            )
        elif isinstance(step, pipeline_module.PlaceholderStep):
            steps.append(
                pipeline_pb2.PipelineDescriptionStep(
                    placeholder=pipeline_pb2.PlaceholderPipelineDescriptionStep(
                        inputs=[pipeline_pb2.StepInput(data=input_data) for input_data in step.inputs],
                        outputs=[pipeline_pb2.StepOutput(id=output_id) for output_id in step.outputs],
                    ),
                ),
            )
        else:
            raise exceptions.UnexpectedValueError("Unknown step type: {step_type}".format(step_type=type(step)))

    return pipeline_pb2.PipelineDescription(
        id=pipeline.id,
        source=source,
        created=encode_timestamp(pipeline.created),
        context=pipeline.context.value,
        name=pipeline.name,
        description=pipeline.description,
        users=[pipeline_pb2.PipelineDescriptionUser(id=user['id'], reason=user.get('reason', None), rationale=user.get('rationale', None)) for user in pipeline.users],
        inputs=[pipeline_pb2.PipelineDescriptionInput(name=input.get('name', None)) for input in pipeline.inputs],
        outputs=[pipeline_pb2.PipelineDescriptionOutput(name=output.get('name', None), data=output['data']) for output in pipeline.outputs],
        steps=steps,
        digest=pipeline.get_digest(),
    )


def _decode_user_description(user_description):
    user_description_dict = {'id': user_description.id}
    if user_description.reason:
        user_description_dict['reason'] = user_description.reason
    if user_description.rationale:
        user_description_dict['rationale'] = user_description.rationale

    return user_description_dict


def decode_pipeline_description(pipeline_description, resolver, *, pipeline_class=None, plasma_get=None,
                                validate_uri=None, compute_digest=dataset_module.ComputeDigest.ONLY_IF_MISSING, strict_digest=False):
    """
    Decodes a GRPC message into a pipeline.

    Parameters
    ----------
    pipeline_description : PipelineDescription
        A GRPC message.
    resolver : Resolver
        An instance of primitive and pipeline resolver to use.
    pipeline_class : Type[Pipeline]
        A pipeline class to use for instances of a pipeline.
        By default `d3m.metadata.pipeline.Pipeline`.
    plasma_get : Callable
        A function to load a value from a Plasma store.
        The function takes object's ID as bytes and should return
        stored the value from a Plasma store.
    validate_uri : Callable
        A function which can validate that URI is a valid and supported file URI.
        The function takes an URI as a string and should throw an exception if URI is invalid.
    compute_digest : ComputeDigest
        When loading datasets, compute a digest over the data?
    strict_digest : bool
        If computed digest does not match the one provided in metadata or message, raise an exception?

    Returns
    -------
    Union[Pipeline, None]
        A pipeline instance, or ``None`` if pipeline is not defined.
    """

    if pipeline_description.context == pipeline_pb2.PipelineContext.Value('PIPELINE_CONTEXT_UNKNOWN') and not pipeline_description.steps:
        return None

    if pipeline_class is None:
        pipeline_class = pipeline_module.Pipeline

    source = {}
    if pipeline_description.source.name:
        source['name'] = pipeline_description.source.name
    if pipeline_description.source.contact:
        source['contact'] = pipeline_description.source.contact
    if pipeline_description.source.pipelines:
        source['from'] = {
            'type': 'PIPELINE',
            'pipelines': [{'id': pipeline_id} for pipeline_id in pipeline_description.source.pipelines],
        }

    if not source:
        source = None

    pipeline = pipeline_class(
        pipeline_id=pipeline_description.id, context=metadata_base.Context(pipeline_description.context),
        created=decode_timestamp(pipeline_description.created), source=source,
        name=(pipeline_description.name or None), description=(pipeline_description.description or None),
    )

    for input_description in pipeline_description.inputs:
        pipeline.add_input(input_description.name or None)

    for step_description in pipeline_description.steps:
        step_type = step_description.WhichOneof('step')

        if step_type == 'primitive':
            step = pipeline._get_step_class(metadata_base.PipelineStepType.PRIMITIVE)(primitive_description=decode_primitive(step_description.primitive.primitive), resolver=resolver)

            for argument_name, argument_description in step_description.primitive.arguments.items():
                argument_type = argument_description.WhichOneof('argument')

                if argument_type == 'container':
                    step.add_argument(argument_name, metadata_base.ArgumentType.CONTAINER, argument_description.container.data)
                elif argument_type == 'data':
                    step.add_argument(argument_name, metadata_base.ArgumentType.DATA, argument_description.data.data)
                elif argument_type == 'container_list':
                    step.add_argument(argument_name, metadata_base.ArgumentType.CONTAINER, argument_description.container_list.data)
                else:
                    raise exceptions.UnexpectedValueError("Unknown argument type: {argument_type}".format(argument_type=argument_type))

            for output_description in step_description.primitive.outputs:
                step.add_output(output_description.id)

            for hyperparameter_name, hyperparameter_description in step_description.primitive.hyperparams.items():
                argument_type = hyperparameter_description.WhichOneof('argument')

                if argument_type == 'container':
                    step.add_hyperparameter(hyperparameter_name, metadata_base.ArgumentType.CONTAINER, hyperparameter_description.container.data)
                elif argument_type == 'data':
                    step.add_hyperparameter(hyperparameter_name, metadata_base.ArgumentType.DATA, hyperparameter_description.data.data)
                elif argument_type == 'primitive':
                    step.add_hyperparameter(hyperparameter_name, metadata_base.ArgumentType.PRIMITIVE, hyperparameter_description.primitive.data)
                elif argument_type == 'value':
                    value = decode_value(hyperparameter_description.value.data, validate_uri=validate_uri)
                    step.add_hyperparameter(hyperparameter_name, metadata_base.ArgumentType.VALUE, load_value(value, plasma_get=plasma_get, validate_uri=validate_uri, compute_digest=compute_digest, strict_digest=strict_digest))
                elif argument_type == 'data_set':
                    step.add_hyperparameter(hyperparameter_name, metadata_base.ArgumentType.DATA, hyperparameter_description.data_set.data)
                elif argument_type == 'primitives_set':
                    step.add_hyperparameter(hyperparameter_name, metadata_base.ArgumentType.PRIMITIVE, hyperparameter_description.primitives_set.data)
                else:
                    raise exceptions.UnexpectedValueError("Unknown argument type: {argument_type}".format(argument_type=argument_type))

            for user_description in step_description.primitive.users:
                step.add_user(_decode_user_description(user_description))

        elif step_type == 'pipeline':
            subpipeline = decode_pipeline_description(step_description.pipeline.pipeline, resolver, pipeline_class=pipeline_class, plasma_get=plasma_get, validate_uri=validate_uri, compute_digest=compute_digest, strict_digest=strict_digest)

            step = pipeline._get_step_class(metadata_base.PipelineStepType.SUBPIPELINE)(pipeline=subpipeline, resolver=resolver)

            for pipeline_input in step_description.pipeline.inputs:
                step.add_input(pipeline_input.data)

            for pipeline_output in step_description.pipeline.outputs:
                step.add_output(pipeline_output.id or None)

        elif step_type == 'placeholder':
            step = pipeline._get_step_class(metadata_base.PipelineStepType.PLACEHOLDER)(resolver=resolver)

            for placeholder_input in step_description.placeholder.inputs:
                step.add_input(placeholder_input.data)

            for placeholder_output in step_description.placeholder.outputs:
                step.add_output(placeholder_output.id)

        else:
            raise exceptions.InvalidArgumentValueError("Invalid step type '{step_type}'.".format(step_type=step_type))

        pipeline.add_step(step)

    for output_description in pipeline_description.outputs:
        pipeline.add_output(output_description.data, output_description.name or None)

    for user_description in pipeline_description.users:
        pipeline.add_user(_decode_user_description(user_description))

    # Generating JSON also checks it against the pipeline schema.
    # This requires all sub-pipelines to be resolved, but this is true in our case.
    pipeline.to_json_structure(nest_subpipelines=True)

    if pipeline_description.digest:
        pipeline_digest = pipeline.get_digest()

        if pipeline_digest != pipeline_description.digest:
            if strict_digest:
                raise exceptions.DigestMismatchError(
                    "Digest for pipeline '{pipeline_id}' does not match a computed one. "
                    "Provided digest: {pipeline_digest}. Computed digest: {new_pipeline_digest}.".format(
                        pipeline_id=pipeline.id,
                        pipeline_digest=pipeline_description.digest,
                        new_pipeline_digest=pipeline_digest,
                    )
                )
            else:
                logger.warning(
                    "Digest for pipeline '%(pipeline_id)s' does not match a computed one. "
                    "Provided digest: %(pipeline_digest)s. Computed digest: %(new_pipeline_digest)s.",
                    {
                        'pipeline_id': pipeline.id,
                        'pipeline_digest': pipeline_description.digest,
                        'new_pipeline_digest': pipeline_digest,
                    },
                )

    return pipeline


def encode_performance_metric(metric):
    """
    Encodes a dict describing a performance metric into a GRPC message.

    Parameters
    ----------
    metric : Dict
        A dict with fields ``metric`` and ``params``, where ``metric``
        is a ``PerformanceMetric`` enumeration value.

    Returns
    -------
    ProblemPerformanceMetric
        A GRPC message.
    """

    return problem_pb2.ProblemPerformanceMetric(
        metric=metric['metric'].value,
        k=metric.get('params', {}).get('k', None),
        pos_label=metric.get('params', {}).get('pos_label', None),
    )


def decode_performance_metric(metric):
    """
    Decodes a GRPC message into a dict describing a performance metric.

    Parameters
    ----------
    metric : ProblemPerformanceMetric
        A GRPC message.

    Returns
    -------
    Dict
        A dict with fields ``metric`` and ``params``, where ``metric``
        is a ``PerformanceMetric`` enumeration value.
    """

    params = {}

    if metric.k:
        params['k'] = metric.k
    if metric.pos_label:
        params['pos_label'] = metric.pos_label

    return {
        # TODO: Support additional metrics like "LOSS".
        'metric': problem_module.PerformanceMetric(metric.metric),
        'params': params,
    }


def encode_score(score, allowed_value_types, scratch_dir, *, plasma_put=None, validate_uri=None):
    """
    Encodes a score description into a GRPC message.

    Parameters
    ----------
    score : Dict
        A score description is a dict with fields: ``metric``
        ``fold``, ``targets``, ``value``, ``dataset_id``.
    allowed_value_types : Sequence[ValueType]
        A list of allowed value types to encode this value as. This
        list is tried in order until encoding succeeds.
    scratch_dir : str
        Path to a directory to store any temporary files needed during execution.
    plasma_put : Callable
        A function to store a value into a Plasma store.
        The function takes a value to store into Plasma store and should return
        stored object's ID as bytes.
    validate_uri : Callable
        A function which can validate that URI is a valid and supported file URI.
        The function takes an URI as a string and should throw an exception if URI is invalid.

    Returns
    -------
    Score
        A GRPC message.
    """

    return core_pb2.Score(
        metric=encode_performance_metric(score['metric']),
        fold=score['fold'],
        targets=[problem_pb2.ProblemTarget(target_index=target['target_index'], resource_id=target['resource_id'], column_index=target['column_index'], column_name=target['column_name']) for target in score['targets']],
        value=encode_value({'type': 'object', 'value': score['value']}, allowed_value_types, scratch_dir, plasma_put=plasma_put, validate_uri=validate_uri),
        dataset_id=score['dataset_id'],
    )


def encode_raw_value(value):
    """
    Encodes a simple Python value into a GRPC message.

    Parameters
    ----------
    value : Any
        A simple Python value.

    Returns
    -------
    ValueRaw
        A GRPC message.
    """

    if value is None:
        return value_pb2.ValueRaw(null=value_pb2.NullValue.Value('NULL_VALUE'))
    elif isinstance(value, bool):
        return value_pb2.ValueRaw(bool=value)
    elif d3m_utils.is_float(type(value)):
        return value_pb2.ValueRaw(double=float(value))
    elif d3m_utils.is_int(type(value)):
        return value_pb2.ValueRaw(int64=int(value))
    elif isinstance(value, str):
        return value_pb2.ValueRaw(string=value)
    elif isinstance(value, bytes):
        return value_pb2.ValueRaw(bytes=value)
    elif isinstance(value, (dict, frozendict.frozendict)):
        return value_pb2.ValueRaw(dict=value_pb2.ValueDict(items={key: encode_raw_value(val) for key, val in value.items()}))
    # We do not want to encode container type "List" as raw value to not lose metadata.
    elif isinstance(value, (list, tuple)) and not isinstance(value, container.List):
        return value_pb2.ValueRaw(list=value_pb2.ValueList(items=[encode_raw_value(item) for item in value]))
    else:
        raise exceptions.InvalidArgumentTypeError("Unsupported type '{value_type}' for raw value.".format(value_type=type(value)))


def decode_raw_value(value):
    """
    Decodes a GRPC message into a simple Python value.

    Parameters
    ----------
    value : ValueRaw
        A GRPC message.

    Returns
    -------
    Any
        A simple Python value.
    """

    value_type = value.WhichOneof('raw')

    if value_type == 'null':
        return None
    elif value_type in ['double', 'int64', 'bool', 'string', 'bytes']:
        return getattr(value, value_type)
    elif value_type == 'list':
        return [decode_raw_value(item) for item in value.list.items]
    elif value_type == 'dict':
        return {key: decode_raw_value(val) for key, val in value.dict.items.items()}
    else:
        raise exceptions.InvalidArgumentTypeError("Unsupported raw value type '{value_type}'.".format(value_type=value_type))


def encode_timestamp(value):
    """
    Encodes a Python's ``datetime`` into a GRPC message.

    Parameters
    ----------
    value : datetime
        A ``datetime`` instance.

    Returns
    -------
    Timestamp
        A GRPC message.
    """

    if value is None:
        return None

    if value.tzinfo is None or value.tzinfo.utcoffset(value) is None:
        raise exceptions.InvalidArgumentValueError("Value is missing timezone information.")
    else:
        # Convert to UTC timezone and set "tzinfo" to "datetime.timezone.utc".
        # Then we remove timezone information before converting it to GRPC because
        # GRPC does not support conversion from timezone aware datetime objects.
        # See: https://github.com/google/protobuf/issues/5003
        value = value.astimezone(datetime.timezone.utc).replace(tzinfo=None)

    timestamp = timestamp_pb2.Timestamp()
    timestamp.FromDatetime(value)
    return timestamp


def decode_timestamp(value):
    """
    Decodes a GRPC message into a Python's ``datetime``.

    Parameters
    ----------
    value : Timestamp
        A GRPC message.

    Returns
    -------
    datetime
        A ``datetime`` instance.
    """

    if value is None:
        return None

    # Default value.
    if value == timestamp_pb2.Timestamp():
        return None

    # Timestamp is in UTC>
    value = value.ToDatetime().replace(tzinfo=datetime.timezone.utc)

    return value


def save_value(value, allowed_value_types, scratch_dir, *, plasma_put=None, raise_error=False):
    """
    Saves a raw Python value and returns a dict representing the
    value at the *intermediate level*.

    It tries to save it based on allowed value types, potentially saving it to a disk
    and providing an URI to the location. It uses Python `tempfile` module to generate
    the location.

    Parameters
    ----------
    value : Any
        A value to save.
    allowed_value_types : Sequence[ValueType]
        A list of allowed value types to save this value as. This
        list is tried in order until encoding succeeds.
    scratch_dir : str
         Path to a directory to store any temporary files needed during run.
    plasma_put : Callable
        A function to store a value into a Plasma store.
        The function takes a value to store into Plasma store and should return
        stored object's ID as bytes.
    raise_error : bool
        If value cannot be encoded, should an exception be raised or
        should it be returned as value type ``error``?

    Returns
    -------
    Dict
        A dict with ``type`` and ``value`` fields. ``type`` can be one of
        ``object``, ``dataset_uri``, ``csv_uri``, ``pickle_uri``, ``plasma_id``, ``error``.
        ``value`` is then a corresponding value for a given type.
        ``error`` value type is possible only if ``raise_error`` is ``False``.
    """

    last_error = None

    for allowed_value_type in allowed_value_types:
        try:
            if allowed_value_type == ValueType.RAW:
                if sys.getsizeof(value) <= MAX_WIRE_OBJECT_SIZE and _can_encode_raw(value):
                    return {
                        'type': 'object',
                        'value': value,
                    }
            elif allowed_value_type == ValueType.LARGE_RAW:
                if _can_encode_raw(value):
                    return {
                        'type': 'object',
                        'value': value,
                    }
            elif allowed_value_type == ValueType.DATASET_URI:
                if isinstance(value, container.Dataset):
                    dataset_id = str(uuid.uuid4())

                    # We change dataset ID to a new value to assure it is unique.
                    value = value.copy()
                    value.metadata = value.metadata.update((), {'id': dataset_id})

                    dataset_dir = tempfile.mkdtemp(prefix=dataset_id, dir=scratch_dir)
                    try:
                        os.chmod(dataset_dir, 0o755)
                        uri = _fix_uri(os.path.abspath(os.path.join(dataset_dir, 'datasetDoc.json')))
                        value.save(uri)
                        return {
                            'type': 'dataset_uri',
                            'value': uri,
                        }
                    except Exception as error:
                        # Clean-up the directory, it will not be used.
                        try:
                            shutil.rmtree(dataset_dir, ignore_errors=True)
                        except Exception:
                            pass
                        raise error
            elif allowed_value_type == ValueType.CSV_URI:
                dataframe_value = None

                if isinstance(value, container.List):
                    dataframe_value = container.DataFrame(value)
                elif isinstance(value, container.ndarray):
                    metadata = value.metadata.query((metadata_base.ALL_ELEMENTS,))

                    if 'dimension' in metadata:
                        # Extract the column names so we can add them to the created dataframe, or set it to index string.
                        num_cols = value.metadata.query((metadata_base.ALL_ELEMENTS,))['dimension']['length']
                        col_names = [value.metadata.query((metadata_base.ALL_ELEMENTS, i)).get('name', str(i)) for i in range(num_cols)]
                    else:
                        col_names = None

                    dataframe_value = container.DataFrame(value, columns=col_names)
                elif isinstance(value, container.DataFrame):
                    dataframe_value = value

                if dataframe_value is not None:
                    csv_file_descriptor, csv_path = tempfile.mkstemp(suffix='.csv', dir=scratch_dir)
                    try:
                        os.chmod(csv_path, 0o644)
                        with open(csv_file_descriptor, 'w') as csv_file:
                            runtime_module.export_dataframe(dataframe_value, csv_file)
                        uri = _fix_uri(os.path.abspath(csv_path))
                        return {
                            'type': 'csv_uri',
                            'value': uri,
                        }
                    except Exception as error:
                        # Clean-up the file, it will not be used.
                        try:
                            os.close(csv_file_descriptor)
                        except Exception:
                            pass
                        try:
                            os.remove(csv_path)
                        except Exception:
                            pass
                        raise error
            elif allowed_value_type == ValueType.PICKLE_URI:
                value_file_descriptor, value_path = tempfile.mkstemp(suffix='.pickle', dir=scratch_dir)
                try:
                    os.chmod(value_path, 0o644)
                    with open(value_file_descriptor, 'wb') as value_file:
                        pickle.dump(value, value_file)
                    uri = _fix_uri(os.path.abspath(value_path))
                    return {
                        'type': 'pickle_uri',
                        'value': uri,
                    }
                except Exception as error:
                    # Clean-up the file, it will not be used.
                    try:
                        os.close(value_file_descriptor)
                    except Exception:
                        pass
                    try:
                        os.remove(value_path)
                    except Exception:
                        pass
                    raise error
            elif allowed_value_type == ValueType.PICKLE_BLOB:
                if sys.getsizeof(value) <= MAX_WIRE_OBJECT_SIZE and _can_pickle(value):
                    return {
                        'type': 'object',
                        'value': value,
                    }
            elif allowed_value_type == ValueType.LARGE_PICKLE_BLOB:
                if _can_pickle(value):
                    return {
                        'type': 'object',
                        'value': value,
                    }
            elif plasma_put is not None and allowed_value_type == ValueType.PLASMA_ID:
                object_id = plasma_put(value)
                return {
                    'type': 'plasma_id',
                    'value': _binary_to_hex(object_id),
                }
            else:
                raise exceptions.UnexpectedValueError("Unknown allowed value type: {allowed_value_type}".format(allowed_value_type=allowed_value_type))

        except Exception as error:
            last_error = error

    # TODO: Add a second pass to try the conversion between "DATASET_URI" and "CSV_URI".

    if last_error is not None:
        if raise_error:
            raise last_error
        else:
            return {
                'type': 'error',
                'value': str(last_error),
            }

    error_message = "None of the allowed value types could encode the value of type '{value_type}'.".format(value_type=type(value))

    if raise_error:
        raise ValueError(error_message)
    else:
        return {
            'type': 'error',
            'value': error_message,
        }


def load_value(value, *, plasma_get=None, validate_uri=None,
               compute_digest=dataset_module.ComputeDigest.ONLY_IF_MISSING, strict_digest=False):
    """
    Loads and returns a raw Python value from a dict representing a value
    at the *intermediate level*.

    Parameters
    ----------
    value : Dict
        A dict with ``type`` and ``value`` fields. ``type`` can be one of
        ``object``, ``dataset_uri``, ``csv_uri``, ``pickle_uri``, ``plasma_id``, ``error``.
        ``value`` is then a corresponding value for a given type.
    plasma_get : Callable
        A function to load a value from a Plasma store.
        The function takes object's ID as bytes and should return
        stored the value from a Plasma store.
    validate_uri : Callable
        A function which can validate that URI is a valid and supported file URI.
        The function takes an URI as a string and should throw an exception if URI is invalid.
    compute_digest : ComputeDigest
        When loading datasets, compute a digest over the data?
    strict_digest : bool
        If computed digest does not match the one provided in metadata, raise an exception?

    Returns
    -------
    Any
        Loaded raw Python value.
    """

    if value['type'] == 'object':
        return value['value']
    elif value['type'] == 'dataset_uri':
        uri = _fix_uri(value['value'])
        return container.Dataset.load(uri, compute_digest=compute_digest, strict_digest=strict_digest)
    elif value['type'] == 'csv_uri':
        uri = _fix_uri(value['value'])
        # Pandas requires a host for "file" URIs.
        uri = _fix_file_uri_host(uri)
        data = pandas.read_csv(
            uri,
            # We do not want to do any conversion of values at this point.
            # This should be done by primitives later on.
            dtype=str,
            # We always expect one row header.
            header=0,
            # We want empty strings and not NaNs.
            na_filter=False,
            encoding='utf8',
            low_memory=False,
        )
        return container.DataFrame(data)
    elif value['type'] == 'pickle_uri':
        uri = _fix_uri(value['value'])
        with _open_file_uri(uri, 'rb', validate_uri=validate_uri) as file:
            # TODO: Limit the types of values being able to load to prevent arbitrary code execution by a malicious pickle.
            return pickle.load(file)
    elif plasma_get is not None and value['type'] == 'plasma_id':
        return plasma_get(_hex_to_binary(value['value']))
    elif value['type'] == 'error':
        raise ValueError("Error in value: {message}".format(message=value['value']))
    else:
        raise exceptions.UnexpectedValueError("Unknown value type: {value_type}".format(value_type=value['type']))


def encode_value(value, allowed_value_types, scratch_dir, *, plasma_put=None, validate_uri=None):
    """
    Encodes a value into a GRPC message.

    The input is a dict representation of the value at the *intermediate level*.

    Parameters
    ----------
    value : Dict
        A dict with ``type`` and ``value`` fields. ``type`` can be one of
        ``object``, ``dataset_uri``, ``csv_uri``, ``pickle_uri``, ``plasma_id``, ``error``.
        ``value`` is then a corresponding value for a given type.
    allowed_value_types : Sequence[ValueType]
        A list of allowed value types to encode this value as. This
        list is tried in order until encoding succeeds.
    scratch_dir : str
        Path to a directory to store any temporary files needed during execution.
    plasma_put : Callable
        A function to store a value into a Plasma store.
        The function takes a value to store into Plasma store and should return
        stored object's ID as bytes.
    validate_uri : Callable
        A function which can validate that URI is a valid and supported file URI.
        The function takes an URI as a string and should throw an exception if URI is invalid.

    Returns
    -------
    Value
        A GRPC message.
    """

    if validate_uri is None:
        def validate_uri(uri):
            return uri

    assert value['type'] in ['object', 'dataset_uri', 'csv_uri', 'pickle_uri', 'plasma_id', 'error']

    if value['type'] == 'error':
        return value_pb2.Value(
            error=value_pb2.ValueError(message=value['value']),
        )

    last_error = None

    # The first pass is without any conversion and tries to match existing value type to allowed value type.
    for allowed_value_type in allowed_value_types:
        try:
            if allowed_value_type == ValueType.RAW:
                if value['type'] == 'object' and sys.getsizeof(value['value']) <= MAX_WIRE_OBJECT_SIZE:
                    return value_pb2.Value(
                        raw=encode_raw_value(value['value']),
                    )
            if allowed_value_type == ValueType.LARGE_RAW:
                if value['type'] == 'object':
                    return value_pb2.Value(
                        raw=encode_raw_value(value['value']),
                    )
            elif allowed_value_type == ValueType.DATASET_URI:
                if value['type'] == 'dataset_uri':
                    uri = _fix_uri(value['value'])
                    validate_uri(uri)
                    return value_pb2.Value(
                        dataset_uri=uri,
                    )
            elif allowed_value_type == ValueType.CSV_URI:
                if value['type'] == 'csv_uri':
                    uri = _fix_uri(value['value'])
                    validate_uri(uri)
                    return value_pb2.Value(
                        csv_uri=uri,
                    )
            elif allowed_value_type == ValueType.PICKLE_URI:
                if value['type'] == 'pickle_uri':
                    uri = _fix_uri(value['value'])
                    validate_uri(uri)
                    return value_pb2.Value(
                        pickle_uri=uri,
                    )
            elif allowed_value_type == ValueType.PICKLE_BLOB:
                if value['type'] == 'object' and sys.getsizeof(value['value']) <= MAX_WIRE_OBJECT_SIZE:
                    return value_pb2.Value(
                        pickle_blob=pickle.dumps(value['value']),
                    )
            elif allowed_value_type == ValueType.LARGE_PICKLE_BLOB:
                if value['type'] == 'object':
                    return value_pb2.Value(
                        pickle_blob=pickle.dumps(value['value']),
                    )
            elif allowed_value_type == ValueType.PLASMA_ID:
                if value['type'] == 'plasma_id':
                    return value_pb2.Value(
                        plasma_id=_hex_to_binary(value['value']),
                    )
            else:
                raise exceptions.UnexpectedValueError("Unknown allowed value type: {allowed_value_type}".format(allowed_value_type=allowed_value_type))

        except Exception as error:
            last_error = error

    # The second pass tries to convert between value types to match an allowed value type.
    # TODO: Support also conversion between "DATASET_URI" and "CSV_URI".
    for allowed_value_type in allowed_value_types:
        try:
            if allowed_value_type == ValueType.PICKLE_URI and value['type'] == 'object':
                value_file_descriptor, value_path = tempfile.mkstemp(suffix='.pickle', dir=scratch_dir)
                try:
                    os.chmod(value_path, 0o644)
                    with open(value_file_descriptor, 'wb') as value_file:
                        pickle.dump(value['value'], value_file)
                    uri = _fix_uri(os.path.abspath(value_path))
                    validate_uri(uri)
                    return value_pb2.Value(
                        pickle_uri=uri,
                    )
                except Exception as error:
                    # Clean-up the file, it will not be used.
                    try:
                        os.close(value_file_descriptor)
                    except Exception:
                        pass
                    try:
                        os.remove(value_path)
                    except Exception:
                        pass
                    raise error
            elif plasma_put is not None and allowed_value_type == ValueType.PLASMA_ID and value['type'] == 'object':
                object_id = plasma_put(value['value'])
                return value_pb2.Value(
                    plasma_id=object_id,
                )

        except Exception as error:
            last_error = error

    if last_error is not None:
        return value_pb2.Value(
            error=value_pb2.ValueError(message=str(last_error)),
        )

    return value_pb2.Value(
        error=value_pb2.ValueError(message="None of the allowed value types could encode the value of type '{value_type}'.".format(value_type=type(value))),
    )


def decode_value(value, *, validate_uri=None, raise_error=True):
    """
    Decodes a GRPC message.

    The output is a dict representation of the value at the *intermediate level*.

    Parameters
    ----------
    value : Value
        A GRPC message to decode.
    validate_uri : Callable
        A function which can validate that URI is a valid and supported file URI.
        The function takes an URI as a string and should throw an exception if URI is invalid.
    raise_error : bool
        If value is representing an error, should an exception be raised or
        should it be returned as value type ``error``?

    Returns
    -------
    Dict
        A dict with ``type`` and ``value`` fields. ``type`` can be one of
        ``object``, ``dataset_uri``, ``csv_uri``, ``pickle_uri``, ``plasma_id``, ``error``.
        ``value`` is then a corresponding value for a given type.
        ``error`` value type is possible only if ``raise_error`` is ``False``.
    """

    if validate_uri is None:
        def validate_uri(uri):
            return uri

    value_type = value.WhichOneof('value')

    if value_type == 'error':
        if raise_error:
            raise ValueError("Error in value: {message}".format(message=value.error.message))
        else:
            return {
                'type': 'error',
                'value': value.error.message,
            }
    elif value_type == 'raw':
        value = decode_raw_value(value.raw)
        return {
            'type': 'object',
            'value': value,
        }
    elif value_type in ['dataset_uri', 'csv_uri', 'pickle_uri']:
        uri = getattr(value, value_type)
        uri = _fix_uri(uri)
        validate_uri(uri)
        return {
            'type': value_type,
            'value': uri,
        }
    elif value_type == 'pickle_blob':
        # TODO: Limit the types of values being able to load to prevent arbitrary code execution by a malicious pickle.
        value = pickle.loads(value.pickle_blob)
        return {
            'type': 'object',
            'value': value,
        }
    elif value_type == 'plasma_id':
        return {
            'type': 'plasma_id',
            'value': _binary_to_hex(value.plasma_id),
        }
    else:
        raise exceptions.InvalidArgumentValueError("Unsupported value type '{value_type}'.".format(value_type=value_type))
