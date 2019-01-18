
# Author: Steven C. Dang

# Class for most common operations with TA2

import logging

from .api_v2 import core_pb2 as core_pb2
from .api_v2 import core_pb2_grpc as core_pb2_grpc

from .api_v2 import data_ext_pb2 as data_ext_pb2
from .api_v2 import data_ext_pb2_grpc as data_ext_pb2_grpc

from .api_v2 import dataflow_ext_pb2 as dataflow_ext_pb2
from .api_v2 import dataflow_ext_pb2_grpc as dataflow_ext_pb2_grpc

logger = logging.getLogger(__name__)

class TA2Client(object):
    """
    A client for common interactions with a TA2 system

    """
    __name__ = "CMU Tigris TA3"
    __version__ = 'v2017.12.20'

    @staticmethod
    def _print_msg(msg):
        msg = str(msg)
        for line in msg.splitlines():
            print("    | %s" % line)
        print("    \\____________________")

    def __init__(self, serv):
        self.serv = serv
        self.cur_context = None

    @staticmethod
    def map_progress(p):
        prog_items =  dict((k, v) for v, k in core_pb2.Progress.items())
        return prog_items.get(p, p)

    @staticmethod
    def map_status(p):
        status_items = dict((k, v)
                           for v, k in dataflow_ext_pb2.ModuleResult.Status.items())
        return status_items.get(p, p)

        
    # _map_progress = dict((k, v) for v, k in core_pb2.Progress.items())
    # map_progress = lambda p: _map_progress.get(p, p)
    # _map_status = dict((k, v)
                       # for v, k in dataflow_ext_pb2.ModuleResult.Status.items())
    # map_status = lambda p: _map_status.get(p, p)

    def start_session(self):
        logger.info("\n> Calling StartSession...")
        reply = self.serv['stub'].StartSession(core_pb2.SessionRequest(
            user_agent='%s %s' % (self.__name__, self.__version__),
            version=self.serv['version'],
        ))
        self.cur_context = reply.context
        logger.info("  Started session %r, status %s" % (
              self.cur_context.session_id, reply.response_info.status.code))
        TA2Client._print_msg(reply)

        return self.cur_context

    def create_pipelines(self, ds, prob):
        logger.info("\n> Calling CreatePipelines...")
        if self.cur_context is None:
            raise Exception("Need to start a new session before calling create pipelines")
        else:
            # Get target features from problem
            info = None
            logger.debug("Name of the dataset %s" % ds.name)
            ds_name = ds.name + "_dataset"
            for dataset in prob.inputs['data']:
                if dataset['datasetID'] == ds_name:
                    info = dataset['targets'][0]
                    break;
            if info is None:
                raise Exception("Could not identify pipeline target info with problem: %s" % str(prob))

            if prob.about['taskType'] == 'classification':
                task_type = core_pb2.CLASSIFICATION
            elif prob.about['taskType'] == 'regression':
                task_type = core_pb2.REGRESSION
            elif prob.about['taskType'] == 'clustering':
                task_type = core_pb2.CLUSTERING
            else:
                task_type = core_pb2.TASK_TYPE_UNDEFINED

            if prob.about['taskSubType'] == 'univariate':
                task_subtype = core_pb2.UNIVARIATE
            elif prob.about['taskSubType'] == 'multiClass':
                task_subtype = core_pb2.MULTICLASS
            else:
                task_subtype = core_pb2.TASK_SUBTYPE_UNDEFINED

            # Get performance metrics from problem doc
            metrics = []
            for metric in prob.inputs['performanceMetrics']:
                if metric == 'meanSquaredError':
                    metrics.append(core_pb2.MEAN_SQUARED_ERROR)
                elif metric == 'f1Macro':
                    metrics.append(core_pb2.F1_MACRO)
                else:
                    metrics.append(core_pb2.METRIC_UNDEFINED)
                
            create_stream = self.serv['stub'].CreatePipelines(core_pb2.PipelineCreateRequest(
                context=self.cur_context,
                dataset_uri=ds.get_schema_uri(),
                task=task_type,
                task_subtype=task_subtype,
                task_description=prob.about['problemDescription'],
                metrics=metrics,
                target_features=[
                    core_pb2.Feature(resource_id=info['resID'],
                                     feature_name=info['colName']),
                ],
                predict_features=[],
                max_pipelines=10,
            ))


            logger.info("Stream open")
            pipelines = set()
            predictions = []
            for created in create_stream:
                if created.response_info.status.code == core_pb2.CANCELLED:
                    logger.info("! Pipelines creation cancelled")
                    break
                elif (created.response_info.status.code != core_pb2.OK and
                        not created.pipeline_id):
                    logger.warning("! Error during pipelines creation")
                    if created.response_info.status.details:
                        logger.info("! details: %r" %
                              created.response_info.status.details)
                    break
                TA2Client._print_msg(created)
                progress = created.progress_info
                pipeline_id = created.pipeline_id
                if progress == core_pb2.COMPLETED:
                    pipelines.add(pipeline_id)
                logger.info("Got pipeline event, pipeline_id=%r, progress=%s" % (
                        pipeline_id, self.map_progress(progress)))
                if created.HasField('pipeline_info'):
                    TA2Client._print_msg(created.pipeline_info)
                    predictions.append(created.pipeline_info)

            return pipelines, predictions

    def execute_pipelines(self, ds, pipelines):
        for pipeline_id in pipelines:
            logger.info("\n> Calling ExecutePipeline for pipeline %r..." % pipeline_id)
            execute_stream = self.serv['stub'].ExecutePipeline(core_pb2.PipelineExecuteRequest(
                context=self.cur_context,
                pipeline_id=pipeline_id,
                dataset_uri=ds.get_schema_uri()
            ))
            logger.info("  Stream open")
            for execd in execute_stream:
                if execd.response_info.status.code == core_pb2.CANCELLED:
                    logger.info("! Pipeline execution cancelled")
                    break
                elif execd.response_info.status.code != core_pb2.OK:
                    logger.warning("! Error during pipeline execution")
                    if execd.response_info.status.details:
                        logger.info("! details: %r" %
                              execd.response_info.status.details)
                    break
                progress = execd.progress_info
                assert execd.pipeline_id == pipeline_id
                logger.info("  Got execution event, pipeline_id=%r, progress=%s" % (
                    pipeline_id, self.map_progress(progress)))
                if progress == core_pb2.COMPLETED:
                    logger.info("  Pipeline execution completed")
                logger.info("    result_uri: %r" % execd.result_uri)
            logger.info("  End of execution of %r" % pipeline_id)

    def end_session(self):
        logger.info("\n> Calling EndSession...")
        reply = self.serv['stub'].EndSession(self.cur_context)
        logger.info("  Ended session %r, status %s" % (
              self.cur_context.session_id, reply.status.code))
        self.cur_context = None

