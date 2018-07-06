
# Author: Steven C. Dang

# Class representing an ordered set of operations on a given data input

import logging
import json
from json import JSONDecodeError
from abc import ABC, abstractmethod
import ast

from google.protobuf import json_format
from protobuf_to_dict import protobuf_to_dict

from d3m_ta2.api_v3 import core_pb2, pipeline_pb2, problem_pb2, value_pb2
from .scores import Metric

logger = logging.getLogger(__name__)

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

class Model(object):

    def __init__(self, mid, name=None, desc=None, model=None):
        self.id = mid
        self.name = name
        self.desc = desc
        self.model = model
    
    def add_description(self, model):
        self.model = model

    def add_description_from_protobuf(self, msg):
        desc = json_format.MessageToJson(msg)
        self.model = json.loads(desc)

    def to_protobuf(self):
        return json_format.Parse(self.model, pipeline_pb2.PipelineDescription())

    def get_default_output(self):
        """
        Just returns the first output

        """
        logger.debug("Model outputs: %s" % str(self.model['outputs']))
        return self.model['outputs'][0]['name']

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
        out.desc = d['description']
        out.add_description(d['model'])
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
        return {
            'id': self.id,
            'name': self.name,
            'description': self.desc,
            'model': self.model
        }
    
    # def from_dict(self, data):
        

    def __str__(self):
        return str(self.to_dict())


class SubModelNode(Model, ModelNode):

    def get_type(self):
        return "SubModelNode"

