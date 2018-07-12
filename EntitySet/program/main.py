"""
This file executes Database Feature Generation
"""
import argparse
import sys
import utils

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Entity Set')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument('-fileName', type=str, help='File name for entity set')

    # Parse & Validate Arguments
    args, option_file_index_args = parser.parse_known_args()
    if not args.fileName:
        sys.exit("Argument(s) -node m -fileName <file name> not specified.")

    workingDir = args.workingDir
    if not workingDir:
        sys.exit("Missing required argument: workingDir")

    programDir = args.programDir
    if not programDir:
        sys.exit("Missing required argument: programDir")

    # Generate Entity Set & Pickle It.
    fileName = programDir + args.fileName

    entity_set = utils.datashop_to_entityset(fileName)

    entity_set.to_pickle(workingDir + "output.pkl")

