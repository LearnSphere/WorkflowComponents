
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
# from ls_problem_desc.d3m_problem import *
from ls_problem_desc.ls_problem import *

__version__ = '0.1'


if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Initialize a new problem")
    parser.add_argument('-probname', type=str,
                       help='the name of the new problem given by the user')
    parser.add_argument('-probdesc', type=str,
                       help='the plain text description of the problem supplied by the user')
    parser.add_argument('-targetname', type=str,
                       help='the name of the column from the dataset to use')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the description of the dataset')
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
    logger = logging.getLogger('problem_creator')

    ### Begin Script ###
    logger.info("Initializing Problem Description for a dataset with selected column")
    logger.debug("Running Problem Creator with arguments: %s" % str(args))

    # Open dataset json
    ds = D3MDataset.from_component_out_file(args.file0)
    logger.debug("Dataset json parse: %s" % str(ds))

    # Get the information about the selected target from the dataset
    for resource in ds.dataResources:
        if resource.resType == 'table':
            logger.debug("Looking for column in resource: %s" % str(resource))
            cols = resource.columns
            logger.debug("Got resource columns: %s" % str([str(col) for col in cols]))
            col_names = [col.colName for col in cols]
            logger.debug("Got column names: %s" % col_names)
            i = col_names.index(args.targetname)
            target_col = cols[i]
            target_resource = resource
            logger.debug("Got target column from resource with ID, %s, at index %i: %s" % (target_resource.resID, i, str(target_col)))

    if target_col is None:
        raise Exception("Could not identify column with name %s from dataset" % args.targetname)

    # Initialize a Problem Description and set target info
    prob = ProblemDesc()
    # prob.description = "CMU-Tigris User generated problem"
    # prob.name = "Problem-%s" % str(datetime.now())
    prob.name = args.probname
    prob.description = args.probdesc
    prob.add_input(ds.id, target_resource, target_col)

    # Write Problem Template with selected target to output file
    out_file_path = path.join(args.workingDir, config.get('Dataset', 'out_file'))
    logger.info("Writing template prob desc to: %s" % out_file_path)
    ModelingProblem.problem_target_select_to_file(prob, out_file_path)
    if args.is_test == 1:
        prob.print(out_file_path + '.readable')
