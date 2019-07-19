
# Author: Steven C. Dang

# Classes for generating problems

import logging
import os.path as path
import sys
import json
import argparse

from ls_dataset.d3m_dataset import D3MDataset
from ls_problem_desc.d3m_problem import DefaultProblemDesc
from ls_problem_desc.ls_problem import ProblemDesc

logger = logging.getLogger(__name__)

class DefaultProblemGenerator(object):
    """
    Get default d3m problem provided with each d3m dataset

    """

    def run(self, ds):

        ### Begin Script ###
        logger.info("Generating Problem Statement based on default problem for given dataset")

        logger.debug("Dataset json: %s" % str(ds))

        # Get Problem Schema from Dataset
        prob_path = DefaultProblemDesc.get_default_problem(ds)
        prob_desc = DefaultProblemDesc.from_file(prob_path)
        logger.debug("Got Problem Description for json: %s" % prob_desc.print())
        # Replace dataset references in problem description with dataset ID from db
        for inpt in prob_desc.inputs:
            inpt.dataset_id = ds._id
        for inpt in prob_desc.inputs:
            logger.debug("Checking replacement of dataset id in default problem description")
            logger.debug(str(inpt))

        return prob_desc

