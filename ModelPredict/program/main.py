
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

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import SettingsFactory
from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.d3m_prediction import D3MPrediction
# from ls_problem_desc.ls_problem import ProblemDesc
# from ls_problem_desc.d3m_problem import D3MProblemDesc
from ls_workflow.workflow import Workflow
from d3m_ta2.ta2_v3_client import TA2Client


__version__ = '0.1'


if __name__ == '__main__':
    # Parse argumennts
    parser = get_default_arg_parser("Model Predict")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for making predictions')
    parser.add_argument('-file1', type=argparse.FileType('r'),
                       help='at tab-delimited list of the fitted models')
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
    logger = logging.getLogger('model_predict')

    ### Begin Script ###
    logger.info("Making predictions with models on given dataset")
    logger.debug("Predicting with models with arguments: %s" % str(args))

    # Open dataset json
    ds = D3MDataset.from_json(args.file0)
    logger.debug("Dataset: %s" % str(ds))

    # Import all the models
    reader = csv.reader(args.file1, delimiter='\t')
    rows = [row for row in reader]
    models =  {mid: Workflow.from_json(rows[1][i]) for i, mid in enumerate(rows[0])}
    logger.info("Got %i models to fit" % len(models))

    # Init the server connection
    address = config.get("TA2", 'ta2_url')
    
    logger.info("using server at address %s" % address)
    serv = TA2Client(address)
    
    # Get Model Predictions
    req_ids = {}
    predictions = {}
    for sid, fsid in fitted_solns.items():
        # req_ids[mid] = serv.produce_solution(model, ds)
        req_ids[fsid] = serv.produce_solution(fsid, solns[sid], ds)
    logger.debug("Created predict solution requests with ids: %s" % str(req_ids))
    for fsid, rid in req_ids.items():
        predictions[fsid] = serv.get_produce_solution_results(rid)

    for fsid, predictions in solution_predictions.items():
        logger.debug("Got predictions from fitted solution, %s: %s" % (fsid, predictions))

    
    # # Write model fit id info to output file
    out_file_path = path.join(args.workingDir, config.get('Output', 'out_file'))
    with open(out_file_path, 'w') as out_file:
        writer = csv.writer(out_file, delimiter='\t')
        writer.writerow([model.id for model in models])
        writer.writerow([model.fit for model in models])


