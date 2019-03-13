
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
from ls_problem_desc.ls_problem import *

__version__ = '0.1'


if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Select Problem Metric")
    parser.add_argument('-metric', type=str,
                       help='the metric the user selected')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the problem description doc')
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
    logger = logging.getLogger('metric_selector')

    ### Begin Script ###
    logger.info("Adding a problem metric to a given problem")
    logger.debug("Running Problem Metric Selection with arguments: %s" % str(args))

    # Get the Problem Doc to forulate the Pipeline request
    logger.debug("Problem input: %s" % args.file0)
    prob = ProblemDesc.from_file(args.file0)
    logger.debug("Got Problem Description: %s" % prob.print())

    ###### Maybe add validation logic here? #####
    prob.add_metric(args.metric)

    # Write Problem Template with task type and metric options to output file
    out_file_path = path.join(args.workingDir, config.get('Dataset', 'out_file'))
    logger.info("Writing list of metrics to: %s" % out_file_path)
    with open(out_file_path, 'w') as out_file:
        writer = csv.writer(out_file, delimiter='\t')
        writer.writerow([args.metric])
