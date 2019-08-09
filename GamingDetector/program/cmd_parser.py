
# Author: Steven C. Dang

# Convenience class and functions for supporting parsing cmd line inputs
# when running Workflow components

import logging
import argparse
import os.path as path


# logging.basicConfig()
logger = logging.getLogger(__name__)


def get_default_arg_parser(desc):
    """
    Define an argument parser for use with Tigris Components and
    mandatory arguments

    """
    parser = argparse.ArgumentParser(description=desc)

    parser.add_argument('-programDir', type=str,
                       help='the component program directory')

    parser.add_argument('-workingDir', type=str,
                       help='the component instance working directory')

    parser.add_argument('-userId', type=str,
                       help='the user id')

    parser.add_argument('-workflowDir', type=str,
                       help='the workflow directory')

    parser.add_argument('-toolDir', type=str,
                       help='the main directory of the component')

    parser.add_argument('-componentXmlFile', type=str,
                       help='a fake path to a component xml file for this instance of the component')


    # For local testing outside of Tigris env
    parser.add_argument('-is_test', type=int,
                       help='set to any integer to indicate this script is being  \
                       run outside a Tigris Workflow')
    return parser

def get_input_files(args, indx):
    """
    Get files specified at a given node index

    """
    out_files = []
    for node_indx in range(len(args.node)):
        # Getting dataset session from db given at node 0 of component
        logger.debug("Looking at input node #%i" % node_indx)
        logger.debug("Node %i: %s" % (node_indx, args.node[node_indx]))
        if int(args.node[node_indx][0]) == indx:
            logger.debug("Getting fileindex for node 0 at node index: %i" % node_indx)
            inpts = args.fileIndex[node_indx]
            # Double check that indices match between node and fileindex
            if int(inpts[0]) == indx:
                out_files.append(inpts[1])
    logger.debug("Got files: %s" % str(out_files))
    return out_files



