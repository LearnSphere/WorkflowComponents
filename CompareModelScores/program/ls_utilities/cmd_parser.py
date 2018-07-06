
# Author: Steven C. Dang

# Convenience class and functions for supporting parsing cmd line inputs
# when running Workflow components

# import logging
import argparse


# logging.basicConfig()
# logger = logging.getLogger('cmd_parser')


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

    # For local testing outside of Tigris env
    parser.add_argument('-is_test', type=int,
                       help='set to any integer to indicate this script is being  \
                       run outside a Tigris Workflow')
    return parser


