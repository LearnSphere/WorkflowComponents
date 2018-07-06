import proto.core_pb2 as core_pb2
import proto.data_ext_pb2_grpc as data_ext_pb2_grpc

from common import logger


class DataExt(data_ext_pb2_grpc.DataExtServicer):
    def AddFeatures(self, request, context):
        sessioncontext = request.context
        logger.info("Got AddFeatures request, session=%s",
                    sessioncontext.session_id)
        return core_pb2.Response(
            status=core_pb2.Status(
                code=core_pb2.OK,
                details="OK adding features..."
            )
        )

    def RemoveFeatures(self, request, context):
        return core_pb2.Response(
            status=core_pb2.Status(
                code=core_pb2.OK,
                details="OK removing features..."
            )
        )
        

def dataExt(server):
    data_ext_pb2_grpc.add_DataExtServicer_to_server(
        DataExt(), server)
