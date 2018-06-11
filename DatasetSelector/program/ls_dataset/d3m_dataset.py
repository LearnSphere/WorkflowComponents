
# Author: Steven C. Dang

# Class encapsulating operations on a remote d3m dataset

import logging
import os.path as path
import os
from io import IOBase
import json
import csv

from ls_dataset.ls_dataset import LSDataset
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
   
    @staticmethod
    def from_json(d):
        """
        A static constructor of this class given a jsonified file

        """
        if isinstance(d, str):
            ds_json = json.loads(d)
        else:
            ds_json = d
        
        logger.debug("got dataset json: %s" % str(ds_json))
        json_doc = {'about': ds_json['about'],
                    'dataResources': ds_json['dataResources']
                    }
        return D3MDataset(ds_json['dataset_info']['root_path'], 
                          json_doc)


    @staticmethod
    def from_dataset_json(fpath):
        """
        A static constructor of this class given a dataset json

        """
        if isinstance(fpath, str):
            if path.exists(fpath):
                #Get dataset path from json path
                dpath = path.split(path.split(fpath)[0])[0] # Assumses root
                with open(fpath, 'r') as f:
                    ds_json = json.load(f)
                    return D3MDataset(dpath,
                                      ds_json)
            else:
                logger.error("Found no dataset json at path: %s" % str(fpath))
                raise Exception("Found no dataset json at path: %s" % str(fpath))
        elif isinstance(fpath, IOBase):
            logger.debug("Loading dataset json from open file")
            logger.debug("dataset path: %s" % str(fpath))
            dpath = path.split(path.split(fpath)[0])[0]
            ds_json = json.load(fpath)
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
        logger.debug("got rows %i from component file" % len(rows))
        col_names = rows[0]
        logger.debug("Got columns names: %s" % str(col_names))
        logger.debug("Got dataset row with type %s:\t %s" % (str(type(rows[1][0])), str(rows[1])))
        return D3MDataset.from_json(rows[1][0])


    def to_json(self, fpath=None):
        """
        Write the dataset to info to file and return a string with the json. If no path is given,
        then just returns a string with the json representation of the dataset json

        """

        out = json.loads(super().to_json())
        out['about'] = self.about
        out['dataResources'] = [json.loads(rc.to_json()) for rc in self.dataResources]
        
        if fpath is not None:
            logger.debug("Writing dataset json to: %s" % fpath)
            out_file = open(fpath, 'w')
            json.dump(out, out_file)
            out_file.close()

        return json.dumps(out)


    def __str__(self):
        return self.to_json()
