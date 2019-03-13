
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
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset

__version__ = '0.1'

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Select Dataset")
    parser.add_argument('-ds_name', type=str,
                       help='the name of the dataset to import')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the file list of all datasets')
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
    logger = logging.getLogger('dataset_selector')

    ### Begin Script ###
    logger.info("Importing D3M Dataset selected by user")
    logger.debug("Running Dataset Import with arguments: %s" % str(args))

    # Get selected dataset name
    if args.ds_name is not None:
        ds_name = args.ds_name
    else:
        ds_name = "baseball"

    if args.is_test is not None:
        is_test = args.is_test == 1
    else:
        is_test = False

    # Lookup path to dataset from input file and selected datset option
    ds_reader = csv.reader(args.file0, delimiter='\t')
    for row in ds_reader:
        logger.debug("Line number: %i" % ds_reader.line_num)
        if ds_reader.line_num == 1:
            logger.debug("Reading first line of dataset file")
            # Dataset names are the first row
            ds_names = row
        elif ds_reader.line_num == 2:
            logger.debug("Reading second line of dataset file")
            # Dataset paths are the second row
            ds_paths = row
    i = ds_names.index(ds_name)
    logger.debug("Index of chosen dataset: %i" % i)
    name = ds_names[i]
    logger.info("Selected Dataset: %s" % name)

    # Crawl dataset directory to find dataset json with matching name
    ds_root = config.get_dataset_path()
    names = set()
    datasets = {}

    for root, dirs, files in os.walk(ds_root):
        for f in files:
            if f == 'datasetDoc.json':
                logger.debug("Found dataset in directory: %s" % root)
                try: 
                    ds = D3MDataset.from_dataset_json(path.join(root, f))
                    if ds.name not in names:
                        logger.info("Found dataset name: %s\nAt path: %s" % (ds.name,  ds.dpath))
                        names.add(ds.name)
                        datasets[ds.name] = ds
                except:
                    logger.warning("Error encountered whiel loading dataset json: %s" % path.join(root, f))

    ds = datasets[name]

    logger.info("Found dataset with name %s, id: %s\n json: %s" % (ds.name, ds.id, str(ds)))

    # Read in the dataset json
    # schema_path = D3MDataset.get_schema_path(ds)
    # with open(schema_path, 'r') as spath:
        # logger.debug("opening dataset schema at path: %s" % schema_path)
        # ds_data = json.load(spath)
    # logger.debug("using json: %s" % str(ds_data)) 
    # ds = D3MDataset(ds_path, ds_data)
    logger.debug("Got dataset: %s" % str(ds))

    # Write dataset info to output file
    out_file_path = path.join(args.workingDir, config.get('Dataset', 'out_file'))
    logger.info("Writing dataset json to: %s" % out_file_path)
    ds.to_component_out_file(out_file_path)
    if args.is_test == 1:
        ds.to_json_pretty(out_file_path + '.readable')
