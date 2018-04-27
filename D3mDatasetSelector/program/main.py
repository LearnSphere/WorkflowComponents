
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
from logging.handlers import SysLogHandler
from logging import StreamHandler
from logging import FileHandler
# from logging.handlers import SysLogHandler, FileHandler, StreamHandler
from datetime import datetime as dt
import os.path as path
import sys
import json
import argparse

__version__ = '0.1'
__dataset_file_name__ = "datasetDoc.json"

logging.basicConfig()
logger = logging.getLogger('d3m_dataset_selector')

config = {
    # 'dataset_dir': "/home/datashop/d3mDatasets",
    'dataset_dir': "/rdata/dataStore/d3m/datasets/seed_datasets_current",
    'dataset_json': 'datasetDoc.json',
    'out_file': 'datasetDoc.json',
    'log_level': logging.DEBUG,
    'enable_syslog': True,
    'enable_file_log': True,
    'file_log_path': '/rdata/dataStore/d3m/tmp'
}


def get_dataset_path(ds):
    """
    Generate path to dataset json based on name of dataset

    """
    return path.join(config['dataset_dir'], 
                     ds, 
                     ds + '_dataset', 
                     config['dataset_json'])

def get_default_arg_parser(desc):
    """
    Define an argument parser for use with Tigris Components and
    mandatory arguments

    """
    parser = argparse.ArgumentParser(description=desc)

    parser.add_argument('-programDir', type=str,
                       help='the component program directory')

    parser.add_argument('-workingDir', type=str,
                       help='the component instance working directory')

    parser.add_argument('-userId', type=str,
                       help='the user id')
    return parser


if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Import D3M Dataset")
    parser.add_argument('-ds_name', type=str,
                       help='the name of the dataset to import')
    args = parser.parse_args()

    # Setup Logging to file in working directory
    logger.setLevel(config['log_level'])
    
    # Write log msgs to sysLog if logging is enabled
    if config['enable_syslog']:
        ch = SysLogHandler()
        ch.setLevel(config['log_level'])
        logger.addHandler(ch)
    # Write log msgs to file if enabled
    if config['enable_file_log']:
        log_id = dt.now().isoformat()
        if config['file_log_path'] is None:
            log_file = path.join(args.workingDir, 'log-%s.txt' % log_id)
        else:
            log_file = path.join(config['file_log_path'], 'log-%s.txt' % log_id)
        ch = FileHandler(filename=log_file)
        ch.setLevel(config['log_level'])
        logger.addHandler(ch)

    # Create stream handler to output error messages to stderr
    ch = StreamHandler(sys.stderr)
    ch.setLevel(logging.ERROR)
    logger.addHandler(ch)

    ### Begin Script ###
    logger.info("Importing D3M Dataset selected by user")
    logger.debug("Running Dataset Import with arguments: %s" % str(args))

    # Get selected dataset name
    ds = args.ds_name
    logger.debug(ds) 

    # Read in the dataset json
    json_path = get_dataset_path(ds)
    logger.debug("Got file path: %s" % json_path)
    json_file = open(json_path, 'r')
    lines = json_file.readlines()

    # Write dataset info to output file
    ds_info = {'root_path': path.join(config['dataset_dir'], ds),
               'dataset_dir': ds + '_dataset',
               'dataset_json': path.join(config['dataset_dir'], ds, ds+'_dataset', config['dataset_json'])
    }
    out_file_path = path.join(args.workingDir, config['out_file'])
    logger.debug("Writing dataset json to: %s" % out_file_path)
    out_file = open(out_file_path, 'w')
    json.dump(ds_info, out_file)
    out_file.close()

