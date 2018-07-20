
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
# from ls_utilities.ls_wf_settings import Settings as stg
from ls_utilities.ls_wf_settings import SettingsFactory
from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.d3m_prediction import D3MPrediction
from d3m_ta2.ta2_v3_client import TA2Client
from modeling.models import *
from modeling.component_out import *


__version__ = '0.1'


if __name__ == '__main__':
    # Parse argumennts
    parser = get_default_arg_parser("Fit Models")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for the search')
    parser.add_argument('-file1', type=argparse.FileType('r'),
                       help='at tab-delimited list of models to fit')
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
    logger = logging.getLogger('model_fit')

    ### Begin Script ###
    logger.info("Fitting models using given dataset")
    logger.debug("Fitting models with arguments: %s" % str(args))

    # Open dataset json
    ds = D3MDataset.from_component_out_file(args.file0)
    logger.debug("Dataset json parse: %s" % str(ds))

    # Decode the models from file
    logger.debug("Model file input: %s" % args.file1)
    m_index, models = ModelSetIO.from_file(args.file1)
    # Read in the the models from tsv
    # reader = csv.reader(args.file1, delimiter='\t')
    # rows = [row for row in reader]

    # Initialize the set of models by model id
    # models = {mid: None for mid in rows[0]}
    # for i, mid in enumerate(rows[0]):
        # models[mid] = Model.from_json(rows[1][i])

    # Init the server connection
    address = config.get_ta2_url()
    name = config.get_ta2_name()
    logger.info("using server, %s, at address %s" % (name, address))
    if is_test:
        serv = TA2Client(address, debug=True, out_dir=args.workingDir, 
                name=name)
    else:
        serv = TA2Client(address, 
                name=name)
                
    
    # Get fitted solution
    fit_req_ids = {}
    #fitted_models = {}
    fitted_results = {}
    for mid, model in models.items():
        logger.debug("Fitting model: %s" % str(model))
        fit_req_ids[mid] = serv.fit_solution(model, ds)
    for mid, rid in fit_req_ids.items():
        logger.debug("Model id: %s\tfit model request id: %s" % (mid, rid))
        models[mid].fitted_id, fitted_results[mid] = serv.get_fit_solution_results(rid)

    for mid in models:
        logger.debug("Got fitted model with model id: %s" % mid)
        logger.debug("Model: %s\tFitted Model: %s" % (mid, models[mid].fitted_id))
    
    # # Write model fit id info to output file
    out_file_path = path.join(args.workingDir, config.get('Output', 'out_file'))

    FittedModelSetIO.to_file(out_file_path, models, m_index)
    # row2 = [] # Model id
    # row3 = [] # Fitted Model id
    # row4 = [] # Model json
    # for mid in models:
        # row1 = mids[mid]
        # row2.append(mid)
        # row3.append(fitted_models[mid])
        # row4.append(models[mid].to_dict())

    # with open(out_file_path, 'w') as out_file:
        # writer = csv.writer(out_file, delimiter='\t')
        # writer.writerow(row2)
        # writer.writerow(row3)
        # writer.writerow(row4)


    out_file_path = path.join(args.workingDir, config.get('Output', 'data_out_file'))
    # Just write out original dataset for now without adding in the fitted data
    ds.to_component_out_file(out_file_path)
    if args.is_test == 1:
        ds.to_json_pretty(out_file_path + '.readable')
