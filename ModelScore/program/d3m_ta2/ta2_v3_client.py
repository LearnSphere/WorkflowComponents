
# Author: Steven C. Dang

# Class for most common operations with TA2

import logging
import grpc
from os import path
from google.protobuf.json_format import MessageToJson

# D3M TA2 API imports
from .api_v3 import core_pb2, core_pb2_grpc
from .api_v3 import value_pb2
from .api_v3 import problem_pb2

from ls_problem_desc.d3m_problem import *
from modeling.models import *

logger = logging.getLogger(__name__)

class TA2Client(object):
    """
    A client for common interactions with a TA2 system

    """
    __name__ = "CMU Tigris TA3 v2.0"
    __version__ = "v2018.5.1"
    __protocol_version__ = core_pb2.DESCRIPTOR.GetOptions().Extensions[core_pb2.protocol_version]
    __allowed_values__ = [value_pb2.RAW, value_pb2.DATASET_URI, value_pb2.CSV_URI]
    
    def __init__(self, addr, debug=False, out_dir=None, name=None):
        logger.info("Initializing TA2 Client with address: %s" % addr)
        self.addr = addr
        self.debug = debug
        if debug  and out_dir is not None:
            self.out_dir = out_dir
        else:
            self.out_dir = ""
        self.name = name

        channel = grpc.insecure_channel(addr)
        self.serv = core_pb2_grpc.CoreStub(channel)
        logger.debug("Connected to server")
        # Send hello and parse response for ta2 defaults
        msg = core_pb2.HelloRequest()
        logger.debug("Formed hello request: %s" % str(msg))
        reply = self.serv.Hello(msg)
        logger.debug("Got Response to hello request: %s" % str(reply))
        self.user_agent = reply.user_agent
        self.version = reply.version
        self.supported_extensions = reply.supported_extensions
        self.allowed_values = reply.allowed_value_types
        logger.info("Connected to TA2 System, %s, using api version, %s" % (self.user_agent, self.version))
        logger.debug("TA2 allowed values: %s" % str(self.allowed_values))
        logger.debug("TA2 supported extensions: %s" % str(self.supported_extensions))
        
        self.search_solution_requests = {}
        self.fitted_solution_requests = {}
        self.produce_solution_requests = {}

    def get_id(self):
        return "%s-%s" % (self.__name__, self.__version__)

    def write_msg_to_file(self, msg, file_name):
        """
        Write a given message to file

        """
        with open(path.join(self.out_dir, file_name), 'w') as out_file:
            out_file.write(MessageToJson(msg))
        

    def hello(self):
        """
        Ping the TA2 server and return the result

        """
        logger.info("Sending Hello to TA2 server, %s,  at: %s" % (self.user_agent, self.addr))
        msg = core_pb2.HelloRequest()
        logger.debug("Formed hello request: %s" % str(msg))
        if self.debug:
            self.write_msg_to_file(msg, 'hello_request.json')
        reply = self.serv.Hello(msg)
        logger.debug("Got Response to hello request: %s" % str(reply))
        if self.debug:
            self.write_msg_to_file(reply, 'hello_response.json')
        return reply

    def search_solutions(self, prob, dataset, inputs=None, pipeline=None, max_time=0, priority=0,
            get_request=False):
        """
        Initiate a solution search request

        """

        if type(prob) == GRPCProblemDesc:
            logger.debug("searching with proper GRPC problem description")
            p = prob
        else:
            logger.debug("Converting %s to GRPC problem desc" % str(type(prob)))
            p = GRPCProblemDesc.from_problem_desc(prob)

        
        msg = core_pb2.SearchSolutionsRequest(
            user_agent = self.__name__,
            version = self.__protocol_version__,
            allowed_value_types = self.__allowed_values__,
            time_bound = max_time,
            priority = priority,
            problem = p.to_protobuf(),
        )
        if pipeline is not None:
            msg.template = pipeline
        # else:
            # #Produce a pipeline with only a placeholder
            # pipe = pipeline_pb2.PipelineDescription()
            # pipe.source = self.get_id()
            # pipe.context = pipeline_pb2.TESTING
            # out = pipe.outputs.add()
        
        # Add inputs if given
        if inputs is None:
            i = msg.inputs.add()
            i.dataset_uri = dataset.get_schema_uri()
        else:
            for inpt in inputs:
                i = msg.inputs.add()
                # For now force it into a string until type checking is implemented
                i.string = str(inpt)

        # logger.debug("################################")
        # logger.debug("Sending msg: %s" % str(msg))
        # for ip in msg.inputs:
            # logger.debug("Got file uri: %s" % ip)
            # logger.debug("Got file uri: %s" % ip.dataset_uri)

        if self.debug:
            self.write_msg_to_file(msg, 'search_request.json')

        logger.debug("Sending Search Solution request: %s" % str(msg))
        reply = self.serv.SearchSolutions(msg)
        if self.debug:
            self.write_msg_to_file(reply, 'search_reply.json')

        # Queue the msg for tracking
        self.search_solution_requests[reply.search_id] = msg
        if get_request:
            return reply.search_id, msg
        else:
            return reply.search_id

    def get_search_solutions_results(self, sid):
        logger.info("Geting Search Solution request results for search id: %s" % sid)
        msg = core_pb2.GetSearchSolutionsResultsRequest(
            search_id = sid
        )
        soln_ids = set()
        for reply in self.serv.GetSearchSolutionsResults(msg):
            logger.debug("Got message: %s" % str(reply))
            if reply.solution_id:
                logger.debug("Got a message with a solution id: %s" % reply.solution_id)
                soln_ids.add(reply.solution_id)
            if reply.progress.state == core_pb2.PENDING:
                logger.debug("Search is still pending and hasn't begin")
            elif reply.progress.state == core_pb2.RUNNING:
                logger.debug("Search is currently running and has not completed: %s" % reply.progress.status)
            elif reply.progress.state == core_pb2.COMPLETED:
                logger.info("Search has completed successfully: %s" % reply.progress.status)
            elif reply.progress.state == core_pb2.ERRORED:
                logger.error("Search has completed in an error state: %s" % reply.progress.status)
                raise Exception("Search Solution returned in error: %s" % reply.progress.status)
            else:
                logger.warning("Search is in an unknown state: %s" % str(reply.progress))
        if len(soln_ids) == 0:
            return None
        else:
            return list(soln_ids)

    def end_search_solutions(self, sid):
        msg = core_pb2.EndSearchSolutionsRequest(search_id=sid)
        reply = self.serv.EndSearchSolutions(msg)
        if sid in self.search_solution_requests:
            try: 
                del search_solution_requestsi[sid]
            except KeyError:
                logger.warning("Can't find search with ID, %s, to end search" % sid)
        else:
            logger.warning("Search solution request ID not found. May already have removed this: %s" % sid)
        logger.info("Ended Search for solutions")

    def stop_search_solutions(self, sid):
        msg = core_pb2.StopSearchSolutionsRequest(search_id=sid)
        reply = self.serv.StopSearchSolutions(msg)
        logger.info("Stopped Search for solutions")

    def get_default_scoring_config(self):
        cfg = core_pb2.ScoringConfiguration(
            method = core_pb2.K_FOLD,
            folds = 10,
            train_test_ratio = 5,
            shuffle = True
        )
        return cfg

    def describe_solution(self, sid):
        logger.info("Requesting description of solution with id: %s" % sid)
        msg = core_pb2.DescribeSolutionRequest(
            solution_id = sid
        )
        if self.debug:
            self.write_msg_to_file(msg, 'describe_solution_request.json')
        reply = self.serv.DescribeSolution(msg)
        if self.debug:
            self.write_msg_to_file(msg, 'describe_solution_reply.json')
        logger.debug("Got describe solution reply: %s" % str(reply))
        model = Model(sid)
        model.add_description_from_protobuf(reply.pipeline)
        return model

    def score_solution(self, sln, dataset, inputs=None, metrics=None):
        logger.info("Requesting to score solution with id: %s" % sln.id)
        msg = core_pb2.ScoreSolutionRequest(
            solution_id=sln.id,
            configuration=self.get_default_scoring_config()
        )
        # Add inputs if given
        if inputs is None:
            i = msg.inputs.add()
            i.dataset_uri = dataset.get_schema_uri()
        else:
            for inpt in inputs:
                i = msg.inputs.add()
                # For now force it into a string until type checking is implemented
                i.string = str(inpt)

        # Add metrics if given
        if metrics is None:
            m = msg.performance_metrics.add()
            m.metric = problem_pb2.ACCURACY
        else:
            for met in metrics:
                metric = met.to_protobuf()
                m = msg.performance_metrics.add()
                m.metric = metric.metric
                

        if self.debug:
            self.write_msg_to_file(msg, 'score_solution_request.json')
        logger.debug("Sending Score solution request: \n%s" % str(msg))
        reply = self.serv.ScoreSolution(msg)
        if self.debug:
            self.write_msg_to_file(reply, 'score_solution_reply.json')
        return reply.request_id

    def get_score_solution_results(self, rid):
        logger.info("Getting Score Solution Results with request id: %s" % rid)
        msg = core_pb2.GetScoreSolutionResultsRequest(
            request_id = rid
        )
        soln_scores = []
        for reply in self.serv.GetScoreSolutionResults(msg):
            if reply.progress.state == core_pb2.PENDING:
                logger.debug("Scoring solution is still pending and hasn't begin")
            elif reply.progress.state == core_pb2.RUNNING:
                logger.debug("Scoring solution is currently running and has not completed: %s" % reply.progress.status)
            elif reply.progress.state == core_pb2.COMPLETED:
                logger.info("Scoring solution has completed successfully: %s" % reply.progress.status)
                if self.debug:
                    self.write_msg_to_file(reply, 'score_solution_result_reply.json')
                return reply.scores
            elif reply.progress.state == core_pb2.ERRORED:
                logger.error("Scoring solution has completed in an error state: %s" % reply.progress.status)
                if self.debug:
                    self.write_msg_to_file(reply, 'score_solution_result_reply.json')
            else:
                logger.warning("Scoring solution is in an unknown state: %s" % str(reply.progress))
        
        logger.debug("Returned %i scores-sets" % len(soln_scores))
        for soln in soln_scores:
            logger.debug("Score solution received: %s" % str(soln))
        return soln_scores

    def fit_solution(self, soln, dataset, inputs=None, outputs=None):
        logger.info("Fitting solution with id: %s\t on dataset at: %s" % 
            (soln.id, dataset.get_schema_uri())
        )
        msg = core_pb2.FitSolutionRequest(
            solution_id = soln.id,
        )

        # Add inputs if given
        if inputs is None:
            i = msg.inputs.add()
            i.dataset_uri = dataset.get_schema_uri()
        else:
            for inpt in inputs:
                i = msg.inputs.add()
                # For now force it into a string until type checking is implemented
                i.string = str(inpt)

        # Add list of outputs to expose
        if outputs is None:
            if 'mit' in self.name:
                logger.debug("Using pipeline format 'describe'")
                msg.expose_outputs.extend([soln.get_default_output(format='describe')])
            else:
                logger.debug("Using pipeline format 'name'")
                msg.expose_outputs.extend([soln.get_default_output(format='name')])
            allowed_vals = [val for val in self.allowed_values if val in self.__allowed_values__]
            msg.expose_value_types.extend(allowed_vals)
        
        if self.debug:
            with open(os.path.join(self.out_dir, 'model.json'), 'w') as model_file:
                model_file.write(json.dumps(soln.to_dict()))
            self.write_msg_to_file(msg, 'fit_solution_request.json')
        logger.debug("Sending Fit request msg: %s" % str(msg))
        reply = self.serv.FitSolution(msg)
        if self.debug:
            self.write_msg_to_file(reply, 'fit_solution_reply.json')
        self.fitted_solution_requests[reply.request_id] = msg
        return reply.request_id


    def get_fit_solution_results(self, rid):
        logger.info("Getting fit solution results for request with id: %s" % rid)
        msg = core_pb2.GetFitSolutionResultsRequest(
            request_id = rid
        )
        results = None
        for reply in self.serv.GetFitSolutionResults(msg):

            if reply.progress.state == core_pb2.PENDING:
                logger.debug("Fitting model to solution is still pending and hasn't begin")
            elif reply.progress.state == core_pb2.RUNNING:
                logger.debug("Fitting model to solution is currently running and has not completed: %s" % reply.progress.status)
            elif reply.progress.state == core_pb2.COMPLETED:
                logger.info("Fitting model to solution has completed successfully: %s" % reply.progress.status)
                # logger.debug("Got reply: %s" % str(reply))
                results = reply
            elif reply.progress.state == core_pb2.ERRORED:
                logger.error("Fitting model to solution has completed in an error state: %s" % reply.progress.status)
            else:
                logger.warning("Fittin model to solution is in an unknown state: %s" % str(reply.progress))
       
        request = self.fitted_solution_requests.pop(rid, None)

        return results.fitted_solution_id, results.exposed_outputs
        
    def produce_solution(self, fsid, soln, ds, inputs=None, outputs=None):
        logger.info("Produce predictions for solution with id: %s" % soln.id)
        msg = core_pb2.ProduceSolutionRequest(
            fitted_solution_id = fsid
        )
        # Add inputs if given
        if inputs is None:
            i = msg.inputs.add()
            i.dataset_uri = ds.get_schema_uri()
        else:
            for inpt in inputs:
                i = msg.inputs.add()
                # For now force it into a string until type checking is implemented
                i.string = str(inpt)

        # Add list of outputs to expose
        if outputs is None:
            if 'mit' in self.name:
                logger.debug("Using pipeline format 'describe'")
                msg.expose_outputs.extend([soln.get_default_output(format='describe')])
            else:
                logger.debug("Using pipeline format 'name'")
                msg.expose_outputs.extend([soln.get_default_output(format='name')])
            allowed_vals = [val for val in self.allowed_values if val in self.__allowed_values__]
            msg.expose_value_types.extend(allowed_vals)
        
        logger.info("****************************************")
        msg_js = json_format.MessageToJson(msg)
        logger.info("Sending produce solution with msg: %s" % msg_js)
        logger.info("****************************************")
        if self.debug:
            self.write_msg_to_file(msg, 'produce_solution_msg.json')
        reply = self.serv.ProduceSolution(msg)
        if self.debug:
            self.write_msg_to_file(reply, 'produce_solution_reply.json')
        self.produce_solution_requests[reply.request_id] = msg

        return reply.request_id

    def get_produce_solution_results(self, req_id):
        logger.info("Getting ProduceSolutionRequest results with id: %s" % req_id)
        msg = core_pb2.GetProduceSolutionResultsRequest(
                request_id = req_id
        )

        for reply in self.serv.GetProduceSolutionResults(msg):
            if reply.progress.state == core_pb2.PENDING:
                logger.debug("Fitting model to solution is still pending and hasn't begin")
            elif reply.progress.state == core_pb2.RUNNING:
                logger.debug("Fitting model to solution is currently running and has not completed: %s" % reply.progress.status)
            elif reply.progress.state == core_pb2.COMPLETED:
                logger.info("Fitting model to solution has completed successfully: %s" % reply.progress.status)
                return reply.exposed_outputs
            elif reply.progress.state == core_pb2.ERRORED:
                logger.error("Fitting model to solution has completed in an error state: %s" % reply.progress.status)
            else:
                logger.warning("Fittin model to solution is in an unknown state: %s" % str(reply.progress))

        # logger.debug("Got %i completed responses" % len(replies))
        # fitted_ids = [reply.fitted_solution_id for reply in replies]
        request = self.produce_solution_requests.pop(rid, None)


    def list_primitives(self):
        logger.info("Getting list of TA2 primitives supported")
        msg = core_pb2.ListPrimitivesRequest()

        reply = self.serv.ListPrimitives(msg)
        logger.debug("Got reply: %s" % str(reply))
        return reply.primitives

    def export_solution(self, model, fit_id, rank):
        logger.info("Requesting export of solution with id; %s" % model.id)

        if not model.has_fit(fit_id):
            raise Exception("Model does not have a fit matching, %s\nAvailable fits: %s" % 
                            (fit_id, [fit.id for fit in model.fitted]))
        
        if self.debug:
            self.write_msg_to_file(msg, 'export_solution_request.json')
        msg = core_pb2.SolutionExportRequest(
                fitted_solution_id  = fit_id,
                rank = rank
        )
        self.serv.SolutionExport(msg)

                








     




