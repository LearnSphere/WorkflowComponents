import os
import re
import time
import shutil

import proto.core_pb2 as core_pb2
import proto.core_pb2_grpc as core_pb2_grpc

from common import logger, __version__

def copy_results(results_root):
    """
    Copy the dummy result files to results_root.
    """
    try:
        shutil.copytree("./out", results_root)
    except OSError as err:
        overwrite = False
        if "results_root_overwrite" in os.environ:
            overwrite = os.environ["results_root_overwrite"]
        if not overwrite and os.path.exists(results_root):
            logger.info("results_root %s is not empty, result uris will not point to valid files" % results_root)
            logger.info("overwrite it by setting env results_root_overwrite=1")
        else:
            shutil.rmtree(results_root)
            shutil.copytree("./out", results_root)

results_root = "/out"
if "results_root" in os.environ:
    results_root = os.environ['results_root']
    logger.info("using results_root %s" % results_root)
    copy_results(results_root)
else:
    logger.info("results_root env not set, using dummy root /out")

def result_file_uri(filename):
    path = os.path.join('file:///', results_root, filename)
    if path[0] == '/':
        path = 'file://' + path
    return path

class Core(core_pb2_grpc.CoreServicer):
    def __init__(self):
        self.sessions = set()

    def StartSession(self, request, context):
        version = core_pb2.DESCRIPTOR.GetOptions().Extensions[
            core_pb2.protocol_version]
        session = "session_%d" % len(self.sessions)
        self.sessions.add(session)
        logger.info("Session started: 1 (protocol version %s)", version)
        return core_pb2.SessionResponse(
            response_info=core_pb2.Response(
                status=core_pb2.Status(code=core_pb2.OK)
            ),
            user_agent="ta2_stub %s" % __version__,
            version=version,
            context=core_pb2.SessionContext(session_id=session),
        )

    def EndSession(self, request, context):
        assert request.session_id in self.sessions
        logger.info("Session terminated: %s", request.session_id)
        return core_pb2.Response(
            status=core_pb2.Status(code=core_pb2.OK),
        )

    def CreatePipelines(self, request, context):
        sessioncontext = request.context
        assert sessioncontext.session_id in self.sessions
        task = request.task
        assert task == core_pb2.CLASSIFICATION
        task_subtype = request.task_subtype
        task_description = request.task_description
        output = request.output
        metrics = request.metrics
        target_features = request.target_features
        predict_features = request.predict_features
        max_pipelines = request.max_pipelines

        logger.info("Got CreatePipelines request, session=%s",
                    sessioncontext.session_id)

        results = [
            (core_pb2.SUBMITTED, "pipeline_1", False),
            (core_pb2.SUBMITTED, "pipeline_2", False),
            (core_pb2.SUBMITTED, "pipeline_3", False),
            (core_pb2.RUNNING, "pipeline_2", False),
            (core_pb2.RUNNING, "pipeline_3", False),
            (core_pb2.RUNNING, "pipeline_1", False),
            (core_pb2.COMPLETED, "pipeline_1", True),
            (core_pb2.COMPLETED, "pipeline_2", True),
            (core_pb2.COMPLETED, "pipeline_3", True),
        ]

        pipeline_infos = [
            core_pb2.Pipeline(
                predict_result_uri=result_file_uri("predict1.csv"),
                output=output,
                scores=[
                    core_pb2.Score(
                        metric=core_pb2.ACCURACY,
                        value=0.8243,
                    ),
                    core_pb2.Score(
                        metric=core_pb2.ROC_AUC,
                        value=0.5213,
                    ),
                    core_pb2.Score(
                        metric=core_pb2.F1,
                        value=0.3549,
                    ),
                ],
            ),
            core_pb2.Pipeline(
                predict_result_uri=result_file_uri("predict2.csv"),
                output=output,
                scores=[
                    core_pb2.Score(
                        metric=core_pb2.ACCURACY,
                        value=0.5531,
                    ),
                    core_pb2.Score(
                        metric=core_pb2.ROC_AUC,
                        value=0.7624,
                    ),
                    core_pb2.Score(
                        metric=core_pb2.F1,
                        value=0.5778,
                    ),
                ],
            ),
            core_pb2.Pipeline(
                predict_result_uri=result_file_uri("predict3.csv"),
                output=output,
                scores=[
                    core_pb2.Score(
                        metric=core_pb2.ACCURACY,
                        value=0.2354,
                    ),
                    core_pb2.Score(
                        metric=core_pb2.ROC_AUC,
                        value=0.2543,
                    ),
                    core_pb2.Score(
                        metric=core_pb2.F1,
                        value=0.3024,
                    ),
                ],
            ),
        ]

        for progress, pipeline_id, send_pipeline in results:
            time.sleep(1)

            if not context.is_active():
                logger.info("Client closed CreatePipelines stream")

            msg = core_pb2.PipelineCreateResult(
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                progress_info=progress,
                pipeline_id=pipeline_id,
            )

            if send_pipeline:
                info_index = int(re.match("pipeline_(\d+)", pipeline_id).group(1)) - 1
                msg.pipeline_info.CopyFrom(pipeline_infos[info_index])

            yield msg

    # TODO: Check ListPipelines Implementation
    # def ListPipelines(self, request, context):
    #     sessioncontext = request.context
    #     assert sessioncontext.session_id in self.sessions
    #     return core_pb2.PipelineListResult(
    #         response_info = core_pb2.Response(status=core_pb2.Status(code=core_pb2.OK)),
    #         pipeline_ids = [
    #             "pipeline_1",
    #             "pipeline_2",
    #             "pipeline_3",
    #         ]
    #     )

    def ExecutePipeline(self, request, context):
        sessioncontext = request.context
        assert sessioncontext.session_id in self.sessions
        pipeline_id = request.pipeline_id
        pipeline_id_number = re.match("pipeline_(\d+)", pipeline_id).group(1)

        logger.info("Got ExecutePipeline request, session=%s",
                    sessioncontext.session_id)

        time.sleep(1)
        yield core_pb2.PipelineExecuteResult(
            response_info=core_pb2.Response(
                status=core_pb2.Status(code=core_pb2.OK),
            ),
            progress_info=core_pb2.RUNNING,
            pipeline_id=pipeline_id,
            result_uri="",
        )
        time.sleep(1)
        yield core_pb2.PipelineExecuteResult(
            response_info=core_pb2.Response(
                status=core_pb2.Status(code=core_pb2.OK),
            ),
            progress_info=core_pb2.COMPLETED,
            pipeline_id=pipeline_id,
            result_uri=result_file_uri("predict%s.csv" % pipeline_id_number),
        )


def core(server):
    core_pb2_grpc.add_CoreServicer_to_server(
        Core(), server)
