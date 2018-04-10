"""
This file executes Database Feature Generation
"""
import argparse
from logger import Logger
from datetime import datetime
import sys
import create_moocdb_entity_set as es
import generate_moocdb_features as ft
import ConfigParser
import pandas as pd
import csv
import os
import dateutil
import time
import pickle
import zipfile

# using extractall would be a lot shorter, but that method does not protect against path traversal vulnerabilities before Python 2.7.4
def unzip(sourcePath, destDir):
    zipRef = zipfile.ZipFile(sourcePath, 'r')
    zipRef.extractall(destDir)
    zipRef.close()

#example of running on command line
#python main.py -MOOCdbName=moocdb_game_theory_gametheory003 -startDateWF 2013-10-14
#-earliestSubmissionDate 2013-10-14 -numberWeeksWF 10 -runExtraction true -exportFormatWF tall
#-featureExtractionId 1 -featuresToExtractWF "1,3,4" -file0 placeholder

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='MOOCdb Features Generation')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument('-file0', type=str, help='the compressed file containing the input data')
    parser.add_argument('-dummyStr', type=str, help='dummy string')
    parser.add_argument('-dummyBool', type=bool, help='dummy boolean')

   # parser.add_argument('-runExtraction', choices=["true", "false"], type=str, help='run the feature extraction (default="false")', default="false")

    #parser.add_argument('-exportFormat', type=str, choices=["wide", "tall"], help='feature export format', default='wide')
    #parser.add_argument('-exportFormatWF', type=str, choices=["wide", "tall"], help='feature export format', default='wide')

    args = parser.parse_args()

    workingDir = args.workingDir
    if not workingDir:
        sys.exit("Missing required argument: workingDir")

    start_dt = datetime.now()
    logFileName = "{0:04d}{1:02d}{2:02d}{3:02d}{4:02d}{5:02d}-{6}".format(start_dt.year, start_dt.month, start_dt.day, start_dt.hour, start_dt.minute, start_dt.second, args.file0)
    logFile = workingDir + "/" + logFileName + ".log"

    log = Logger(logToConsole=True, logFilePath=logFile)

    file0 = args.file0
    if not file0:
        sys.exit("Argument file0 undefined.")

    unzip(file0, workingDir)

    class_name = None
    valid = True
    try:

        paths = workingDir.split("/")
        class_name = paths[-1]
        if len(class_name) == 0: #directory name includes a trailing slash
            class_name = paths[-2]
    except:
        print "Usage: python ft_integration.py <class database directory> ", workingDir
        valid = False
    if (valid):
        moocdb_es = es.create_moocdb_entity_set(workingDir, class_name)
        feature_tools = ft.generate_moocdb_features(moocdb_es)

        #feature_matrix, feature_defs = generate_features(moocdb_es, class_start_date, class_end_date)
        print "Writing outputs to file."
        filename = workingDir + "ft_output_pickle"
        f = open(filename, "w")
        pickle.dump(feature_tools, f)
        #pickle.dump(feature_matrix, f)
        #pickle.dump(feature_defs, f)
        f.close()

