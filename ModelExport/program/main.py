
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import sys
import json
import pprint
import argparse
import csv
import pandas as pd

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from modeling.models import *
from modeling.component_out import *
from d3m_ta2.ta2_client import TA2Client
from d3m_eval.summer_2018.model_generation import RankedPipelineWriter


__version__ = '0.1'

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Export Models")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the tab-separated list of ranked models to export')
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
    logger = logging.getLogger('model_export')

    ### Begin Script ###
    logger.info("Export set of models for d3m evaluation")
    logger.debug("Running Model Export with arguments: %s" % str(args))

    if args.is_test is not None:
        is_test = args.is_test == 1
    else:
        is_test = False

    # Decode the models from file
    logger.debug("ModelExport file input: %s" % args.file0)
    m_index, ranked_models = ModelRankSetIO.from_file(args.file0)

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

    #Create model writer 
    logger.debug("Writing Ranked models to out_dir: %s" % config.get_out_path())
    model_writer = RankedPipelineWriter(config.get_out_path())
    model_writer.write_ranked_models(ranked_models)

    for mid, rmodel in ranked_models.items():
        logger.info("Exporting model via TA2 with id: %s\t and rank: %s" % (mid, rmodel.rank))
        serv.export_solution(rmodel.mdl, rmodel.mdl.id, rmodel.rank)
