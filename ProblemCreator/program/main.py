
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import hashlib
from datetime import datetime
import os.path as path
import sys
import json
import pprint
import argparse
import csv

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from ls_problem_desc.d3m_problem import DefaultProblemDesc
from ls_problem_desc.ls_problem import *
from dxdb.dx_db import DXDB
from dxdb.workflow_session import ProblemCreatorSession
from ls_utilities.dexplorer import *
from user_ops.problem import *

__version__ = '0.1'


if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Initialize a new problem")
    # parser.add_argument('-probname', type=str,
                       # help='the name of the new problem given by the user')
    # parser.add_argument('-probdesc', type=str,
                       # help='the plain text description of the problem supplied by the user')
    # parser.add_argument('-targetname', type=str,
                       # help='the name of the column from the dataset to use')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset selection session information')
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
    logger = logging.getLogger('problem_creator')

    ### Begin Script ###
    logger.info("Initializing Problem Description for a dataset with selected column")
    logger.debug("Running Problem Creator with arguments: %s" % str(args))

    # Get connection to db
    logger.debug("DB URL: %s" % dx_config.get_db_backend_url())
    db = DXDB(dx_config.get_db_backend_url())

    # Getting dataset session from db
    sess_json = json.load(args.file0, encoding='utf-16')
    ds_sess = db.get_workflow_session(sess_json['_id'])
    logger.debug("recovered dataset session: %s" % ds_sess.to_json())

    # Checking if dataset has been selected in dataset session
    if not ds_sess.is_state_complete() or ds_sess.dataset_id is None:
        logger.error("Dataset has not been selected yet. Cannot create problem without selecting dataset from dataset importer tool")
        raise Exception("Cannot initialize tool, no dataset specified yet")
    else:
        ds = db.get_dataset_metadata(ds_sess.dataset_id)
        logger.debug("Retrieved dataset from db for initializing problem: %s" % ds.to_json())

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
    session = ProblemCreatorSession(user_id=user_id, workflow_id=workflow_id, 
                                   comp_type=comp_type, comp_id=comp_id)
    session.set_dataset_id(ds._id)
    session.set_input_wfids([ds_sess._id])
    session = db.add_workflow_session(session)
    logger.debug("Created new Workflow Session: %s" % session.to_json())

    # get Connection to Dexplorer Service
    dex = DexplorerUIServer(dx_config.get_dexplorer_url())
    session.session_url = dex.get_problem_creator_ui_url(session)
    logger.debug('***************************************************')
    logger.debug('session before updating db %s' % str(session.to_json()))
    db.update_workflow_session(session, 'session_url')
    logger.debug("added Dexplorer url to session: %s" % session.session_url)


    # Open dataset json
    # ds = D3MDataset.from_component_out_file(args.file0)
    # logger.debug("Dataset json parse: %s" % str(ds))

    # Get the information about the selected target from the dataset
    # for resource in ds.dataResources:
        # if resource.resType == 'table':
            # logger.debug("Looking for column in resource: %s" % str(resource))
            # cols = resource.columns
            # logger.debug("Got resource columns: %s" % str([str(col) for col in cols]))
            # col_names = [col.colName for col in cols]
            # logger.debug("Got column names: %s" % col_names)
            # i = col_names.index(args.targetname)
            # target_col = cols[i]
            # target_resource = resource
            # logger.debug("Got target column from resource with ID, %s, at index %i: %s" % (target_resource.resID, i, str(target_col)))

    # if target_col is None:
        # raise Exception("Could not identify column with name %s from dataset" % args.targetname)

    # # Initialize a Problem Description and set target info
    prob = ProblemDesc()
    prob._id = db.insert_problem(prob)
    logger.debug("Initialized problem: %s" % str(prob))
    session.prob_id = prob._id
    db.update_workflow_session(session, ['prob_id'])

    # Get Default problem description and add as suggestion
    runner = DefaultProblemGenerator()
    def_prob = runner.run(ds)
    def_prob._id = db.insert_problem(def_prob)
    session.add_suggestion_prob(def_prob)
    db.update_workflow_session(session, ['suggest_pids'])

    # Set session state to ready
    session.set_state_ready()
    db.update_workflow_session(session, ['state'])

    # Test retrieve problem
    # temp_prob = db.get_problem(prob._id)
    # logger.debug("********************************************")
    # logger.debug("********************************************")
    # logger.debug("Got problem from DB: %s" % str(temp_prob))

    
    # # prob.description = "CMU-Tigris User generated problem"
    # # prob.name = "Problem-%s" % str(datetime.now())
    # prob.name = args.probname
    # prob.description = args.probdesc
    # prob.add_input(ds.id, target_resource, target_col)

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
    with open(out_file_path, 'w') as out_file:
        out_file.write(out_data)
