
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
from modeling.models import Model, ModelScores, Score
from modeling.scores import *


__version__ = '0.1'


if __name__ == '__main__':
    # Parse argumennts
    parser = get_default_arg_parser("Model Score")
    parser.add_argument('-metric', type=str,
                       help='the metric to use to compare the models')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for the search')
    parser.add_argument('-file1', type=argparse.FileType('r'),
                       help='the set of models to score')
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
    logger = logging.getLogger('model_score')

    ### Begin Script ###
    logger.info("Scoring models with selected metric")
    logger.debug("Running Model Scoring with arguments: %s" % str(args))

    # Open dataset json to use for scoring
    ds = D3MDataset.from_component_out_file(args.file0)
    logger.debug("Dataset json parse: %s" % str(ds))

    # Decode the models from file
    logger.debug("Model file input: %s" % args.file1)
    # Read in the the models from tsv
    reader = csv.reader(args.file1, delimiter='\t')
    rows = [row for row in reader]
    # reader.close()
    # Initialize the set of models by model id
    solns = {mid: None for mid in rows[0]}
    for i, mid in enumerate(rows[0]):
        solns[mid] = Model.from_json(rows[1][i])

    # Init the server connection
    address = config.get_ta2_url()
    
    # Crete the metric(s) to use in the score request
    logger.info("using server at address %s" % address)
    serv = TA2Client(address)
   
    # Create the metric(s) to use in the score request
    metric = Metric(args.metric)

    # Get Score for each solution
    score_req_ids = {}
    for soln_id in solns:
        soln = solns[soln_id]
        score_req_ids[soln.id] = serv.score_solution(soln, ds, metrics=[metric])
    scores = {}
    for sid in score_req_ids:
        results = serv.get_score_solution_results(score_req_ids[sid])
        scores[sid] = ModelScores(solns[sid].id, [ds.get_schema_uri()], [Score.from_protobuf(result) for result in results])


    # serv.end_search_solutions(search_id)

    # ### For testing only ###
    # serv.hello()
    # serv.list_primitives()

    # search_id = serv.search_solutions(prob, ds)
    # soln_ids = serv.get_search_solutions_results(search_id)
    # if soln_ids is None:
        # raise Exception("No solution returned")
    # fit_req_ids = {}
    # for sid, soln in solns.items():
        # fit_req_ids[sid] = serv.fit_solution(soln, ds)
    # for sid, rid in fit_req_ids.items():
        # solns[sid].model = serv.get_fit_solution_results(rid)

        

    

        
    out_file_path = path.join(args.workingDir, config.get('Output', 'out_file'))
    with open(out_file_path, 'w') as out_file:
        out = csv.writer(out_file, delimiter='\t')
        out.writerow([sid for sid in scores])
        out.writerow([scores[sid].to_dict() for sid in scores])
        # # out.writerow([scores[sln].to_dict() for sln in solns])
# 
    # # Write dataset info to output file
    # out_file_path = path.join(args.workingDir, config.get('Output', 'dataset_out_file'))
    # ds.to_component_out_file(out_file_path)
    # if args.is_test == 1:
        # # Write out human readable version for debugging
        # ds.to_json_pretty(out_file_path + '.readable')

    # Write Solution workflows to file



