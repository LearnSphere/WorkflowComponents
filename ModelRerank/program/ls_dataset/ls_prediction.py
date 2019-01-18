
# Author: Steven C. Dang

# Represents a remote dataset with addional model prediction results

import logging
import os.path as path
import os
from io import IOBase
import re
import json
import pathlib

from ls_dataset.ls_dataset import LSDataset
from ls_dataset.d3m_dataset import D3MDataset
from ls_problem_desc.ls_problem import ProblemDesc

logger = logging.getLogger(__name__)

class LSPrediction(LSDataset):
    """
    Class representing a remote dataset with prediction results

    """


    def __init__(self, dpath, ppath, prob_desc=None, pfiles=None):
        """
        inputs: 
            dpath - the path to the dataset root
            ppath - the path the the prediction results file(s) directory root
            prob_desc - the path to the problem description schema file that describes the prediction
        
        """
        LSDataset.__init__(self, dpath)
        logger.debug("Initializing prediction dataset")
        self.ppath = ppath
        self.prob_desc = prob_desc
        if pfiles is not None:
            self.pfiles = pfiles
        else:
            # Grab the list of prediction files in the ppath directory
            self.pfiles = []
            pat = re.compile('pipeline_[0-9]*.csv', re.IGNORECASE)
            for fpath, dirs, files in os.walk(ppath):
                for f in files:
                    if pat.match(f) is not None:
                        logger.debug("found prediction file: %s" % f)
                        self.pfiles.append(path.join(fpath, f))

    @staticmethod
    def from_json(fpath):
        """
        A static constructor of this class given a dataset json

        """
        if isinstance(fpath, str):
            if path.exists:
                with open(fpath, 'r') as f:
                    ds_json = json.load(f)
            else:
                logger.error("Found no dataset json at path: %s" % str(fpath))
                raise Exception("Found no dataset json at path: %s" % str(fpath))
        elif isinstance(fpath, IOBase):
            logger.debug("Loading dataset json from open file")
            ds_json = json.load(fpath)
        else:
            logger.error("Found no dataset json at path: %s" % str(fpath))
            raise Exception("Found no dataset json at path: %s" % str(fpath))

        dpath = ds_json['dataset_info']['root_path']
        dname = ds_json['dataset_info']['dataset_dir'].rsplit("_", 1)[0]

        if isinstance(fpath, str):
            logger.debug("Creating D3mDataset with fpath: %s\nMetadata: %s" % (str(fpath), str(ds_json['about'])))
            # ds = D3MDataset(fpath, ds_json['about'])
            ds = D3MDataset(dpath, ds_json)
        elif isinstance(fpath, IOBase):
            logger.debug("Creating D3mDataset with fpath: %s\nMetadata: %s" % (fpath.name, str(ds_json['about'])))
            # ds = D3MDataset(fpath.name, ds_json['about'])
            ds = D3MDataset(dpath, ds_json)
        logger.debug("Creating problem description")
        logger.debug("Got default problem: %s" % str(ProblemDesc.get_default_problem(ds)))
        prob_desc = ProblemDesc.from_json(ProblemDesc.get_default_problem(ds))
        # prob_desc = ProblemDesc.from_json(
        # prob_desc = ProblemDesc.from_json(
            # ProblemDesc.get_default_problem(D3MDataset(fpath, ds_json['about']))
        # )
            # LSPrediction.get_default_problem(ds_json['dataset_info']['root_path']))
        
        return LSPrediction(ds_json['dataset_info']['root_path'], 
                            ds_json['pred_info']['pred_root'],
                            prob_desc=prob_desc,
                            pfiles=ds_json['pred_info']['pred_files'])

    def to_json(self, fpath=None):
        """
        Write the dataset to info to file and return a string with the json. If no path is given,
        then just returns a string with the json representation of the dataset json

        """

        out = json.loads(super().to_json())
        out['pred_info'] = {
            'pred_root': self.ppath,
            'pred_files': self.pfiles
        }
        if self.prob_desc is not None:
            out['prob_desc'] = json.loads(self.prob_desc.to_json())

        if fpath is not None:
            logger.debug("Writing dataset json to: %s" % fpath)
            out_file = open(fpath, 'w')
            json.dump(out, out_file)
            out_file.close()

        return json.dumps(out)

