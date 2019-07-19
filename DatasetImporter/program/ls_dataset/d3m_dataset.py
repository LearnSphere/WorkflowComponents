
# Author: Steven C. Dang

# Class encapsulating operations on a remote d3m dataset

import logging
import os.path as path
import os
from io import IOBase
import json
import csv
import pandas as pd

from ls_dataset.ls_dataset import LSDataset
from ls_dataset.dsr_table import DSRTable
from ls_dataset.dsr_factory import DatasetResourceFactory

logger = logging.getLogger(__name__)

class D3MDataset(LSDataset):
    """
    Class representing a remote dataset with prediction results

    """


    def __init__(self, dspath, dsdata):
        """
        inputs: 
            dspath - the path to the dataset root
            dsdata - a dictionary containing the dataset metadata
        
        """
        LSDataset.__init__(self, dspath)
        logger.debug("Initializing D3M dataset")

        # Parse dataset metadata
        self.about = dsdata['about']
        self.id = dsdata['about']['datasetID']
        self.name = dsdata['about']['datasetName']

        # Parse data resources in the dataset
        self.dataResources = [DatasetResourceFactory.get_resource(dsr) for dsr in dsdata['dataResources']]

        # Store qualities field (currently noto used)A
        if 'qualities' in dsdata:
            self.qualities = dsdata['qualities']
        else:
            self.qualities = None
   
    @staticmethod
    def from_json(d):
        """
        A static constructor of this class given a jsonified file

        """
        if isinstance(d, str):
            logger.debug("Loading json string")
            ds_json = json.loads(d)
        else:
            logger.debug("Handling input with type: %s" % type(d))
            ds_json = d
        
        # logger.debug("got dataset json: %s" % str(ds_json))
        # logger.debug("json about: %s" % ds_json['about'])
        # logger.debug("json data resources: %s" % ds_json['dataResources'])
        # json_doc = {'about': ds_json['about'],
                    # 'dataResources': ds_json['dataResources']
                    # }
        # return D3MDataset(ds_json['dataset_info']['root_path'], 
                          # json_doc)
        ds = D3MDataset(ds_json['dataset_info']['root_path'],
                          ds_json)
        logger.debug("********************************")
        logger.debug(type(ds_json))
        logger.debug(str(ds_json.keys()))
        if '_id' in ds_json.keys():
            logger.debug("dataset has a database id. manually setting it")
            ds._id = ds_json['_id']
        return ds


    @staticmethod
    def from_dataset_json(fpath):
        """
        A static constructor of this class given a dataset json

        """
        if isinstance(fpath, str):
            if path.exists(fpath):
                #Get dataset path from json path
                dpath = path.dirname(fpath)
                # dpath = path.split(path.split(fpath)[0])[0] # Assumses root
                try:
                    with open(fpath, 'r', encoding="utf-8") as f:
                        logger.info("Loading json")
                        ds_json = json.load(f)
                        logger.info("Constructing D3MDataset")
                        return D3MDataset(dpath,
                                          ds_json)
                except:
                    logger.error("Error while decoding dataset json: %s" % fpath)

            else:
                logger.error("Found no dataset json at path: %s" % str(fpath))
                raise Exception("Found no dataset json at path: %s" % str(fpath))
        elif isinstance(fpath, IOBase):
            logger.debug("Loading dataset json from open file")
            logger.debug("dataset path: %s" % str(fpath))
            dpath = path.dirname(fpath)
            # dpath = path.split(path.split(fpath)[0])[0]
            # ds_json = json.load(fpath)
            ds_json = json.load(fpath, encoding='utf-16')
            return D3MDataset(dpath,
                                ds_json)
        else:
            logger.error("Found no dataset json at path: %s" % str(fpath))
            raise Exception("Found no dataset json at path: %s" % str(fpath))

    @staticmethod
    def get_schema_path(dpath):
        name = path.split(dpath)[-1]
        fpath = path.join(dpath, name + '_dataset', LSDataset.__default_schema__)
        if path.exists(fpath):
            return fpath
        else:
            raise Exception("No schema doc found in dataset directory: %s" % dpath)

    def to_component_out_file(self, fpath):
        """
        Write the dataset to file for passing between components. 
        Writes the first row of a tab separated file as the list of column names.
        The first cell of the second row is simply the 

        """
        for resource in self.dataResources:
            if resource.resType == 'table':
                logger.debug("Resource type: %s\t %s" % (str(type(resource.columns)), str(resource.columns)))
                for col in resource.columns:
                    logger.debug("Type: %s\t col: %s" % (str(type(col)), str(col)))
                names = [col.colName for col in resource.columns]
        js = self.to_json()

        with open(fpath, 'w') as out_file:
            logger.debug("Writing dataset json to component out file: %s" % fpath)
            writer = csv.writer(out_file, delimiter='\t')
            writer.writerow(names)
            writer.writerow([js])

    @staticmethod
    def from_component_out_file(fpath):
        """
        Load the dataset from an out file written to pass between workflow components

        """
        if isinstance(fpath, str):
            in_file = open(fpath, 'r')
            reader = csv.reader(in_file, delimiter='\t')
            rows = [row for row in reader]
            in_file.close()
        elif isinstance(fpath, IOBase):
            reader = csv.reader(fpath, delimiter='\t')
            rows = [row for row in reader]
            fpath.close()
        col_names = rows[0]
        logger.debug("Got columns names: %s" % str(col_names))
        # logger.debug("Got dataset row with type %s:\t %s" % (str(type(rows[1][0])), str(rows[1][0])))
        # logger.debug(len(rows[1]))
        # logger.debug(rows[1][0])
        # logger.debug(type(rows[1][0]))
        return D3MDataset.from_json(rows[1][0])


    def to_json(self, fpath=None):
        """
        Write the dataset to info to file and return a string with the json. If no path is given,
        then just returns a string with the json representation of the dataset json

        """
        # logger.debug("D3MDataset to json")

        out = json.loads(super().to_json())
        out['_id'] = str(self._id)
        out['about'] = self.about
        out['dataResources'] = [json.loads(rc.to_json()) for rc in self.dataResources]
        out['qualities'] = self.qualities
        
        if fpath is not None:
            logger.debug("Writing dataset json to: %s" % fpath)
            out_file = open(fpath, 'w')
            json.dump(out, out_file)
            out_file.close()

        return json.dumps(out)


    def __str__(self):
        return self.to_json()

    def load_dataset(self):
        """
        Load the dataset table

        """
        data = None
        for dr in [dr for dr in self.dataResources if type(dr) is DSRTable]:
            logger.debug("Found data resource table with ID: %s\tpath: %s" % (dr.resID, dr.resPath))
            if data is None:
                dpath = path.join(self.dpath, dr.resPath)
                data = pd.read_csv(dpath, ',')
                return data

    def get_resource(self, rid):
        """
        Get a data resource by its index

        """
        for dr in self.dataResources:
            if dr.resID == rid:
                return dr

        logger.warning("No data resource found with id matching: %s" % rid)

    def get_data_columns(self):
        for dr in [dr for dr in self.dataResources if type(dr) is DSRTable]:
            logger.debug("Found data resource table with ID: %s\tpath: %s" % (dr.resID, dr.resPath))
            return [col for col in dr.columns if col.colName != 'd3mIndex']

    def get_training_path(self):
        logger.debug("Checking for TRAIN directory for dataset at path: %s", self.dpath)
        parent = path.dirname(self.dpath)
        train_dir = path.join(parent, "TRAIN", "dataset_TRAIN")
        train_ds_file = path.join(train_dir, "datasetDoc.json")
        if path.isfile(train_ds_file):
            logger.debug("Found training dataset schema at: %s" % train_ds_file)
            return train_dir
        else:
            logger.debug("Not training dataset schema found at: %s" % train_dir)
            return None

    def get_training_dataset(self):
        train_path = self.get_training_path()
        if train_path is not None:
            ds_json = json.loads(self.to_json())
            ds_json['dataset_info']['root_path'] = train_path
            ds_json['dataset_info']['dataset_schema'] = path.join(train_path, self.__default_schema__)
            logger.debug("dataset json before reinitializing dataset: %s" % str(ds_json))
            train_ds = D3MDataset.from_json(ds_json)
            logger.debug("Created training dataset: %s" % train_ds.to_json())
        else:
            train_ds = ds
        return train_ds


    def get_test_path(self):
        logger.debug("Checking for TEST directory for dataset at path: %s", self.dpath)
        parent = path.dirname(self.dpath)
        test_dir = path.join(parent, "TEST", "dataset_TEST")
        test_ds_file = path.join(test_dir, "datasetDoc.json")
        if path.isfile(test_ds_file):
            logger.debug("Found test dataset schema at: %s" % test_ds_file)
            return test_dir
        else:
            logger.debug("Not test dataset schema found at: %s" % test_dir)
            return None

    def get_test_dataset(self):
        test_path = self.get_test_path()
        if test_path is not None:
            ds_json = json.loads(self.to_json())
            ds_json['dataset_info']['root_path'] = test_path
            ds_json['dataset_info']['dataset_schema'] = path.join(test_path, self.__default_schema__)
            logger.debug("dataset json before reinitializing dataset: %s" % str(ds_json))
            test_ds = D3MDataset.from_json(ds_json)
            logger.debug("Created training dataset: %s" % test_ds.to_json())
        else:
            test_ds = ds
        return test_ds


    def get_score_path(self):
        logger.debug("Checking for SCORE directory for dataset at path: %s", self.dpath)
        parent = path.dirname(self.dpath)
        score_dir = path.join(parent, "SCORE", "dataset_SCORE")
        score_ds_file = path.join(score_dir, "datasetDoc.json")
        if path.isfile(score_ds_file):
            logger.debug("Found score dataset schema at: %s" % score_ds_file)
            return score_dir
        else:
            logger.debug("Not score dataset schema found at: %s" % score_dir)
            return None

    def get_score_dataset(self):
        score_path = self.get_score_path()
        if score_path is not None:
            ds_json = json.loads(self.to_json())
            ds_json['dataset_info']['root_path'] = score_path
            ds_json['dataset_info']['dataset_schema'] = path.join(score_path, self.__default_schema__)
            logger.debug("dataset json before reinitializing dataset: %s" % str(ds_json))
            score_ds = D3MDataset.from_json(ds_json)
            logger.debug("Created training dataset: %s" % score_ds.to_json())
        else:
            score_ds = ds
        return score_ds





