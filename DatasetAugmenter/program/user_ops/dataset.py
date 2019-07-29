
# Author: Steven C. Dang

# Class for scanning for available d3m datasets

import logging
import os
import os.path as path
import sys
import json
import pprint
import argparse
import csv

from ls_dataset.d3m_dataset import D3MDataset

logger = logging.getLogger(__name__)

class DatasetImporter(object):
    """
    Scanning for available datasets

    """

    def __init__(self, db, session):
        self.db = db
        self.session = session


    def run(self, ds_root):
        """
        Main scripted oepration

        """
        ### Begin Script ###
        logger.info("Importing List of available datasets")

        # Read in the dataset json
        datasets = set()
        for root, dirs, files in os.walk(ds_root):
            for f in files:
                if f == 'datasetDoc.json':
                    logger.debug("Found dataset in directory: %s" % root)
                    ds_dir = path.split(root)[1]
                    # Determine if dataset is root dataset in d3m dataset structure
                    if "TEST" not in ds_dir and "TRAIN" not in ds_dir and "SCORE" not in ds_dir:
                        logger.debug("Found dataset not from TEST, TRAIN, or SCORE directories")
                        try:
                            ds = D3MDataset.from_dataset_json(path.join(root, f))
                            if not self.db.has_dataset(ds):
                                logger.info("Found dataset name: %s\nAt path: %s" % (ds.name,  ds.dpath))
                                logger.debug(str(json.loads(ds.to_json())))
                                # Add dataset to db 
                                dsid = self.db.insert_dataset_metadata(ds)
                                ds._id = str(dsid)
                                logger.debug("Inserted dataset to db with id: %s" % ds._id)
                                self.session.set_dataset_id(ds._id)
                                self.db.update_workflow_session(self.session, ['dataset_id'])
                                logger.debug("Has dataset after insert: %s" % str(self.db.has_dataset(ds)))
                                datasets.add(ds._id)
                            else:
                                logger.info("Dataset with name: %s\t was already in db. Retrieving record from db" % ds.name)
                                ds = self.db.get_dataset_with_name(ds.id)
                                logger.debug("Retrieved dataset from db with id %s: %s" % (ds._id, str(ds.to_json())))
                                datasets.add(ds._id)

                        except Exception as e:
                            # Don't choke on unsupported dataset jsons
                            logger.warning("Encountered unsupported dataset: %s" % str(e))


        logger.debug("Found datasets with ids: %s" % str(datasets))

        # Add datasets to session metadata
        self.session.set_available_dataset_ids(list(datasets))
        logger.debug("session updated with dataset list: %s" % self.session.to_json())
        self.db.update_workflow_session(self.session, 'available_datasets')
        self.session.set_state_ready()
        logger.debug("session state updated:  %s" % self.session.to_json())
        self.db.update_workflow_session(self.session, 'state')


        return datasets


class DatasetSelector(object):
    """
    Select and import a dataset from a set of datasets

    """


    def run(self, ds_root, ds_name):
        """
        Main scripted oepration

        """
        ### Begin Script ###
        logger.info("Importing D3M Dataset selected by user")

        # Read in the dataset json
        datasets = {}
        names = set()
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


        ds = datasets[ds_name]

        logger.info("Found dataset with name %s, id: %s\n json: %s" % (ds.name, ds.id, str(ds)))

        return ds

