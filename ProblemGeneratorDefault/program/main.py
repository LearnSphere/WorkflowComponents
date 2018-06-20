
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import sys
import json
import argparse

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from ls_problem_desc.d3m_problem import DefaultProblemDesc
from ls_problem_desc.ls_problem import ProblemDesc

__version__ = '0.1'


if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Generate Default Problem")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for the search')
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
    logger = logging.getLogger('problem_generator_default')

    ### Begin Script ###
    logger.info("Generating Problem Statement based on default problem for given dataset")
    logger.debug("Running Generate Default Problem with arguments: %s" % str(args))

    # Open dataset json
    ds = D3MDataset.from_component_out_file(args.file0)
    logger.debug("Dataset json: %s" % str(ds))

    # Get Problem Schema from Dataset
    prob_path = DefaultProblemDesc.get_default_problem(ds)
    prob_desc = DefaultProblemDesc.from_file(prob_path)
    logger.debug("Got Problem Description for json: %s" % prob_desc.print())

    # Write dataset info to output file
    out_file_path = path.join(args.workingDir, config.get('Main', 'out_file'))
    logger.info("Writing dataset json to: %s" % out_file_path)
    prob_desc.to_file(out_file_path)
    if args.is_test == 1:
        prob_desc.print(out_file_path + '.readable')
