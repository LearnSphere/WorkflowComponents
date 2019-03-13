
# Author: Steven C. Dang

# Class supporting writing files out for model generation task

import logging
import os
import os.path as path
import json
from json import JSONDecodeError
from google.protobuf.json_format import MessageToJson
import csv
import pandas as pd

logger = logging.getLogger(__name__)

class RankedPipelineWriter(object):

    __out_dirs__ = ['pipelines',
                    'subpipelines',
                    'executables',
                    'supporting_files',
                    'predictions'
                    ]


    def __init__(self, out_dir, ta2=None):
        ### Check if directory structure is in place
        self.out_dir = path.join(out_dir)
        # Create basic directory structure
        for dir_name in self.__out_dirs__:
            odir = path.join(out_dir, dir_name)
            if not path.exists(odir):
                logger.info("Couldn't find output directory path. Creating nested path: %s" 
                        % odir)
                try:
                    os.makedirs(odir)
                except OSError as e:
                    logger.error("Could not create output directory for Model Generation \
                            \n%s" % str(e))
                    raise Exception("Could not find/create output directory for Model Generation")
        # Store TA2 daemon object
        self.ta2 = ta2
    
    def write_ranked_models(self, ranked_models):
        logger.info("writing %i ranked models to file" % len(ranked_models))
        for mid, rmodel in ranked_models.items():
            logger.debug("######")
            logger.debug("writing model with id: %s" % mid)
            # Translate model to json with rank field
            logger.debug("model: %s" % str(rmodel.mdl.to_dict()))
            data = rmodel.mdl.to_dict()['model']
            data['rank'] = rmodel.rank
            logger.debug("Writing model with rank: %s" % json.dumps(data))
            # Write json to file with model_id as file name
            file_name = data['id'] + '.json'
            with open(path.join(self.out_dir, 'pipelines', file_name), 'w') as out_file:
                json.dump(data, out_file)
            # Ask TA2 to export the file
            # if rmodel.
            # self.ta2.export_solution(mid, rmodel.model.fit_id, rmodel.rank)




