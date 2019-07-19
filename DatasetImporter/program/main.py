
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import os
import csv

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from user_ops.dataset import DatasetImporter
from dxdb.dx_db import DXDB
from dxdb.workflow_session import ImportDatasetSession
from ls_utilities.dexplorer import *

__version__ = '0.1'

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Import List of available D3M Datasets")
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
    logger = logging.getLogger('datasets_importer')

    ### Begin Script ###
    logger.info("Importing List of available datasets")
    logger.debug("Running Dataset Importer with arguments: %s" % str(args))

    # Get connection to db
    logger.debug("DB URL: %s" % dx_config.get_db_backend_url())
    db = DXDB(dx_config.get_db_backend_url())

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
    session = ImportDatasetSession(user_id=user_id, workflow_id=workflow_id, 
                                   comp_type=comp_type, comp_id=comp_id)
    session = db.add_workflow_session(session)
    logger.debug("Created new Dataset Import Session: %s" % session.to_json())

    # get Connection to Dexplorer Service
    dex = DexplorerUIServer(dx_config.get_dexplorer_url())
    session.session_url = dex.get_dataset_importer_ui_url(session)
    logger.debug('***************************************************')
    logger.debug('session before updating db %s' % str(session.to_json()))
    db.update_workflow_session(session, 'session_url')
    logger.debug("added Dexplorer url to session: %s" % session.session_url)

    # Testing recovering session
    # sess = db.get_workflow_session(session._id)
    # logger.debug("recovered session: %s" % sess.to_json())

    # Read in the dataset json
    ds_root = config.get_dataset_path()
    runner = DatasetImporter(db, session)
    datasets = runner.run(ds_root)


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
    logger.info("Writing session info to select from %i datasets to file: %s" % (len(datasets), out_file_path))
    out_data = session.to_json()
    logger.debug("Session json to write out: %s" % out_data)
    with open(out_file_path, 'w') as out_file:
        out_file.write(out_data)
