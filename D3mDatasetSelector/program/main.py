
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import argparse

__version__ = '0.1'
__dataset_file_name__ = "datasetDoc.json"

logging.basicConfig()
logger = logging.getLogger('d3m_dataset_selector')
logger.setLevel(logging.DEBUG)

config = {}
config['dataset_dir'] = "/rdata/dataStore/d3m/datasets/seed_datasets_current"
config['dataset_json'] = 'datasetDoc.json'


def get_dataset_path(ds):
    """
    Generate path to dataset json based on name of dataset

    """
    return path.join(config['dataset_dir'], 
                     ds, 
                     ds + '_dataset', 
                     config['dataset_json'])

def get_default_arg_parser(desc):
    parser = argparse.ArgumentParser(description=desc)

    parser.add_argument('-programDir', type=str,
                       help='the component program directory')

    parser.add_argument('-workingDir', type=str,
                       help='the component instance working directory')

    return parser


if __name__ == '__main__':
    logger.info("Importing D3M Dataset selected by user")

    # Parse argumennts
    parser = get_default_arg_parser("Import D3M Dataset")
    parser.add_argument('-ds_name', type=str,
                       help='the name of the dataset to import')
    args = parser.parse_args()
    logger.debug("Running Dataset Import with arguments: %s" % str(args))

    # Get selected dataset name
    ds = args.ds_name

    # Read in the dataset json
    json_path = get_dataset_path(ds)
    logger.debug("Got file path: %s" % json_path)
    json_file = open(json_path, 'r')
    lines = json_file.readlines()

    # Write dataset json to output directory
    out_file_path = path.join(args.workingDir, config['dataset_json'])
    logger.debug("Writing dataset json to: %s" % out_file_path)
    out_file = open(out_file_path, 'w')
    for line in lines:
        out_file.write(line)
    out_file.close()

