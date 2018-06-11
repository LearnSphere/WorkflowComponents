
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import sys
import json
import pprint
import argparse
import csv

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import Settings as stg
from ls_dataset.d3m_dataset import D3MDataset

__version__ = '0.1'

logging.basicConfig()

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Select Dataset")
    parser.add_argument('-ds_name', type=str,
                       help='the name of the dataset to import')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the file list of all datasets')
    args = parser.parse_args()

    # Get config file
    if args.programDir is None:
        config = stg()
    else:
        config = stg(path.join(args.programDir, 'program', 'settings.cfg'))

    # Setup Logging
    setup_logging(config.parse_logging(), args.workingDir, args.is_test == 1)
    logger = logging.getLogger('dataset_selector')

    ### Begin Script ###
    logger.info("Importing D3M Dataset selected by user")
    logger.debug("Running Dataset Import with arguments: %s" % str(args))

    # Get selected dataset name
    if args.ds_name is not None:
        ds_name = args.ds_name
    else:
        ds_name = "baseball"

    # Lookup path to dataset from input file and selected datset option
    ds_reader = csv.reader(args.file0, delimiter='\t')
    for row in ds_reader:
        logger.debug("Line number: %i" % ds_reader.line_num)
        if ds_reader.line_num == 1:
            # Dataset names are the first row
            ds_names = row
        elif ds_reader.line_num == 2:
            # Dataset paths are the second row
            ds_paths = row
    i = ds_names.index(ds_name)
    logger.debug("Index of chosen dataset: %i" % i)
    ds_path = ds_paths[i]
    logger.debug("Corresponding path: %s" % ds_paths)


    # Read in the dataset json
    schema_path = D3MDataset.get_schema_path(ds_path)
    with open(schema_path, 'r') as spath:
        logger.debug("opening dataset schema at path: %s" % schema_path)
        ds_data = json.load(spath)
    logger.debug("using json: %s" % str(ds_data)) 
    ds = D3MDataset(ds_path, ds_data)
    logger.debug("Got dataset: %s" % str(ds))

    # Write dataset info to output file
    out_file_path = path.join(args.workingDir, config.get('Dataset', 'out_file'))
    logger.info("Writing dataset json to: %s" % out_file_path)
    ds.to_component_out_file(out_file_path)
    if args.is_test == 1:
        ds.to_json_pretty(out_file_path + '.readable')
