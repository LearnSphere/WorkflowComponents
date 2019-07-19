
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

import pandas as pd

from google.protobuf import json_format

# Workflow component specific imports
from ls_utilities.ls_wf_settings import SettingsFactory
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.d3m_prediction import D3MPrediction
from ls_problem_desc.ls_problem import ProblemDesc
from ls_problem_desc.d3m_problem import DefaultProblemDesc
from d3m_ta2.ta2_client import TA2Client
from d3m_eval.summer_2018.prob_discovery import ProblemDiscoveryWriter
# from dxdb.workflow_session import ProblemCreatorSession
from dxdb.dx_db import DXDB
from dxdb.workflow_session import ModelSearchSession
# from ls_workflow.workflow import Workflow as Solution
from ls_utilities.dexplorer import *
from modeling.models import *
from modeling.component_out import *
from user_ops.modeling import *


__version__ = '0.1'


if __name__ == '__main__':
    # Parse argumennts
    parser = get_default_arg_parser("Model Search")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the workflow session json from a problem session')
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
    logger = logging.getLogger('model_search')

    ### Begin Script ###
    logger.info("Running Pipeline Search on TA2")
    logger.debug("Running D3M Pipeline Search with arguments: %s" % str(args))

    # Get connection to db
    logger.debug("DB URL: %s" % dx_config.get_db_backend_url())
    db = DXDB(dx_config.get_db_backend_url())

    # Getting dataset session from db
    sess_json = json.load(args.file0, encoding='utf-16')
    prev_sess = db.get_workflow_session(sess_json['_id'])
    logger.debug("recovered input session: %s" % prev_sess.to_json())

    # Checking if problem has been defined 
    if not prev_sess.is_state_complete():
        logger.error("Problem Definition has not been completed yet. Cannot conduct solution search without defining problem with problem creator tool")
        raise Exception("Cannot initialize tool, incomplete problem definition  specified yet")
    else:
        prob = db.get_problem(prev_sess.prob_id)
        logger.debug("Retrieved problem from db for initializing model search: %s" % prob.to_json())
        ds = db.get_dataset_metadata(prev_sess.dataset_id)
        logger.debug("Retrieved dataset info from db for initializing model search: %s" % ds.to_json())

    # Get Session Metadata
    user_id = args.userId
    logger.debug("User ID: %s" % user_id)
    workflow_id = os.path.split(os.path.abspath(args.workflowDir))[1]
    logger.debug("Workflow ID: %s" % workflow_id)
    comp_type = os.path.split(os.path.abspath(args.toolDir))[1]
    logger.debug("Component Type: %s" % comp_type)
    comp_id = os.path.split(os.path.abspath(args.componentXmlFile))[1].split(".")[0]
    logger.debug("Component Id: %s" % comp_id)


    # Initialize new session
    session = ModelSearchSession(user_id=user_id, workflow_id=workflow_id, 
                                   comp_type=comp_type, comp_id=comp_id)
    session.set_dataset_id(ds._id)
    session.set_problem_id(prob._id)
    session.set_input_wfids([prev_sess._id])
    session = db.add_workflow_session(session)
    logger.debug("Created new Workflow Session: %s" % session.to_json())

    # get Connection to Dexplorer Service
    dex = DexplorerUIServer(dx_config.get_dexplorer_url())
    session.session_url = dex.get_model_search_ui_url(session)
    logger.debug('***************************************************')
    logger.debug('session before updating db %s' % str(session.to_json()))
    db.update_workflow_session(session, 'session_url')
    logger.debug("added Dexplorer url to session: %s" % session.session_url)

    # Init the TA2 server connection
    address = config.get_ta2_url()
    name = config.get_ta2_name()
    
    logger.info("using server at address %s" % address)
    if is_test:
        serv = TA2Client(address, debug=True, out_dir=args.workingDir, 
                name=name)
    else:
        serv = TA2Client(address, 
                name=name)
    session.ta2_addr = address
    db.update_workflow_session(session, 'ta2_addr')
    logger.debug("added TA2 address url to session: %s" % session.ta2_addr)


    if config.get_mode() == 'D3M':
        # Write search and problem to file for problem discovery task
        out_path = config.get_out_path()
    else:
        out_path = None

    runner = ModelSearch()
    m_index, models, result_df, score_data, ranked_models = runner.run(ds, prob, serv, out_path)


    # Write html ui to output file
    out_file_path = path.join(args.workingDir, 
                              config.get('Output', 'ui_out_file')
                              )
    logger.info("Writing output html to: %s" % out_file_path)
    logger.debug("Embedded iframe url: %s" % session.session_url)
    out_html = '<iframe src="http://%s" width="1024" height="768"></iframe>' % session.session_url
    with open(out_file_path, 'w') as out_file:
        out_file.write(out_html)

    # Write session info to output file
    out_file_path = path.join(args.workingDir, config.get('Output', 'session_out_file'))
    logger.info("Writing session info to file: %s" % (out_file_path))
    out_data = session.to_json()
    logger.debug("Session json to write out: %s" % out_data)

    # Write ranked model list to file
    with open(out_file_path, 'w') as out_file:
        out_file.write(out_data)
        
    out_file_path = path.join(args.workingDir, config.get('Output', 'ranked_model_file'))
    ModelRankSetIO.to_file(out_file_path, ranked_models, m_index)
