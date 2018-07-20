
# Author: Steven C. Dang

# Main script for importing provided D3M dataset schemas for operation on datasets

import logging
import os.path as path
import sys
import json
import pprint
import argparse
import csv
import pandas as pd

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from modeling.models import *
from modeling.component_out import *


__version__ = '0.1'

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Rerank Model")
    parser.add_argument('-model_id', type=str,
                       help='the name of the dataset to import')
    parser.add_argument('-new_rank', type=int,
                       help='the new rank to resort the specified model')
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the tab-separated list of models to select from')
    args = parser.parse_args()

    if args.is_test is not None:
        is_test = args.is_test == 1
    else:
        is_test = False

    # Get config file
    config = SettingsFactory.get_settings(path.join(args.programDir, 'program', 'settings.cfg'), 
                                          program_dir=args.programDir,
                                          working_dir=args.workingDir,
                                          is_test=is_test
                                          )

    # Setup Logging
    setup_logging(config)
    logger = logging.getLogger('model_rerank')

    ### Begin Script ###
    logger.info("Reranking the models according to new given rank")
    logger.debug("Running Model Rerank with arguments: %s" % str(args))

    if args.is_test is not None:
        is_test = args.is_test == 1
    else:
        is_test = False

    # Decode the models from file
    logger.debug("ModelRank file input: %s" % args.file0)
    m_index, ranked_models = ModelRankSetIO.from_file(args.file0)
    
    selected_mid = m_index[int(args.model_id)]
    model = ranked_models[selected_mid]
    logger.debug("Seleted Model ID:\t %s" % selected_mid)
    logger.debug("Seleted Ranked Model:\t%s" % str(model.to_dict()))

    new_rank = int(args.new_rank)
    logger.debug("Previous Rank %i\t New rank: %i" % (model.rank, new_rank))

    # Resort the ranked models
    ranks = range(1, len(ranked_models)+1)
    mid_ranks = {mdl.rank: mid for mid, mdl in ranked_models.items()}
    ordered_models = [mid_ranks[i] for i in ranks if mid_ranks[i] != selected_mid]
    ordered_models.insert(new_rank - 1, selected_mid)
    # Update new ranks for all models
    for i, mid in enumerate(ordered_models):    
        ranked_models[mid].update_rank(i+1)

    # Write dataset info to output file
    out_file_path = path.join(args.workingDir, config.get('Output', 'out_file'))
    ModelRankSetIO.to_file(out_file_path, ranked_models, m_index)
