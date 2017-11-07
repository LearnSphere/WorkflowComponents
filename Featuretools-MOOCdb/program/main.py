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

#example of running on command line
#python main.py -MOOCdbName=moocdb_game_theory_gametheory003 -startDateWF 2013-10-14
#-earliestSubmissionDate 2013-10-14 -numberWeeksWF 10 -runExtraction true -exportFormatWF tall
#-featureExtractionId 1 -featuresToExtractWF "1,3,4" -file0 placeholder
    
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='MOOCdb Features Generation')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")  
    parser.add_argument('-class_dir', type=str, help='the directory that contains the .tsv files')
    
   # parser.add_argument('-runExtraction', choices=["true", "false"], type=str, help='run the feature extraction (default="false")', default="false")

    #parser.add_argument('-exportFormat', type=str, choices=["wide", "tall"], help='feature export format', default='wide')
    #parser.add_argument('-exportFormatWF', type=str, choices=["wide", "tall"], help='feature export format', default='wide')
    parser.add_argument('-file0', type=str, help='placeholder for WF', default='')
    
    args = parser.parse_args()

    workingDir = args.workingDir
    if not workingDir:
        workingDir = '.'
        
    start_dt = datetime.now()
    logFileName = "{0:04d}{1:02d}{2:02d}{3:02d}{4:02d}{5:02d}-{6}".format(start_dt.year, start_dt.month, start_dt.day, start_dt.hour, start_dt.minute, start_dt.second, args.class_dir)
    logFile = workingDir + "/" + logFileName + ".log"

    log = Logger(logToConsole=True, logFilePath=logFile)

    class_dir = args.class_dir
    if not class_dir:
        class_dir = './program/inputs/'
    class_name = None
    valid = True
    try:
        class_dir = class_dir
        paths = class_dir.split("/")
        class_name = paths[-1]
        if len(class_name) == 0: #directory name includes a trailing slash
            class_name = paths[-2]
    except:
        print "Usage: python ft_integration.py <class database directory> ", class_dir 
        valid = False
    if (valid):
        moocdb_es = es.create_moocdb_entity_set(class_dir, class_name)
        feature_tools = ft.generate_moocdb_features(moocdb_es)

        #feature_matrix, feature_defs = generate_features(moocdb_es, class_start_date, class_end_date)
        print "Writing outputs to file."
        filename = workingDir + "ft_output_pickle"
        f = open(filename, "w")
        pickle.dump(feature_tools, f)
        #pickle.dump(feature_matrix, f)
        #pickle.dump(feature_defs, f)
        f.close()
        