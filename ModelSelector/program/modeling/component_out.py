
# Author: Steven C. Dang

# Classes for handling component outputs of modeling outputs

import logging
import json
import csv
from json import JSONDecodeError
from .models import *
from .scores import *

logger = logging.getLogger(__name__)


class ModelSetIO(object):

    @staticmethod
    def to_file(fpath, models):
        """
        Write models to tab-delimited csv. 
        Imputs:
            fpath - the path to the file to write out to
            models - a dictionary where the key is the model_id and the value is
                the model object

        """
    
        logger.info("Writing Model Set to file: %s" % fpath)
        with open(fpath, 'w') as out_file:
            out = csv.writer(out_file, delimiter='\t')
            out.writerow([i for i, mid in enumerate(models)])
            out.writerow([models[mid].id for mid in models])
            out.writerow([models[mid].to_dict() for mid in models])

    @staticmethod
    def from_file(fpath):
        """
        Read models from tab-delmisted file
        Inputs:
            fpath - the path to the file to import
        Outputs:
            model_index - List of the model_ids in the document to recover order.
            models - dictionary with the model id as the key, and model object
                as the value

        """
        logger.info("Reading Model file: %s" % fpath)
        # Read in the the models from tsv
        reader = csv.reader(fpath, delimiter='\t')
        rows = [row for row in reader]

        # Initialize the set of models by model id
        models = {mid: None for mid in rows[1]}
        model_index = rows[1]
        for i, mid in enumerate(model_index):
            models[mid] = Model.from_json(rows[2][i])
        return model_index, models


class FittedModelSetIO(object):

    @staticmethod
    def to_file(fpath, fitted_models, models, model_index=None):
        """
        Write models to tab-delimited csv. 
        Imputs:
            fpath - the path to the file to write out to
            models - a dictionary where the key is the model_id and the value is
                the model object

        """
    
        logger.info("Writing Fitted Model Set to file: %s" % fpath)
        rows = []
        if model_index is not None:
            if len(model_index) != len(fitted_models):
                logger.warning("Invalid model index given. index has %i entries, \
                        but %i models were given" % (len(model_index), len(fitted_models)))
                rows.append(range(len(fitted_models)))
                model_index = [mid for mid in models]
            else:
                rows.append(range(len(model_index)))

        rows.append(model_index)
        rows.append([fitted_models[mid] for mid in model_index])
        rows.append([models[mid].to_dict() for mid in model_index]) # Model json

        with open(fpath, 'w') as out_file:
            out = csv.writer(out_file, delimiter='\t')
            for row in rows:
                out.writerow(row)


    @staticmethod
    def from_file(fpath):
        """
        Read models from tab-delmisted file
        Inputs:
            fpath - the path to the file to import
        Outputs:
            model_index - List of the model_ids in the document to recover order.
            fitted_models - 
            models - dictionary with the model id as the key, and model object
                as the value

        """
        logger.info("Reading Fitted Models file: %s" % fpath)
        # Read in the the models from tsv
        reader = csv.reader(fpath, delimiter='\t')
        rows = [row for row in reader]

        # Initialize the set of models by model id
        model_index = rows[1]
        models = {mid: None for mid in model_index}
        fitted_models = {mid: None for mid in model_index}
        for i, mid in enumerate(model_index):
            fitted_models[mid] = rows[2][i]
            models[mid] = Model.from_json(rows[3][i])

        return model_index, fitted_models, models

class ModelScoreSetIO(object):

    @staticmethod
    def to_file(fpath, scores, models, model_index=None):
        """
        Write model scores to file

        """
        logger.info("Writing Model Scores to file: %s" % fpath)

        rows = []
        if model_index is not None:
            if len(model_index) != len(scores):
                logger.warning("Invalid model index given. index has %i entries, \
                        but %i models were given" % (len(model_index), len(fitted_models)))
                rows.append(range(len(models)))
                model_index = [mid for mid in models]
            else:
                rows.append(range(len(model_index)))

        rows.append(model_index)
        rows.append([scores[mid].to_dict() for mid in model_index])
        rows.append([models[mid] for mid in model_index])

        with open(fpath, 'w') as out_file:
            out = csv.writer(out_file, delimiter='\t')
            for row in rows:
                out.writerow(row)
    
    @staticmethod
    def from_file(fpath):
        """
        Read Model scores from file

        """
        logger.info("Reading Model Scores from file: %s" % fpath)

        # Read in the scores from tsv
        reader = csv.reader(fpath, delimiter='\t')
        rows = [row for row in reader]

        # Initialize the set of models by model id
        model_index = rows[1]
        models = {mid: None for mid in model_index}
        scores = {mid: None for mid in model_index}
        for i, mid in enumerate(model_index):
            scores[mid] = ModelScores.from_json(rows[2][i])
            models[mid] = Model.from_json(rows[3][i])

        return model_index, scores, models


