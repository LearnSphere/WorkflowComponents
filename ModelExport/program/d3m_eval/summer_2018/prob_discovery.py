
# Author: Steven C. Dang

# Class supporting writing files out for problem discovery task

import logging
import os
import os.path as path
import json
from json import JSONDecodeError
from google.protobuf.json_format import MessageToJson
import csv
import pandas as pd

from ls_problem_desc.d3m_problem import GRPCProblemDesc

logger = logging.getLogger(__name__)


class ProblemDiscoveryWriter(object):
    """
    Problem Discovery output writer

    """

    def __init__(self, out_dir):
        ### Check if directory structure is in place
        self.out_dir = path.join(out_dir, 'problems')
        if not path.exists(self.out_dir):
            logger.info("Couldn't find output directory path. Creating nested path: %s" 
                    % self.out_dir)
            try:
                os.makedirs(self.out_dir)
            except OSError as e:
                logger.error("Could not create output directory for Problem Discovery \
                        \n%s" % str(e))
                raise Exception("Could not find/create output directory for Problem Discovery")
        # Initialize or read in problem list (labels) file
        self.prob_list_file = path.join(self.out_dir, 'labels.csv')
        if not path.isfile(self.prob_list_file):
            try:
                with open(self.prob_list_file, 'w') as prob_file:
                    writer = csv.writer(prob_file, delimiter=',')
                    writer.writerow(["problem_id","system","meaningful"])
            except IOError as e:
                logger.error("Could not initialize problem list file, labels.csv \
                        \n%s" % str(e))
                raise Exception("Could not initialize problem list file, labels.csv")
            self.prob_list = pd.DataFrame(columns=["problem_id","system","meaningful"])
        else:
            self.prob_list = pd.read_csv(self.prob_list_file, sep=',', index_col=False)
            # Ensure even numerical id only column is treated as a string
            self.prob_list['problem_id'] = self.prob_list.problem_id.astype(str)


    def write_output(self):
        try:
            logger.debug("#############################################")
            if path.exists(self.prob_list_file):
                logger.info("Removing old problem list file")
                os.remove(self.prob_list_file)
            self.prob_list.to_csv(self.prob_list_file, sep=',', index=False)
        except IOError as e:
            logger.error("Could not write problem list to file %s\n%s" % 
                (self.prob_list_file, str(e)))
            raise Exception("Could not write problem list to file %s" % self.prob_list_file)


    def add_problem(self, prob, search):
        logger.debug("Problem ID to add: %s\t%s" % (prob._id, type(prob._id)))
        # logger.debug("Current problem list: %s" % str(self.prob_list['problem_id']))
        logger.debug("Current problem list: %s" % str(self.prob_list.problem_id))
        logger.debug("Is in list? %s" % str(prob._id in self.prob_list.problem_id.values))
        # if prob._id not in self.prob_list.problem_id.astype(str).values:
        if prob._id not in self.prob_list.problem_id.values:
            logger.debug("Adding new problem to problem list with id: %s" % prob._id)
            self.prob_list = self.prob_list.append({'problem_id': prob._id,
                                   'system': 'user',
                                   'meaningful': 'not_asked'
                                   }, ignore_index=True)
            self.write_output()
            prob_dir = path.join(self.out_dir, prob._id)
            out_prob = GRPCProblemDesc.from_problem_desc(prob)
            try:
                # Create directory with problem id as name
                os.mkdir(prob_dir)
                # Write problem schema to file
                prob_file = path.join(prob_dir, 'schema.json')
                out_prob.to_file(prob_file)
                # Write Search Solution API request to file
                search_file = path.join(prob_dir, 'ssapi.json')
                with open(search_file, 'w') as out_file:
                    out_file.write(MessageToJson(search))
            except IOError as e:
                logger.error("Error encountered while writing new problem to file")
                raise IOError(e)
        else:
            logger.warning("Attempted to add Problem already in problem list with id %s" 
                    % prob._id)

