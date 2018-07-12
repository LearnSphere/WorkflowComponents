
# Author: Steven C. Dang

# Main script for runnign a pipeline search on the DARPA D3M system


from __future__ import absolute_import, division, print_function

import grpc
import logging
import json
import sys
from os import path
import argparse
import pprint
import csv

from google.protobuf import json_format

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.d3m_prediction import D3MPrediction
from ls_problem_desc.ls_problem import ProblemDesc
from ls_problem_desc.d3m_problem import DefaultProblemDesc
from d3m_ta2.ta2_v3_client import TA2Client
# from ls_workflow.workflow import Workflow as Solution
from modeling.models import *
from modeling.component_out import *


__version__ = '0.1'


if __name__ == '__main__':
    # Parse argumennts
    parser = get_default_arg_parser("Model Search")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for the search')
    parser.add_argument('-file1', type=argparse.FileType('r'),
                       help='the problem json provided for the search')
    args = parser.parse_args()

    if args.is_test is not None:
        is_test = args.is_test == 1
    else:
        is_test = False

    # Get config file
    config = SettingsFactory.get_settings(path.join(args.programDir, 'program', 'settings.cfg'), 
                                          program_dir=args.programDir,
                                          working_dir=args.workingDir,
                                          is_test=is_test
                                          )

    # Setup Logging
    setup_logging(config)
    logger = logging.getLogger('model_search')

    ### Begin Script ###
    logger.info("Running Pipeline Search on TA2")
    logger.debug("Running D3M Pipeline Search with arguments: %s" % str(args))

    # Open dataset json
    ds = D3MDataset.from_component_out_file(args.file0)
    logger.debug("Dataset json parse: %s" % str(ds))

    # Get the Problem Doc to forulate the Pipeline request
    logger.debug("Problem input: %s" % args.file1)
    prob = ProblemDesc.from_file(args.file1)
    logger.debug("Got Problem Description: %s" % prob.print())

    # Init the server connection
    address = config.get_ta2_url()
    name = config.get_ta2_name()
    
    logger.info("using server at address %s" % address)
    if is_test:
        serv = TA2Client(address, debug=True, out_dir=args.workingDir, 
                name=name)
    else:
        serv = TA2Client(address, 
                name=name)
    

#     serv.hello()

    # Search for solutions
    search_id = serv.search_solutions(prob, ds)
    soln_ids = serv.get_search_solutions_results(search_id)
    if soln_ids is None:
        raise Exception("No solution returned")
    
    # Get Model for each solution returned
    solns = {}
    for soln_id in soln_ids:
        solns[soln_id] = serv.describe_solution(soln_id)
        logger.debug("Got pipeline descripton for solution id %s: \n%s" % (soln_id, str(solns[soln_id])))

    # Get Score for each solution
    # score_req_ids = {}
    # for soln_id in solns:
        # soln = solns[soln_id]
        # score_req_ids[soln.id] = serv.score_solution(soln, ds)
    # scores = {}
    # for sid in score_req_ids:
        # results = serv.get_score_solution_results(score_req_ids[sid])
        # scores[sid] = ModelScores(solns[sid].id, [ds.get_schema_uri()], [Score.from_protobuf(result) for result in results])


    # serv.end_search_solutions(search_id)

    # ### For testing only ###
    # serv.hello()
    # serv.list_primitives()

    # search_id = serv.search_solutions(prob, ds)
    # soln_ids = serv.get_search_solutions_results(search_id)
    # if soln_ids is None:
        # raise Exception("No solution returned") 

    # fit_req_ids = {}
    # fitted_solns = {}
    # fitted_results = {}
    # for sid, soln in solns.items():
        # fit_req_ids[sid] = serv.fit_solution(soln, ds)
    # for sid, rid in fit_req_ids.items():
        # logger.debug("solution id: %s\tfit solution request id: %s" % (sid, rid))
        # fitted_solns[sid], fitted_results[sid] = serv.get_fit_solution_results(rid)
# 
    # logger.debug("Got fitted solutions with ids: %s" % str(fitted_solns) )
# 
   #  
    # req_ids = {}
    # solution_predictions = {}
    # for sid, fsid in fitted_solns.items():
        # # req_ids[mid] = serv.produce_solution(model, ds)
        # req_ids[fsid] = serv.produce_solution(fsid, solns[sid], ds)
    # logger.debug("Created predoce solution requests with ids: %s" % str(req_ids))
    # for fsid, rid in req_ids.items():
        # solution_predictions[fsid] = serv.get_produce_solution_results(rid)
# 
    # for fsid, predictions in solution_predictions.items():
        # logger.debug("Got predictions from fitted solution, %s: %s" % (fsid, predictions))


    # serv.end_search_solutions(search_id)

    ### End testing code ###
   
    # Write the received solutions to file
    # for sid, soln in solns.items():
        # logger.debug("###########################################")
        # logger.debug("Received solution: %s" % str(soln.to_dict()))
        # logger.debug("###########################################")
        
    out_file_path = path.join(args.workingDir, config.get('Output', 'model_out_file'))
    ModelSetIO.to_file(out_file_path, solns)
    # with open(out_file_path, 'w') as out_file:
        # out = csv.writer(out_file, delimiter='\t')
        # out.writerow([solns[sln].id for sln in solns])
        # out.writerow([solns[sln].to_dict() for sln in solns])
        # out.writerow([scores[sln].to_dict() for sln in solns])

    # Write dataset info to output file
    out_file_path = path.join(args.workingDir, config.get('Output', 'dataset_out_file'))
    ds.to_component_out_file(out_file_path)
    if args.is_test == 1:
        # Write out human readable version for debugging
        ds.to_json_pretty(out_file_path + '.readable')

    # Write Solution workflows to file



