
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
            logger.debug(data)
            d = data
        else:
            raise Exception("Invalid type given: %s" % str(type(data)))

        logger.debug("got json data for new model: %s" % str(d))
        
        mdl = Model(d['id'])
        mdl.name = d['name']
        mdl.desc = d['description']
        mdl.add_description(d['model'])
	
        logger.debug("Got pipeline parsed: %s" % str(mdl))
        return mdl


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

class ModelScores(object):

    def __init__(self, mid, inputs, scores):
        logger.debug("ModelScore initialized with id: %s\ninputs: %s\nscores: %s" % (mid, str(inputs), str(scores)))
        # A ModeL id this score applies to
        self.mid = mid
        # Inputs used to generate the score
        self.inputs = inputs
        # list of scores
        self.scores = scores

    def to_dict(self):
        out = {'model_id': self.mid,
               'inputs': self.inputs,
               # 'scores': [json_format.MessageToJson(score) for score in self.scores]
               # 'scores': [protobuf_to_dict(score) for score in self.scores]
               'scores': [score.to_dict() for score in self.scores]
        }
        return out

    def __str__(self):
        out = self.to_dict()
        return json.dumps(out)
    
    @staticmethod
    def from_json(raw_data):
        logger.debug("########################")
        logger.debug("Got raw data:\n%s" % raw_data)
        data = json.loads(raw_data)
        logger.debug("########################")
        logger.debug("data keys: %s" % data.keys())
        logger.debug("########################")
        logger.debug("Model id: %s" % data['model_id'])
        logger.debug("########################")
        logger.debug("Model inputs: %s" % data['inputs'])
        logger.debug("########################")
        logger.debug("Model scores: %s" % [str(score) for score in data['scores']])
        logger.debug("Model scores: %s" % [Score.from_json(score) for score in data['scores']])
        return ModelScores(data['model_id'], data['inputs'],[Score.from_json(score) for score in data['scores']])

class Score(object):

    def __init__(self, metric, fold, targets, value):
        self.metric = metric
        self.fold = fold
        if targets is None:
            self.targets = []
        else:
            self.targets = targets
        self.value = value

    @staticmethod
    def from_json(data):
        if isinstance(data, str):
            data = json.loads(data)
        metric = Metric.from_json(data['metric'])        
        val = Value.from_json(data['value'])
        if 'targets' in data:
            targets = data['targets']
        else:
            targets = []
        return Score(metric, data['fold'], targets, val)

    @staticmethod
    def from_protobuf(msg):
        metric = Metric.from_protobuf(msg.metric)
        targets = [json_format.MessageToJson(target) for target in msg.targets]
        val = Value.from_protobuf(msg.value)
        return Score(metric, msg.fold, targets, val)

    def to_protobuf(self):
        msg = core_pb2.Score(
            fold=self.fold,
            metric=self.metric.to_protobuf(),
            value=self.value.to_protobuf()
        )
        # msg.metric = self.metric.to_protobuf()
        if len(self.targets) > 0:
            msg.targets = targets
        # msg.value = self.value.to_protobuf()
        return msg

    def to_dict(self):
        return {
            'metric': self.metric.to_dict(),
            'fold': self.fold,
            'targets': self.targets,
            'value': self.value.to_dict(),
        }

    def __str__(self):
        return str(self.to_dict())

class Value(object):

    def __init__(self, val, vtype):
        self.value = val
        self.type = vtype

    @staticmethod
    def from_json(msg):
        logger.debug("Creating Value from %s: %s" % (str(type(msg)), str(msg)))
        mtype = list(msg.keys())[0]
        val = msg[mtype]
        return Value(val, mtype)

    @staticmethod
    def from_protobuf(msg):
        d = json.loads(json_format.MessageToJson(msg))
        logger.debug("Got msg json: %s" % str(d))
        return Value.from_json(d)

    def to_protobuf(self):
        msg = value_pb2.Value()
        setattr(msg, self.type, self.value)
        return msg

    def to_dict(self):
        return {
            self.type: self.value
        }

    def __str__(self):
        return str(self.to_dict())

        

class Fit(object):

    def __init__(self, mdl, dataset, fit):
        self.mdl = mdl
        self.dataset = dataset
        self.fit = fit

