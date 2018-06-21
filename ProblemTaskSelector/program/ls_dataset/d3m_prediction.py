
# Author: Steven C. Dang

# Class encapsulating a remote dataset with addional model prediction results

import logging
import os.path as path
import os
from io import IOBase
import json

from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.ls_prediction import LSPrediction
from ls_dataset.dsr_factory import DatasetResourceFactory
from ls_problem_desc.ls_problem import ProblemDesc

logger = logging.getLogger(__name__)

class D3MPrediction(D3MDataset, LSPrediction):
    """
    Class representing a remote dataset with prediction results

    """

    def __init__(self, dspath, dsdata, ppath, prob_desc=None, pfiles=None):
        """
        inputs: 
            dspath - the path to the dataset root
            dsdata - a dictionary containing the dataset metadata
            ppath - the path the the prediction results file(s) directory root
            prob_desc - the path to the problem description schema file that describes the prediction
        
        """
        D3MDataset.__init__(self, dspath, dsdata)
        LSPrediction.__init__(self, dspath, ppath, prob_desc, pfiles)

        logger.debug("Initializing D3M prediction dataset")

   
    @staticmethod
    def from_json(fpath):
        """
        A static constructor of this class given a jsonified file

        """
        if isinstance(fpath, str):
            if path.exists(fpath):
                #Get dataset path from json path
                with open(fpath, 'r') as f:
                    ds_json = json.load(f)
            else:
                # logger.error("Found no dataset json at path: %s" % str(fpath))
                raise Exception("Found no dataset json at path: %s" % str(fpath))
        elif isinstance(fpath, IOBase):
            logger.debug("Loading dataset json from open file")
            ds_json = json.load(fpath)
        else:
            # logger.error("Found no dataset json at path: %s" % str(fpath))
            raise Exception("Found no dataset json at path: %s" % str(fpath))
        
        logger.debug("got dataset json: %s" % str(ds_json))
        json_doc = {'about': ds_json['about'],
                    'dataResources': ds_json['dataResources']
                    }
        dpath = ds_json['dataset_info']['root_path']
        dname = ds_json['dataset_info']['dataset_dir'].rsplit("_", 1)[0]
        if isinstance(fpath, str):
            logger.debug("Creating D3mDataset with fpath: %s\nMetadata: %s" % (dpath, str(ds_json)))
            # ds = D3MDataset(fpath, ds_json['about'])
            ds = D3MDataset(dpath, ds_json)
        elif isinstance(fpath, IOBase):
            logger.debug("Creating D3mDataset with fpath: %s\nMetadata: %s" % (dpath, str(ds_json)))
            # ds = D3MDataset(fpath.name, ds_json['about'])
            ds = D3MDataset(dpath, ds_json)
        logger.debug("Creating problem description")
        logger.debug("Got default problem: %s" % str(ProblemDesc.get_default_problem(ds)))
        prob_desc = ProblemDesc.from_json(ProblemDesc.get_default_problem(ds))
        # prob_desc = ProblemDesc.from_json(
            # LSPrediction.get_default_problem(ds_json['dataset_info']['root_path']))
        return D3MPrediction(dpath,
                             json_doc,
                             path.join(dpath, ds_json['dataset_info']['dataset_dir'], 'output'),
                             prob_desc=prob_desc,
                             pfiles=ds_json['pred_info']['pred_files']
                             )


    def to_json(self, fpath=None):
        """
        Write the dataset to info to file and return a string with the json. If no path is given,
        then just returns a string with the json representation of the dataset json

        """

        out = json.loads(super().to_json())
        pred_out = json.loads(super().to_json())
        out['pred_info'] = pred_out['pred_info']

        if fpath is not None:
            logger.debug("Writing dataset json to: %s" % fpath)
            out_file = open(fpath, 'w')
            json.dump(out, out_file)
            out_file.close()

        return json.dumps(out)

    def __str__(self):
        return self.to_json()

