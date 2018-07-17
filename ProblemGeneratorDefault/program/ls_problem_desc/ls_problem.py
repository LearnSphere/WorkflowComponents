
# Author: Steven C. Dang

# Class representing a D3m problem description for pipeline search

import logging
import os.path as path
import os
from io import IOBase
import json
from json import JSONDecodeError
import csv
from datetime import datetime
import pprint
import ast

from ls_dataset.d3m_dataset import D3MDataset
from modeling.scores import Metric

logger = logging.getLogger(__name__)



class ProblemDesc(object):
    """
    Class representing a D3m problem description for pipeline search

    """

    __task_types__ =  [
        'CLASSIFICATION',
        'REGRESSION',
        'CLUSTERING',
        'LINK_PREDICTION',
        'VERTEX_NOMINATION',
        'COMMUNITY_DETECTION',
        'GRAPH_CLUSTERING',
        'GRAPH_MATCHING',
        'TIME_SERIES_FORECASTING',
        'COLLABORATIVE_FILTERING ',
        'OBJECT_DETECTION '
    ]
    __task_subtypes__ =  [
        'NONE',
        'BINARY',
        'MULTICLASS',
        'MULTILABEL',
        'UNIVARIATE',
        'MULTIVARIATE',
        'OVERLAPPING',
        'NONOVERLAPPING'
    ]

    __ignore_chars__=['-','_']
    
    def __init__(self, name=None, desc=None, task_type=None, subtype=None, version=1.0, metrics=None, metadata=None):
        """
        inputs: 
            metadata - dictionary representation of additional information
        
        """
        self.id = str(abs(hash(datetime.now())))
        self.name=name
        self.version=version
        self.description=desc
        self.task_type=task_type
        self.subtype = subtype
        if metrics is None:
            self.metrics=[]
        elif hasattr(metrics, '__iter__'):
            if isinstance(metrics[0], str):
                self.metrics = [Metric(m) for m in metrics]
            elif isinstance(metrics[0], dict):
                self.metrics = [Metric.from_json(m) for m in metrics]
        else:
            raise Exception("Invalid metrics given, must be a list")
        self.inputs = []
       
        # Catchall for extra information to support easy subclassing
        if metadata is not None:
            self.metadata = metadata
        else:
            self.metadata = None

    def add_input(self, did, res, col):
        if len(self.inputs) == 0:
            inpt = Input(did)
            inpt.add_target(res, col)
            self.inputs.append(inpt)
        else:
            added = False
            for inpt in self.inputs:
                if inpt.id == did:
                    inpt.add_target(res, col)
                    added = True

            if not added:
                inpt = Input(did)
                inpt.add_target(res,col)
                self.inputs.append(inpt)

    def add_task_type(self, task_type):
        # Transform input to lowercase and removed ignore chars
        t = task_type.lower()
        for char in self.__ignore_chars__:
            t = t.replace(char, "")
        # Get index of matching type
        logger.debug("Looking for match of tyoe: %s" % t)
        i = self.get_task_types().index(t)
        logger.debug("got matching type: %s" % self.__task_types__[i])
        self.task_type = self.__task_types__[i]

    def add_metric(self, metric):
        """
        Add a metric given the name of the metric

        """
        if isinstance(metric, str):
            m = Metric(metric)
            self.metrics.append(m)
        elif isinstance(metric, dict):
            self.metrics.append(Metric.from_json(metric))
        else:
            raise Exception("Error adding metric: %s" % str(metric))

    @staticmethod
    def get_task_types():
        tasks = [t.lower() for t in ProblemDesc.__task_types__]
        for i,t in enumerate(tasks):
            for char in ProblemDesc.__ignore_chars__:
                tasks[i] = t.replace(char, "")
        logger.debug("Got problem task types: %s" % str(tasks))
        return tasks    

    def get_valid_tasks(self):
        if len(self.inputs) > 0:
            tasks = set()
            hasTargets = False
            for inpt in self.inputs:
                if len(inpt.targets) > 0:
                    tasks |= set(ProblemDesc.__task_types__)
                    hasTargets = True
            if not hasTargets:
                raise Exception("No targets specified for any input, so cannot retrieve valid tasks")
            return tasks
        else:
            raise Exception("No Datasets specificied, can not get valid tasks")
        

    def to_dict(self):
        # logger.debug("######################################")
        # logger.debug("ProblmDesc to dict")
        # logger.debug("######################################")
        out = {
            "id": self.id,
            "version": self.version,
            "metrics": [metric.to_dict() for metric in self.metrics],
            "inputs": [inpt.to_dict() for inpt in self.inputs]
        }
        if self.name is not None:
            out["name"] = self.name
        if self.description is not None:
            out["description"] = self.description
        if self.task_type is not None:
            out["task_type"] = self.task_type
        if self.subtype is not None:
            out["task_subtype"] = self.subtype
        if self.metadata is not None:
            out["metadata"] = self.metadata
        return out

    def to_file(self, fpath):
        if isinstance(fpath, str):
            with open(fpath, 'w') as out_file:
                json.dump(self.to_dict(), out_file)
        elif isinstance(fpath, IOBase):
            json.dump(self.to_dict(), fpath)
        else:
            raise Exception("Invalid file/path given to write to file. Given \
                            input type: %s" % type(fpath))

    @staticmethod
    def from_file(fpath):
        logger.info("Initializing ProblemDesc from json file")
        if isinstance(fpath, str):
            with open(fpath, 'r') as f:
                data = json.load(f)
        elif isinstance(fpath, IOBase):
            data = json.load(fpath)
        else:
            raise Exception("Expected path string or file io object to initialize \
                            Problem Description. Got %s instead" % type(fpath))
        # logger.debug("Read in Problem Doc from file: %s" % str(data))
        return ProblemDesc.from_json(data)

    
    @staticmethod
    def from_json(inpt):
        """
        A static constructor of this class given a jsonified file

        """
        if isinstance(inpt, str):
            logger.debug(inpt)  
            try: 
                data = json.loads(inpt)
            except JSONDecodeError:
                data = ast.literal_eval(inpt)
        elif isinstance(inpt, dict):
            data = inpt
        else:
            raise Exception("Could not create problem given input type %s\nraw input: %s" % (type(inpt), str(inpt)))
        logger.debug("Got json to import: %s" % str(data))

        name = data['name'] if 'name' in data.keys() else None
        desc = data['description'] if 'description' in data.keys() else None
        task_type = data['task_type'] if 'task_type' in data.keys() else None
        task_subtype = data['task_subtype'] if 'task_subtype' in data.keys() else None
        version = data['version'] if 'version' in data.keys() else 1
        metrics = [Metric.from_json(metric) for metric in data['metrics']] if 'metrics' in data.keys() else None
        metadata = data['metadata'] if 'metadata' in data.keys() else None

        prob = ProblemDesc(
            name=name,
            desc=desc,
            task_type=task_type,
            subtype=task_subtype,
            version=version,
            metrics=None,
            metadata=metadata
        )

        # Override the autogenerated id
        prob.id = data['id']

        # Add metrics
        for metric in data['metrics']:
            prob.add_metric(metric)

        # Add inputs
        prob.inputs.extend([Input.from_dict(i) for i in data['inputs']])

        logger.debug("Created problem description form import: %s" % str(prob))

        return prob

    def __str__(self):
        return json.dumps(self.to_dict())

    def print(self, fpath=None):
        msg_json = self.to_dict()
        if fpath is None:
            return pprint.pformat(msg_json)
        else:
            logger.debug("Writing readable problem json to: %s" % fpath)
            with open(fpath, 'w') as out_file:
                pprint.pprint(msg_json, out_file)
            return pprint.pformat(msg_json)

       
class Input(object):
    def __init__(self, did):
        self.dataset_id = did
        self.targets = []

    def add_target(self, res, col):
        i = len(self.targets)
        target = Target(i, res, col)
        self.targets.append(target)

    def to_dict(self):
        out = {'dataset_id': self.dataset_id}
        out['targets'] = [t.to_dict() for t in self.targets]
        return out

    @staticmethod
    def from_dict(data):
        out = Input(data['dataset_id'])
        out.targets.extend([Target.from_dict(t) for t in data['targets']])
        return out

    def __str__(self):
        return str(self.to_dict())

class Target(object):
    def __init__(self, indx, res=None, col=None):
        self.target_index = indx
        if col is not None:
            self.column_index = col.colIndex
            self.column_name = col.colName
        else:
            self.column_index = None
            self.column_name = None

        if res is not None:
            self.resource_id = res.resID
        else:
            self.resource_id = None


    def to_dict(self):
        return {
            'target_index': self.target_index,
            'column_index': self.column_index,
            'column_name': self.column_name,
            'resource_id': self.resource_id
        }

    @staticmethod
    def from_dict(data):
        out = Target(data['target_index'])
        out.column_index = data['column_index']
        out.column_name = data['column_name']
        out.resource_id = data['resource_id']
        return out

    def __str__(self):
        return str(self.to_dict())


class ModelingProblem(ProblemDesc):

    @staticmethod
    def problem_target_select_to_file(prob, fpath):
        out = prob.to_dict()
        if isinstance(fpath, str):
            with open(fpath, 'w') as out_file:
                writer = csv.writer(out_file, delimiter='\t')
                # writer.writerow(Metric.get_valid_metrics("all"))
                writer.writerow(prob.get_valid_tasks())
                writer.writerow([out])
        elif isinstance(fpath, IOBase):
            writer = csv.writer(fpath, delimiter='\t')
            # Write available metrics for next step in specifying problem
            writer.writerow(Metric.__types__)
            # Write problem doc to second row
            writer.writerow([out])
        else:
            raise Exception("Invalid file/path given to write to file. Given \
                            input type: %s" % type(fpath))

    @staticmethod
    def problem_target_select_from_file(fpath):
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
        logger.debug("Got Task optiosn: %s" % str(col_names))
        logger.debug("Got dataset row with type %s:\t %s" % (str(type(rows[1][0])), str(rows[1])))
        return ProblemDesc.from_json(rows[1][0])

    @staticmethod
    def problem_task_select_to_file(prob, fpath):
        out = prob.to_dict()
        if isinstance(fpath, str):
            with open(fpath, 'w') as out_file:
                writer = csv.writer(out_file, delimiter='\t')
                writer.writerow(Metric.get_valid_metrics("all"))
                writer.writerow([out])
        elif isinstance(fpath, IOBase):
            writer = csv.writer(fpath, delimiter='\t')
            # Write available metrics for next step in specifying problem
            writer.writerow(ProblemDesc.__task_types__)
            # Write problem doc to second row
            writer.writerow([out])
        else:
            raise Exception("Invalid file/path given to write to file. Given \
                            input type: %s" % type(fpath))

    @staticmethod
    def problem_task_select_from_file(fpath):
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
        logger.debug("Got Valid Metrics: %s" % str(col_names))
        logger.debug("Got dataset row with type %s:\t %s" % (str(type(rows[1][0])), str(rows[1])))
        return ProblemDesc.from_json(rows[1][0])

    @staticmethod
    def problem_metric_select_to_file(prob, fpath):
        out = prob.to_dict()
        if isinstance(fpath, str):
            with open(fpath, 'w') as out_file:
                writer = csv.writer(out_file, delimiter='\t')
                writer.writerow(Metric.__types__)
                writer.writerow([out])
        elif isinstance(fpath, IOBase):
            writer = csv.writer(fpath, delimiter='\t')
            # Write available metrics for next step in specifying problem
            writer.writerow(Metric.__types__)
            # Write problem doc to second row
            writer.writerow([out])
        else:
            raise Exception("Invalid file/path given to write to file. Given \
                            input type: %s" % type(fpath))

    @staticmethod
    def problem_metric_select_from_file(fpath):
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
        return DefaultProblemDesc.from_json(rows[1][0])

