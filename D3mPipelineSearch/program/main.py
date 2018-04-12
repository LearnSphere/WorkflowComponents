from __future__ import absolute_import, division, print_function

import grpc
import logging
import sys
from os import path
import argparse
import json

import core_pb2 as core_pb2
import core_pb2_grpc as core_pb2_grpc

import data_ext_pb2 as data_ext_pb2
import data_ext_pb2_grpc as data_ext_pb2_grpc

import dataflow_ext_pb2 as dataflow_ext_pb2
import dataflow_ext_pb2_grpc as dataflow_ext_pb2_grpc

logging.basicConfig()
logger = logging.getLogger('d3m_pipeline_search')
logger.setLevel(logging.DEBUG)

__version__ = '0.1'
config = {
# 'dataset_dir': "file:///rdata/dataStore/d3m/datasets/seed_datasets_current",
# 'dataset_json': 'datasetDoc.json',
# 'dataset_uri': "file:///rdata/dataStore/d3m/test_datasets/185_baseball/185_baseball_dataset",
# 'dataset_json': 'datasetDoc.json',
'ta2_address': 'sophia.stevencdang.com:45042'
}
# config['ta2_address'] = 'lyra.auton.cs.cmu.edu:45042'

def print_msg(msg):
    msg = str(msg)
    for line in msg.splitlines():
        print("    | %s" % line)
    print("    \\____________________")


_map_progress = dict((k, v) for v, k in core_pb2.Progress.items())
map_progress = lambda p: _map_progress.get(p, p)
_map_status = dict((k, v)
                   for v, k in dataflow_ext_pb2.ModuleResult.Status.items())
map_status = lambda p: _map_status.get(p, p)

# def get_dataset_uri():
    # return path.join(config['dataset_uri'], config['dataset_json'])

def get_dataset_path(ds):
    """
    Generate path to dataset json based on name of dataset

    """
    return path.join(config['dataset_dir'], 
                     ds, 
                     ds + '_dataset', 
                     config['dataset_json'])

def get_default_arg_parser(desc):
    """
    Define an argument parser for use with Tigris Components and
    mandatory arguments

    """
    parser = argparse.ArgumentParser(description=desc)

    parser.add_argument('-programDir', type=str,
                       help='the component program directory')

    parser.add_argument('-workingDir', type=str,
                       help='the component instance working directory')

    return parser



def main():
    logger.info("Running Pipeline Search on TA2")

    # Parse argumennts
    parser = get_default_arg_parser("D3M Pipeline Search")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for the search')
    args = parser.parse_args()
    logger.debug("Running D3M Pipeline Search with arguments: %s" % str(args))

    # Open and pring lines of dataset json
    # ds_file = open(args.file0, 'r')
    # for line in ds_file.readlines():
        # logger.debug(line)
    # ds_file.close()
    ds_file = args.file0
    ds_info = json.load(ds_file)
    logger.debug("Dataset json parse: %s" % str(ds_info))


    address = config['ta2_address']
    
    print("using server at addres %s" % address)
    channel = grpc.insecure_channel(address)
    stub = core_pb2_grpc.CoreStub(channel)
    stub_dataflow_ext = dataflow_ext_pb2_grpc.DataflowExtStub(channel)
    stub_data_ext = data_ext_pb2_grpc.DataExtStub(channel)

    version = core_pb2.DESCRIPTOR.GetOptions().Extensions[
        core_pb2.protocol_version]


    print("\n> Calling StartSession...")
    reply = stub.StartSession(core_pb2.SessionRequest(
        user_agent='text_client %s' % __version__,
        version=version,
    ))
    context = reply.context
    print("  Started session %r, status %s" % (
          context.session_id, reply.response_info.status.code))
    print_msg(reply)


    print("\n> Calling CreatePipelines...")
    create_stream = stub.CreatePipelines(core_pb2.PipelineCreateRequest(
        context=context,
        dataset_uri=ds_info['dataset_json'],
        task=core_pb2.CLASSIFICATION,
        task_subtype=core_pb2.NONE,
        task_description="Debugging task",
        metrics=[
            core_pb2.ACCURACY,
            core_pb2.ROC_AUC,
            core_pb2.F1
        ],
        target_features=[
            core_pb2.Feature(resource_id='0',
                             feature_name='Hall_of_Fame'),
        ],
        predict_features=[],
        max_pipelines=10,
    ))


    print("  Stream open")
    pipelines = set()
    for created in create_stream:
        if created.response_info.status.code == core_pb2.CANCELLED:
            print("! Pipelines creation cancelled")
            break
        elif (created.response_info.status.code != core_pb2.OK and
                not created.pipeline_id):
            print("! Error during pipelines creation")
            if created.response_info.status.details:
                print("! details: %r" %
                      created.response_info.status.details)
            break
        print_msg(created)
        progress = created.progress_info
        pipeline_id = created.pipeline_id
        if progress == core_pb2.COMPLETED:
            pipelines.add(pipeline_id)
        print("  Got pipeline event, pipeline_id=%r, progress=%s" % (
              pipeline_id, map_progress(progress)))
        if created.HasField('pipeline_info'):
            print_msg(created.pipeline_info)

    # # TODO: ListPipelines seems broken.
    # # print("\n> Calling ListPipelines...")
    # # pipelines = stub.ListPipelines(
    # #     core_pb2.PipelineListRequest(context = context)
    # # )
    # # print(pipelines)
    # # print("  Requested pipelines")
    
    
    # print("\n> Calling DataflowExt.DescribeDataflow...")
    # try:
        # for pipeline_id in pipelines:
            # print("  pipeline %r" % pipeline_id)
            # dataflow_description = stub_dataflow_ext.DescribeDataflow(
                # dataflow_ext_pb2.PipelineReference(
                    # context=context,
                    # pipeline_id=pipeline_id))
            # print_msg(dataflow_description)
    # except grpc.RpcError as e:
        # if e.code() == grpc.StatusCode.UNIMPLEMENTED:
            # print(". Call is not implemented")


    for pipeline_id in pipelines:
        print("\n> Calling ExecutePipeline for pipeline %r..." % pipeline_id)
        execute_stream = stub.ExecutePipeline(core_pb2.PipelineExecuteRequest(
            context=context,
            pipeline_id=pipeline_id,
            dataset_uri=ds_info['dataset_json'],
        ))
        print("  Stream open")
        for execd in execute_stream:
            if execd.response_info.status.code == core_pb2.CANCELLED:
                print("! Pipeline execution cancelled")
                break
            elif execd.response_info.status.code != core_pb2.OK:
                print("! Error during pipeline execution")
                if execd.response_info.status.details:
                    print("! details: %r" %
                          execd.response_info.status.details)
                break
            progress = execd.progress_info
            assert execd.pipeline_id == pipeline_id
            print("  Got execution event, pipeline_id=%r, progress=%s" % (
                pipeline_id, map_progress(progress)))
            if progress == core_pb2.COMPLETED:
                print("  Pipeline execution completed")
            print("    result_uri: %r" % execd.result_uri)
        print("  End of execution of %r" % pipeline_id)


    # print("\n> Calling DataflowExt.GetDataflowResults...")
    # try:
        # modules_stream = stub_dataflow_ext.GetDataflowResults(
            # dataflow_ext_pb2.PipelineReference(context=context,
                                               # pipeline_id=pipeline_id))
    # except grpc.RpcError as e:
        # if e.code() == grpc.StatusCode.UNIMPLEMENTED:
            # print(". Call is not implemented")
    # for module_result in modules_stream:
        # if module_result.response_info.status.code == core_pb2.CANCELLED:
            # print("! Pipeline execution cancelled")
            # break
        # elif module_result.response_info.status.code != core_pb2.OK:
            # print("! Error during execution")
            # if module_result.response_info.status.details:
                # print("! details: %r" %
                      # module_result.response_info.status.details)
        # status = module_result.status
        # print("  Got module event, module_id=%r, status=%s, "
              # "progress=%s, execution_time=%s" % (
                  # module_result.module_id, map_status(status),
                  # module_result.progress, module_result.execution_time))
        # for output in module_result.outputs:
            # print("  Output:")
            # print_msg(output)


    # print("\n> Calling DataExt.AddFeatures...")
    # print_msg(stub_data_ext.AddFeatures(
              # data_ext_pb2.AddFeaturesRequest(features=[])))


    # print("\n> Calling DataExt.RemoveFeatures...")
    # print_msg(stub_data_ext.RemoveFeatures(
              # data_ext_pb2.RemoveFeaturesRequest(features=[])))

    print("\n> Calling EndSession...")
    reply = stub.EndSession(context)
    print("  Ended session %r, status %s" % (
          context.session_id, reply.status.code))


if __name__ == '__main__':
    main()
