
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
from ls_utilities.cmd_parser import *
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from modeling.models import *
from modeling.component_out import *
from d3m_ta2.ta2_client import TA2Client
from d3m_eval.summer_2018.model_generation import RankedPipelineWriter
from dxdb.dx_db import DXDB
from dxdb.workflow_session import ModelSearchSession, WorkflowSession
from user_ops.modeling import *

__version__ = '0.1'

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Export Models")
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
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
    # Get config for dx services
    dx_config = SettingsFactory.get_dx_settings()


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

    # Get connection to db
    logger.debug("DB URL: %s" % dx_config.get_db_backend_url())
    db = DXDB(dx_config.get_db_backend_url())

    # Getting workflow session from db
    # Parsing inputs
    session_indx = 0
    session_files = get_input_files(args, session_indx)
    # Assume only 1 session input file
    try:
        with open(session_files[0], 'r') as session_file:
            sess_json = json.load(session_file, encoding='utf-16')
    except Exception as e:
        logger.error("Error while loading session json at %s" % session_files[0])
    obj = db.get_object('wf_sessions', sess_json['_id'])
    prev_sess = WorkflowSession.from_json(obj) 
    logger.debug("read input session from file: %s" % prev_sess.to_json())
    prev_sess = db.get_workflow_session(prev_sess._id)
    logger.debug("recovered session from db: %s" % prev_sess.to_json())
    
    # Get Ranked Models from db
    ranked_models = []
    for rm_id in prev_sess.ranked_mdl_ids:
        rm = RankedModel.from_json(db.get_object('ranked_models', rm_id))
        logger.debug("Got ranked model from db to export: %s" % str(rm))
        ranked_models.append(rm)

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
    runner = ModelExporter()
    runner.run(config.get_out_path(), ranked_models, serv) 

    serv.end_search_solutions(prev_sess.search_id)
    logger.info("Ended search solution after exporting: %s" % prev_sess.search_id)
# logger.debug("Writing Ranked models to out_dir: %s" % config.get_out_path())
    # model_writer = RankedPipelineWriter(config.get_out_path())
    # model_writer.write_ranked_models(ranked_models)

    # for mid, rmodel in ranked_models.items():
        # logger.info("Exporting model via TA2 with id: %s\t and rank: %s" % (mid, rmodel.rank))
        # serv.export_solution(rmodel.mdl, rmodel.mdl.id, rmodel.rank)
