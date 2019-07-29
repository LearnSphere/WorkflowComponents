
# Author: Steven C. Dang

# Class representing an ordered set of operations on a given data input

import logging
import json
from json import JSONDecodeError
from abc import ABC, abstractmethod
import ast

from google.protobuf import json_format

from ta3ta2_api import core_pb2, pipeline_pb2, problem_pb2, value_pb2
# from .scores import Metric

logger = logging.getLogger(__name__)

class DBModel(ABC):

    def __init__(self, _id):
        self._id = _id
        super().__init__()

    def to_json(self):
        return self.__dict__

    def __str__(self):
        return str(self.__dict__)

    @classmethod
    def from_json(cls, data):
        logger.debug("type of data to load from json: %s" % str(type(data)))
        if isinstance(data, str):
            try: 
                d = json.loads(data)
            except JSONDecodeError:
                d = ast.literal_eval(data)
        elif isinstance(data, dict):
            d = data
        else:
            raise Exception("Invalid type given: %s" % str(type(data)))

        logger.debug("got json data for new model: %s" % str(d))
        return cls(**d)

class ModelInput(object):

    def __init__(self, name):
        self.name = name

class ModelOutput(object):

    def __init__(self, name, source):
        self.name = name
        self.source = source

class ModelNode(ABC):

    @abstractmethod
    def get_type(self):
        pass

class SimpleModelNode(ModelNode):

    def __init__(self, op, args=None, outputs=None, hyperparams=None):
        self.operator = op
        if args is not None:
            self.args = args
        else:
            self.args = []
        if outputs is not None:
            self.outputs = outputs
        else:
            self.outputs = []
        if hyperparams is not None:
            self.hyperparams = hyperparams
        else:
            self.hyperparams = []
    
    def get_type(self):
        return "SimpleNode"

class SearchModelNode(ModelNode):

    def __init__(self, inputs=None, outputs=None):
        if inputs is None:
            self.inputs = None
        else:
            self.inputs = [ModelInput(input) for input in inputs]
        if outputs is None:
            self.outputs = None
        else:
            self.outputs = [ModelOutput(out) for out in outputs]

    def get_type(self):
        return "SearchModelNode"

class Model(DBModel):

    def __init__(self, mid, name=None, desc=None, model=None, _id=None):
        self.id = mid
        self.fitted_id = None
        self.name = name
        self.desc = desc
        self.model = model
        super().__init__(_id)
    
    def add_description(self, model):
        self.model = model

    def add_description_from_protobuf(self, msg):
        desc = json_format.MessageToJson(msg)
        self.model = json.loads(desc)

    def to_protobuf(self):
        return json_format.Parse(self.model, pipeline_pb2.PipelineDescription())

    def get_default_output(self, format=None):
        """
        Just returns the first output

        """
        logger.debug("Model outputs: %s" % str(self.model['outputs']))
        if format == 'name':
            return self.model['outputs'][0]['name']
        elif format == 'data':
            return self.model['outputs'][0]['data']
        elif format == 'declare':
            return "outputs.0"
        else:
            return "outputs.0"

    @staticmethod
    def from_json(data):
        """
        Load from json string

        """
        logger.debug("type of data to load from json: %s" % str(type(data)))
        if isinstance(data, str):
            try: 
                d = json.loads(data)
            except JSONDecodeError:
                d = ast.literal_eval(data)
        elif isinstance(data, dict):
            d = data
        else:
            raise Exception("Invalid type given: %s" % str(type(data)))

        logger.debug("got json data for new model: %s" % str(d))
        
        out = Model(d['id'])
        out.name = d['name']
        out.desc = d['desc']
        out.add_description(d['model'])
        if 'fitted_id' in d.keys():
            logger.debug("Found fitted id in model json")
            out.fitted_id = d['fitted_id']
        else:
            logger.debug("Did not find fitted id in model json")
        logger.debug("Got pipeline parsed: %s" % str(out))
        return out


    def to_file(self, fpath):
        """
        Writes the workflows to a file where the first line is tab separated
        list of solution ids. The second row contains a stringified version
        of the json for the corresponding solution id

        """
        return fpath

    def to_dict(self):
        out = {
            'id': self.id,
            'name': self.name,
            'desc': self.desc,
            'model': self.model
        }
        if self.fitted_id is not None:
            out['fitted_id'] = self.fitted_id
        return out

    def to_json(self):
        return self.to_dict()
    

    def __str__(self):
        return str(self.to_dict())


class SubModelNode(Model, ModelNode):

    def get_type(self):
        return "SubModelNode"


class FittedModel(DBModel):

    def __init__(self, solution_id, fitted_id, dataset_id, _id=None):
        self.solution_id = solution_id
        self.fitted_id = fitted_id
        self.dataset_id = dataset_id
        super().__init__(_id)
    
class ModelPredictions(DBModel):

    def __init__(self, dataset_id, problem_id, fitted_model_id, data, _id=None):
        self.dataset_id = dataset_id
        self.problem_id = problem_id
        self.fitted_model_id = fitted_model_id
        self.data=data
        super().__init__(_id)

class ModelScore(DBModel):

    def __init__(self, dataset_id, metric, score, _id=None):
        self.dataset_id = dataset_id
        self.metric = metric
        self.score = score
        super().__init__(_id)
