
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import os
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
    parser = get_default_arg_parser("Import List of available D3M Datasets")
    args = parser.parse_args()

    # Get config file
    config = stg(path.join(args.programDir, 'program', 'settings.cfg'))

    # Setup Logging
    setup_logging(config.parse_logging(), args.workingDir, args.is_test == 1)
    logger = logging.getLogger('datasets_importer')

    ### Begin Script ###
    logger.info("Importing List of available datasets")
    logger.debug("Running Dataset Importer with arguments: %s" % str(args))

    # Read in the dataset json
    ds_root = config.get('Dataset', 'dataset_dir')
    datasets = set()
    for root, dirs, files in os.walk(ds_root):
        for f in files:
            if f == 'datasetDoc.json':
                logger.debug("Found dataset in directory: %s" % root)
                ds = D3MDataset.from_dataset_json(path.join(root, f))
                if ds.name not in datasets:
                    logger.info("Found dataset name: %s\nAt path: %s" % (ds.name,  ds.dpath))
                    datasets.add(ds.name)

    logger.debug("Found datasets: %s" % str(datasets))


    # # Write dataset info to output file
    out_file_path = path.join(args.workingDir, config.get('Dataset', 'out_file'))
    logger.info("Writing dataset list of %i datasets to file: %s" % (len(datasets), out_file_path))
    with open(out_file_path, 'w') as out_file:
        out_csv = csv.writer(out_file, delimiter='\t')
        out_csv.writerow(datasets)
