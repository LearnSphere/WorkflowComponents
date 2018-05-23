"""
This file executes Database Featue Extraction of the Data Science Foundry:

This file needs to be run after the MoocDb is constructed and curated

"""
import argparse
from logger import Logger
from datetime import datetime
import sys
import feature_extract as fe
import ConfigParser

#example of running on command line
#python main.py -MOOCdbName=moocdb_game_theory_gametheory003 -startDateWF 2013-10-14
#-earliestSubmissionDate 2013-10-14 -numberWeeksWF 10 -runExtraction true -exportFormatWF tall
#-featureExtractionId 1 -featuresToExtractWF "1,3,4" -node 0 -fileIndex 0 placeholder

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Feature Extraction MoocDb.')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument('-MOOCdbName', type=str, help='MoocDb name')
    parser.add_argument('-un', type=str, help='user name to access mooc-db)', default="datashop")
    parser.add_argument('-p', type=str, help='password to access mooc-db)', default="datashop")
    #parser.add_argument('-dbHost', type=str, help='host name to access mooc-db', default="127.0.0.1")
    #parser.add_argument('-dbPort', type=str, help='host port number to access mooc-db', default="3306")
    parser.add_argument('-runExtraction', choices=["true", "false"], type=str, help='run the feature extraction (default="false")', default="false")
    parser.add_argument('-startDate', type=str, help='the start date for analysis', default="")
    parser.add_argument('-startDateWF', type=str, help='the start date for analysis from WF')
    parser.add_argument('-earliestSubmissionDate', type=str, help='the earliest submission date submissions table')
    parser.add_argument('-timeout', type=int, help='how long to wait for a single feature to be extracted (in seconds)', default=1800)
    parser.add_argument('-numberWeeks', type=int, help='number of weeks for which course data exists', default=52)
    parser.add_argument('-numberWeeksWF', type=int, help='number of weeks for which course data exists for WF', default=52)
    parser.add_argument('-featuresToExtract', type=str, help='the features to extract for analysis', default="")
    parser.add_argument('-featuresToExtractWF', type=str, help='the features to extract for analysis for WF')
    parser.add_argument('-featureExtractionId', type=str, help='the id that is associated to this feature extraction configuration')
    parser.add_argument('-exportFormat', type=str, choices=["wide", "tall"], help='feature export format', default='wide')
    parser.add_argument('-exportFormatWF', type=str, choices=["wide", "tall"], help='feature export format', default='wide')
    parser.add_argument('-userId', type=str, help='placeholder for WF', default='')
    p.add_argument("-node", nargs=1, action='append')
    p.add_argument("-fileIndex", nargs=2, action='append')

    args, option_file_index_args = parser.parse_known_args()

    #read config file
    config = ConfigParser.RawConfigParser()
    if args.programDir == "..":
        config.read(args.programDir + '/ConfigFile.properties')
    else:
        config.read(args.programDir + '/program/ConfigFile.properties')
    dbHost = config.get('database', 'dbHost');
    dbPort = config.get('database', 'dbPort');

    userName = args.un
    password = args.p


    dbPort = int(dbPort)

    workingDir = args.workingDir
    if not workingDir:
        workingDir = '.'
    start_dt = datetime.now()
    logFileName = "{0:04d}{1:02d}{2:02d}{3:02d}{4:02d}{5:02d}-{6}".format(start_dt.year, start_dt.month, start_dt.day, start_dt.hour, start_dt.minute, start_dt.second, args.MOOCdbName)
    logFile = workingDir + "/" + logFileName + ".log"

    log = Logger(logToConsole=True, logFilePath=logFile)

    if args.runExtraction == "true":
        runExtraction = True
        log.log("runExtraction is true for: " + args.MOOCdbName)
    else:
        runExtraction = False
        log.log("runExtraction is false for: " + args.MOOCdbName)

    featuresToExtract = args.featuresToExtractWF
    if not featuresToExtract or featuresToExtract == "all":
            featuresToExtract = fe.featureDict.keys()
    else:
            featuresToExtract = map(int, featuresToExtract.split(','))
    featuresToExtract.sort()
    log.log("featuresToExtract: " + str(featuresToExtract))

    startDate = args.startDateWF
    if startDate == "" or startDate == "yyyy-mm-dd":
        startDate = args.earliestSubmissionDate
    log.log("startDate: " + startDate)

    if runExtraction:
        fe.extract_features(dbName             = args.MOOCdbName,
                            userName           = userName,
                            passwd             = password,
                            dbHost             = dbHost,
                            dbPort             = dbPort,
                            timeout            = 86400,
                            startDate          = startDate,
                            earliestSubmissionDate          = args.earliestSubmissionDate,
                            numWeeks           = args.numberWeeksWF,
                            features_to_extract= featuresToExtract,
                            featureExtractionId   = args.featureExtractionId,
                            logger             = log,
                            )
    #export features with feature description
    fe.exportFeatures(dbName            = args.MOOCdbName,
                        userName        = userName,
                        passwd          = password,
                        dbHost          = dbHost,
                        dbPort          = dbPort,
                        exportFileFolderPath = workingDir,
                        featureExtractionId   = args.featureExtractionId,
                        featuresToExtract = featuresToExtract,
                        exportFormat    = args.exportFormatWF,
                        logger          = log,)
