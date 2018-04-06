import time

import proto.core_pb2 as core_pb2
import proto.dataflow_ext_pb2 as dataflow_ext_pb2
import proto.dataflow_ext_pb2_grpc as dataflow_ext_pb2_grpc

from common import logger


class DataflowExt(dataflow_ext_pb2_grpc.DataflowExtServicer):
    def __init__(self):
        self.sessions = set()

    def DescribeDataflow(self, request, context):
        sessioncontext = request.context
        logger.info("Got DescribeDataflow request, session=%s",
                    sessioncontext.session_id)
        if request.pipeline_id == "pipeline_1":
            return dataflow_ext_pb2.DataflowDescription(
                pipeline_id = "pipeline_1",
                modules=[
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_1",
                        type="reading_data",
                        label="Load Data",
                        inputs=[],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_2",
                        type="one_hot_encoder",
                        label="ISI: One Hot Encoder",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_3",
                        type="inputation",
                        label="ISI: Inputation",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_4",
                        type="classification",
                        label="sklearn: Linear SVM",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="labels", type="numpy_array")
                        ],
                    )
                ],
                connections=[
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_1",
                        to_module_id="module_2",
                        from_output_name="data_out",
                        to_input_name="data_in"
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_2",
                        to_module_id="module_3",
                        from_output_name="data_out",
                        to_input_name="data_in"
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_3",
                        to_module_id="module_4",
                        from_output_name="data_out",
                        to_input_name="data_in"
                    ),
                ],
            )
        elif request.pipeline_id == "pipeline_2":
            return dataflow_ext_pb2.DataflowDescription(
                pipeline_id="pipeline_2",
                modules=[
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_1",
                        type="reading_data",
                        label="Load Data",
                        inputs=[],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_2",
                        type="inputation",
                        label="ISI: Inputation",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_3",
                        type="classification",
                        label="Keras: CNN",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="labels", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_4",
                        type="metric",
                        label="Accuracy",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="labels", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="value", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_5",
                        type="metric",
                        label="ROC AUC",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="labels", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="value", type="numpy_array")
                        ],
                    ),

                ],
                connections=[
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_1",
                        to_module_id="module_2",
                        from_output_name="data_out",
                        to_input_name="data_in",
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_2",
                        to_module_id="module_3",
                        from_output_name="data_out",
                        to_input_name="data_in",
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_3",
                        to_module_id="module_4",
                        from_output_name="labels",
                        to_input_name="labels",
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_3",
                        to_module_id="module_5",
                        from_output_name="labels",
                        to_input_name="labels",
                    ),
                ],
            )
        elif request.pipeline_id == "pipeline_3":
            return dataflow_ext_pb2.DataflowDescription(
                pipeline_id="pipeline_3",
                modules=[
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_1",
                        type="reading_data",
                        label="Load Data",
                        inputs=[],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_2",
                        type="inputation",
                        label="ISI: Inputation",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_3",
                        type="normalization",
                        label="sklearn: Normalization",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="data_out", type="numpy_array")
                        ],
                    ),
                    dataflow_ext_pb2.DataflowDescription.Module(
                        id="module_4",
                        type="classification",
                        label="sklearn: Gradient Boosting",
                        inputs=[
                            dataflow_ext_pb2.DataflowDescription.Input(name="data_in", type="numpy_array")
                        ],
                        outputs=[
                            dataflow_ext_pb2.DataflowDescription.Output(name="labels", type="numpy_array")
                        ],
                    )
                ],
                connections=[
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_1",
                        to_module_id="module_2",
                        from_output_name="data_out",
                        to_input_name="data_in",
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_2",
                        to_module_id="module_3",
                        from_output_name="data_out",
                        to_input_name="data_in",
                    ),
                    dataflow_ext_pb2.DataflowDescription.Connection(
                        from_module_id="module_3",
                        to_module_id="module_4",
                        from_output_name="data_out",
                        to_input_name="data_in",
                    ),
                ],
            )

    def GetDataflowResults(self, request, context):
        sessioncontext = request.context
        logger.info("Got GetDataflowResults request, session=%s",
                    sessioncontext.session_id)
        
        if request.pipeline_id == "pipeline_1" or request.pipeline_id == "pipeline_3":
            time.sleep(1)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_1",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0
            )
            time.sleep(1)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_2",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
            )
            time.sleep(1)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_3",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.RUNNING,
                progress=0.5,
                execution_time=5.58123,
            )
            time.sleep(10)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_3",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
                execution_time=15.243,
            )
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_4",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
                execution_time=6.135,
            )
        elif request.pipeline_id == "pipeline_2":
            time.sleep(1)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_1",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
            )
            time.sleep(1)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_2",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
            )
            time.sleep(1)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_3",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.RUNNING,
                progress=0.5,
                execution_time=12.235,
            )
            time.sleep(10)
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_3",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
                execution_time=6.234,
            )
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_4",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
                execution_time=5.8455,
            )
            yield dataflow_ext_pb2.ModuleResult(
                module_id="module_5",
                response_info=core_pb2.Response(
                    status=core_pb2.Status(code=core_pb2.OK),
                ),
                status=dataflow_ext_pb2.ModuleResult.DONE,
                progress=1.0,
                execution_time=46.23441,
            )


def dataflowExt(server):
    dataflow_ext_pb2_grpc.add_DataflowExtServicer_to_server(
        DataflowExt(), server)
