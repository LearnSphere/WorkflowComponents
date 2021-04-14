"""
This file executes Database Feature Generation
"""
import os
import argparse
import sys
import shutil
import pandas as pd
import warnings
warnings.filterwarnings("ignore")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Entity Set')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument('-aggPrimitives', type=str, default=".")
    parser.add_argument('-transPrimitives', type=str, default=".")
    parser.add_argument('-maxDepth', type=str, default=".")
    parser.add_argument('-encodeOutput', type=str, default="0")

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

    aggPrimitives = args.aggPrimitives
    if not aggPrimitives:
        sys.exit("Missing required argument: aggPrimitives")

    aggPrimitives = aggPrimitives.split(',')

    transPrimitives = args.transPrimitives
    if not transPrimitives:
        sys.exit("Missing required argument: transPrimitives")
    transPrimitives = transPrimitives.split(',')

    maxDepth = args.maxDepth
    if not maxDepth:
        sys.exit("Missing required argument: maxDepth")
    maxDepth = int(maxDepth)

    encodeOutput = args.encodeOutput
    if not encodeOutput:
        sys.exit("Missing required argument: encodeOutput")

    # Assign WorkingDir to Feature Tools
    os.environ['FEATURETOOLS_DIR'] = workingDir

    # Import featuretools now that we've correctly set the env var.
    import featuretools as ft
    from featuretools.selection import remove_low_information_features

    # Unzip and Unpickle Input
    dir_name = workingDir + "output.pkl"
    shutil.unpack_archive(inFile.name, dir_name, 'zip')

    es = ft.read_pickle(dir_name, load_data=True)

    if not es:
        sys.exit("Missing required argument: entity set")

    # Adjust Entity Set
    cutoff_times = es['transactions'].df[['Transaction Id', 'End Time', 'Outcome']]

    pd.options.display.max_columns = 500

    fm, features = ft.dfs(entityset=es,
                          target_entity='transactions',
                          agg_primitives=aggPrimitives,
                          trans_primitives=transPrimitives,
                          max_depth=maxDepth,
                          cutoff_time=cutoff_times[1000:],
                          verbose=True)

    if encodeOutput == "1":
        # Encode the feature matrix using One-Hot encoding
        fm_enc, f_enc = ft.encode_features(fm, features)
        fm_enc = fm_enc.fillna(0)
        fm_enc = remove_low_information_features(fm_enc)

        # Write Output to CSV
        fm_enc.to_csv("output.csv")
    else:
        # Write Output to CSV
        fm.to_csv("output.csv")

    # Remove Pickle Directory
    shutil.rmtree(dir_name)

    # Close the input file
    inFile.close()
