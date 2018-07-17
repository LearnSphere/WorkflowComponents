"""
This file executes Database Feature Generation
"""
import argparse
import sys
import utils
import shutil

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Entity Set')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')

    # Parse & Validate Arguments
    args, option_file_index_args = parser.parse_known_args()

    inFile = None
    for x in range(len(args.node)):
        if args.node[x][0] == "0" and args.fileIndex[x][0] == "0":
            inFile = open(args.fileIndex[x][1], 'r')
    if not inFile:
        sys.exit("Argument(s) -node m -fileIndex n <file> not specified.")

    workingDir = args.workingDir
    if not workingDir:
        sys.exit("Missing required argument: workingDir")

    programDir = args.programDir
    if not programDir:
        sys.exit("Missing required argument: programDir")

    # Generate Entity Set & Pickle It.
    entity_set = utils.datashop_to_entityset(inFile)

    dir_name = workingDir + "output.pkl"

    entity_set.to_pickle(dir_name)

    # Zip Output.pkl and Remove Directory
    shutil.make_archive('output', 'zip', dir_name)
    shutil.rmtree(dir_name)

