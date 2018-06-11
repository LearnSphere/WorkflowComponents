
# Author: Steven C. Dang

# Class for managing communication between workflow components operating on a remote datset

import logging
import os.path as path
import json
from io import IOBase
import pathlib
import pprint

logger = logging.getLogger(__name__)

class LSDataset(object):
    """
    A class to encapsulate a dataset located in a particular location

    """

    __default_schema__ = "datasetDoc.json"

    def __init__(self, dpath):
        # path to the root directory of the dataset
        self.dpath = dpath
        # Name of the dataset (assumes the datset name is the same as the directory name
        self.name = path.split(dpath)[-1]
        # Path to the dataset json
        self.schema_path = path.join(self.dpath, self.name + "_dataset", self.__default_schema__)
    
    @staticmethod
    def from_json(fpath):
        """
        A static constructor of this class given a dataset json

        """
        if isinstance(fpath, str):
            if path.exists(fpath):
                with open(fpath, 'r') as f:
                    ds_json = json.load(f)
                    return LSDataset(ds_json['dataset_info']['root_path'])
            else:
                logger.error("Found no dataset json at path: %s" % str(fpath))
                raise Exception("Found no dataset json at path: %s" % str(fpath))
        elif isinstance(fpath, IOBase):
            logger.debug("Loading dataset json from open file")
            ds_json = json.load(fpath)
            return LSDataset(ds_json['dataset_info']['root_path'])
        else:
            logger.error("Found no dataset json at path: %s" % str(fpath))
            raise Exception("Found no dataset json at path: %s" % str(fpath))


    def has_dataset_schema(self):
        """
        Check if the specified dataset directory has a datset schema

        """
        return path.isfile(self.dpath)
        # logger.error("Found no dataset json at path: %s" % str(fpath))
        # raise Exception("Found no dataset json at expected path: %s" % str(fpath))

    def get_schema_uri(self):
        """
        return the path to the schema file as a file uri

        """
        return pathlib.Path(self.schema_path).as_uri()

    def get_ds_path(self):
        """
        Return the path to the root directory as a uri

        """
        return pathlib.Path(self.dpath).as_uri()
        # return path.join(self.dpath, self.name + "_dataset")

    def to_json(self, fpath=None):
        """
        Write the dataset to info to file and return a string with the json. If no path is given,
        then just returns a string with the json representation of the dataset json

        """
        out = {
            'dataset_info': {
                'root_path': self.dpath,
                'dataset_dir': self.name + '_dataset',
                'dataset_schema': self.schema_path
                }
        }
        if fpath is not None:
            logger.debug("Writing dataset json to: %s" % fpath)
            out_file = open(fpath, 'w')
            json.dump(out, out_file)
            out_file.close()

        return json.dumps(out)

    def to_json_pretty(self, fpath=None):
        """
        Write the dataset info to file in human readable format and return a 
        string with the json. If no path is given, then just returns a string 
        with the json representation of the dataset json. 

        """
        out = self.print()
        if fpath is not None:
            logger.debug("Writing pretty dataset json to: %s" % fpath)
            with open(fpath, 'w') as out_file:
                out_file.write(out)

        return out


    def __str__(self):
        return self.to_json()
    
    def print(self):
        # return self.__str__()
        out = self.__str__()
        ds_json = json.loads(out)
        return pprint.pformat(ds_json)

