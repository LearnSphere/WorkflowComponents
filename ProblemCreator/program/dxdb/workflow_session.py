
# Author: Steven C. Dang

# MongoDB interface DataExplorer DB

import logging
import inspect

from ls_iviz.simple_eda import *


logger = logging.getLogger(__name__)

class WorkflowSession(object):
    
    available_states = ['Not Ready']
    
    def __init__(self, 
                 user_id, 
                 workflow_id, 
                 comp_id, 
                 comp_type, 
                 _id=None, 
                 state=None,
                 session_url=None
                 ):
        if _id is not None:
            self._id = _id
        self.user_id = user_id
        self.workflow_id = workflow_id
        self.comp_type = comp_type
        self.comp_id = comp_id
        self.session_url = session_url
        if state is None:
            self.state = self.available_states[0]
        else:
            self.state = state

    def to_json(self):
        logger.debug("Type of self: %s" % str(type(self)))
        return json.dumps(self, default=lambda o: o.__dict__, 
                                      sort_keys=True, indent=4)

    @staticmethod
    def from_json(ses_json): 
        logger.debug("Initializing WorkflowSession from json: %s" % str(ses_json))
        if ses_json['comp_type'].lower() == "datasetimporter":
            logger.debug("initializing a dataset importer session")
            ses = ImportDatasetSession(**ses_json)
        elif  ses_json['comp_type'].lower() == "simpleedasession":
            logger.debug("initializing a simple eda session")
            ses = SimpleEDASession(**ses_json)
        elif  ses_json['comp_type'].lower() == "problemcreator":
            logger.debug("initializing a problem creation session")
            ses = ProblemCreatorSession(**ses_json)
        else:
            raise Exception("Unable to identify class to initialize workflow session with session: %s" % str(ses_json))
        return ses
        # if issubclass(cls, WorkflowSession):
            # ses = cls(**ses_json)
            # return ses
        # else:
            # raise Exception("Invalid class given: %s" % str(cls))

    def set_session_url(self, url):
        self.session_url = url

    def get_possible_states(self):
        return self.available_states
        

class SimpleEDASession(WorkflowSession):

    dataset_id = None

    def __init__(self, userId, workflowId, compType, session_url=None, dataset=None):
        if dataset is not None:
            self.dataset_id = dataset._id
        super().__init__(userId, workflowId, compType, session_url)
        self.visualizations = []

    def set_dataset(self, dataset):
        self.dataset_id = dataset._id

    def add_viz(self, viz):
        v_ids = set(self.visualizations)
        if viz._id not in v_ids:
            self.visualizations.append(viz._id)

    @classmethod
    def from_json(cls, ses_json): 
        logger.debug("Initializing SimpleEDASession from json: %s" % str(ses_json))
        ses = cls(ses_json['user_id'], ses_json['workflow_id'],
                         ses_json['component_type'], ses_json['session_url'],
        )
        ses.dataset_id = ses_json['dataset_id']
        for vizid in ses_json['visualizations']:
             logger.debug("Adding visualization id to eda session: %s" % vizid)
             ses.visualizations.append(vizid)
        return ses

class ImportDatasetSession(WorkflowSession):

    available_states = ['Not Ready', 
                        'No Dataset Imported',
                        'Dataset Imported'
                        ]
    def __init__(self, 
                 user_id, 
                 workflow_id, 
                 comp_id, 
                 comp_type, 
                 _id=None, 
                 state=None,
                 dataset_id=None,
                 available_datasets=None,
                 session_url=None):
        super().__init__(user_id=user_id, 
                         workflow_id=workflow_id, 
                         comp_id=comp_id, 
                         comp_type=comp_type, 
                         _id=_id, 
                         session_url=session_url)
        self.dataset_id = dataset_id
        if available_datasets is None:
            self.available_datasets = []
        else:
            self.available_datasets = available_datasets
        if state is None:
            self.state = self.available_states[0]
        else:
            self.state = state

    def set_state_ready(self):
        self.state = self.available_states[1]

    def set_state_complete(self):
        self.state = self.available_states[2]

    def is_state_complete(self):
        return self.state == self.available_states[2]

    def set_dataset_id(self, dataset_id):
        logger.info("Setting session dataset id: %s" % dataset_id)
        self.dataset_id = dataset_id

    def set_available_dataset_ids(self, datasets):
        # Takes a list of dataset IDs
        self.available_datasets = datasets

    def get_available_dataset_ids(self):
        return self.available_datasets
    

class ProblemCreatorSession(WorkflowSession):

    available_states = ['Not Ready', 
                        'Problem Creation Incomplete',
                        'Problem Creation Completed'
                        ]
    def __init__(self, 
                 user_id, 
                 workflow_id, 
                 comp_id, 
                 comp_type, 
                 _id=None, 
                 state=None,
                 input_wfids=[],
                 dataset_id=None,
                 prob_id=None,
                 prob_state={},
                 suggest_pids=[],
                 session_url=None):
        super().__init__(user_id=user_id, 
                         workflow_id=workflow_id, 
                         comp_id=comp_id, 
                         comp_type=comp_type, 
                         _id=_id, 
                         session_url=session_url)
        self.dataset_id = dataset_id
        self.prob_id = prob_id
        self.prob_state = prob_state
        self.input_wfids = input_wfids
        self.suggest_pids = suggest_pids
        if state is None:
            self.state = self.available_states[0]
        else:
            self.state = state

    def set_state_ready(self):
        self.state = self.available_states[1]

    def set_state_complete(self):
        self.state = self.available_states[2]

    def is_state_complete(self):
        return self.state == self.available_states[2]

    def check_state_complete(self):
        # Check problem state for valid and sufficient user input
        return False # stubbed for now

    def set_dataset_id(self, ds_id):
        logger.debug("Setting session dataset id: %s" % ds_id)
        self.dataset_id = ds_id

    def set_problem_id(self, prob_id):
        logger.debug("Setting session problem id: %s" % prob_id)
        self.problem_id = prob_id

    def set_prob_state(self, prob):
        logger.debug("Setting problem state with problem: %s" % prob.to_json())
        state = {'Name': prob.name,
                 'Description': prob.description,
                 'TaskType': prob.task_type,
                 'SubType': prob.subtype
                 }
        self.prob_state = state

    def update_prob_state(self, field, value):
        logger.debug("Updating problem state field, %s, with value: %s" % (field, str(value)))
        self.state[field] = value

    def set_input_wfids(self, wfids):
        logger.debug("Adding list of input workflow ids to session")
        self.input_wfids = wfids

    def add_suggestion_prob(self, prob):
        logger.debug("Adding problem ID to list of suggested problems")
        self.suggest_pids.append(prob._id)

class ModelSearchSession(WorkflowSession):

    available_states = ['Not Ready', 
                        'Searching for Solutions',
                        'Fitting Solutions',
                        'Scoring Solutions',
                        'Getting Predicitons',
                        'Solution Search Completed'
                        ]
    def __init__(self, 
                 user_id, 
                 workflow_id, 
                 comp_id, 
                 comp_type, 
                 _id=None, 
                 state=None,
                 input_wfids=[],
                 dataset_id=None,
                 prob_id=None,
                 ta2_addr=None,
                 session_url=None):
        super().__init__(user_id=user_id, 
                         workflow_id=workflow_id, 
                         comp_id=comp_id, 
                         comp_type=comp_type, 
                         _id=_id, 
                         session_url=session_url)
        self.dataset_id = dataset_id
        self.prob_id = prob_id
        self.input_wfids = input_wfids
        self.ta2_addr = ta2_addr
        if state is None:
            self.state = self.available_states[0]
        else:
            self.state = state


    def set_state_ready(self):
        self.state = self.available_states[1]

    def set_state_complete(self):
        self.state = self.available_states[-1]

    def is_state_complete(self):
        return self.state == self.available_states[-1]

    def check_state_complete(self):
        # Check problem state for valid and sufficient user input
        return False # stubbed for now

    def set_dataset_id(self, ds_id):
        logger.debug("Setting session dataset id: %s" % ds_id)
        self.dataset_id = ds_id

    def set_problem_id(self, prob_id):
        logger.debug("Setting session problem id: %s" % prob_id)
        self.problem_id = prob_id

    def set_input_wfids(self, wfids):
        logger.debug("Adding list of input workflow ids to session")
        self.input_wfids = wfids

