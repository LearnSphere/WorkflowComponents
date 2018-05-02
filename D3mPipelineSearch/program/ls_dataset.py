
# Author: Steven C. Dang

# Class for managing communication between workflow components operating on a remote datset

import logging
import os.path as path
import json
import pathlib

logger = logging.getLogger('ls_dataset')


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
        if isinstance(fpath, basestring):
            if path.exists:
                with open(fpath, 'r') as f:
                    ds_json = json.load(f)
                    return LSDataset(ds_json['root_path'])
            else:
                logger.error("Found no dataset json at path: %s" % str(fpath))
                raise Exception("Found no dataset json at path: %s" % str(fpath))
        elif isinstance(fpath, file):
            logger.debug("Loading dataset json from open file")
            ds_json = json.load(fpath)
            return LSDataset(ds_json['root_path'])
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


    def to_json(self, fpath=None):
        """
        Write the dataset to info to file and return a string with the json. If no path is given,
        then just returns a string with the json representation of the dataset json

        """

        out = {'root_path': self.dpath,
               'dataset_dir': self.name + '_dataset',
               'dataset_schema': self.schema_path
        }
        if fpath is not None:
            logger.debug("Writing dataset json to: %s" % fpath)
            out_file = open(fpath, 'w')
            json.dump(out, out_file)
            out_file.close()

        return json.dumps(out)

    def __str__(self):
        return self.to_json()
    
    def get_schema_uri(self):
        """
        return the path to the schema file as a file uri

        """
        return pathlib.Path(self.schema_path).as_uri()
