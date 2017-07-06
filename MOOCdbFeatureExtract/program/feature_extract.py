"""
Main feature extraction file. Consists of feature extraction scripts and their helper functions.
"""

import time
from sql_functions_feature_extract.sql_functions import *
import os
import getpass
import datetime
import sys
import platform
from logger import Logger
import traceback
import warnings

##################################### MAIN FUNCTION: EXTRACT FEATURES ##########################################

def extract_features(dbName, userName, passwd, dbHost, startDate, earliestSubmissionDate, numWeeks, features_to_extract, featureExtractionId,
        dbPort=3306, currentDate=datetime.datetime.now().isoformat(), timeout = 1800, logger=None):
    """
    Note: depending upon the platform this program is running on, compute_featrues() is called differently
    
    Args
        dbName:             Name of MySQL database \n
        userName:           Username for MySQL database \n
        passwd:             Password for MySQL database \n
        dbHost:             Host for MySQL database (e.g., 'localhost') \n
        dbPort:             Port for MySQL database (e.g., 3306) \n
        startDate:          User defined starting date for course \n
        currentDate:        Today's date \n
        features_to_extract:Features to extract in extraction, used when features_to_skip is None\n
        earliestSubmissionDate: The earliest submission date in submissions table \n
        featureExtractionId: feature_extraction_id that corresponds to the configuration of this feature extraction (decided by startDate, feature_extraction_list, num_of_weeks) \n
        timeout:            How long to wait for a single feature to be extracted (in seconds) \n
        numWeeks:           Number of weeks for which course data exists \n
    """

    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger

    dependencies_dict = get_dependencies()
    allFeatures = []
    for feature in features_to_extract:
        allFeatures.append(feature)
        if feature in dependencies_dict.keys():
            allFeatures.extend(dependencies_dict[feature])
    #features_to_extract = ','.join(str(x) for x in sorted(list(set(allFeatures))))
    features_to_extract = sorted(list(set(allFeatures)))
    
    connection = openSQLConnectionP(dbName, userName, passwd, dbHost, dbPort) 
    if (platform.system().lower().find("win") > -1):
        compute_features(conn=None, host=dbHost, port=dbPort, userName=userName, passwd=passwd,
                         dbName=dbName, startDate=startDate, earliestSubmissionDate=earliestSubmissionDate, currentDate=currentDate, numWeeks=numWeeks,
                         featuresToExtract=features_to_extract, featureExtractionId=featureExtractionId, timeout=timeout, logger=log)
    else:
        compute_features(conn=connection,
                         dbName=dbName, startDate=startDate, earliestSubmissionDate=earliestSubmissionDate, currentDate=currentDate, numWeeks=numWeeks,
                         featuresToExtract=features_to_extract, featureExtractionId=featureExtractionId, timeout=timeout, logger=log)
    connection.close()

###################################### UTILITIES FROM MAIN #####################################################

def write_to_csv(conn,dbName):
    """
    Writes features to csv in the /tmp directory

    Args
        conn:       connection to MySQL database \n
        dbName:     name of database \n \n
    """
    fileName = 'sql_scripts/export_to_csv.sql'

    print 'writing features to csv'
    dirName = 'sql_functions_feature_extract'
    this_file = os.path.dirname(os.path.realpath(__file__))
    fileLocation = dirName+'/'+fileName
    fileLocation = this_file+'/'+fileLocation
    txt = open(fileLocation, 'r').read()
    try:
        executeSQL(conn, txt)
    except:
        print "\n ERROR: Write unsucessful."
        print "Help message below:"
        print "Writing to csv files in MySQL requires 2 steps:"
        print "\t 1) Add the following line to the my.cnf file in your MySQL setup files:"
        print "\t \t secure_file_priv=''"
        print "\t 2) Change the directory specified in export_to_csv.sql to one which the MySQL user has write permissions"
        print "\t 3) Delete user_longitudinal_feature_values.csv if already exists in directory"
        print "Original error message: \n"
        raise
    print 'done'

def exportFeatures(dbName, userName, passwd, dbHost,exportFileFolderPath,featuresToExtract,featureExtractionId,
                   dbPort=3306, exportFormat=None,logger=None):
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    connection = openSQLConnectionP(dbName, userName, passwd, dbHost,dbPort)
    if exportFormat is None:
        exportFormat = "tall"

    #featureFilePath = exportFileFolderPath+"/" + dbName + "_features.txt"
    featureFilePath = exportFileFolderPath+"/moocdb_features.txt"
    descFilePath = exportFileFolderPath+"/feature_descriptions.txt"
    if exportFormat == "wide":
        writeFeaturesInWideForm(connection,dbName,featureFilePath,featuresToExtract,featureExtractionId,log)
    else:
        writeFeaturesInTallForm(connection,dbName,featureFilePath,featuresToExtract,featureExtractionId,log)
    writeFeaturesDescription(connection,dbName,descFilePath,log)
    connection.close()
    

def writeFeaturesInTallForm(conn,dbName,filePath,featuresToExtract,featureExtractionId,logger=None):
    """
    Writes features in tall format to tab-delimited file

    Args
        conn:       connection to MySQL database \n
        dbName:     name of database \n
        filePath:   where file is \n
        featuresToExtract:   what features to include \n
        featureExtractionId: feature_extraction_id from db table \n
    """
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger

    log.log('writing features in tall format')
    sql = '''SELECT a.longitudinal_feature_id,
                    b.longitudinal_feature_name,
                    a.user_id,
                    a.longitudinal_feature_week,
                    a.longitudinal_feature_value,
                    a.date_of_extraction
             FROM `%s`.user_longitudinal_feature_values a
             JOIN `%s`.longitudinal_features b
             ON a.longitudinal_feature_id = b.longitudinal_feature_id
             WHERE a.longitudinal_feature_id in (%s) 
                     AND a.feature_extraction_id = %s ''' % (dbName, dbName, ",".join(str(i) for i in featuresToExtract), featureExtractionId)
    sql += ''' ORDER BY a.longitudinal_feature_id, a.user_id, a.longitudinal_feature_week '''
    log.log(sql)
    try:
        cursor = conn.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute(sql)
        rows = cursor.fetchall()
        outfile = open(filePath, 'w')
        headers = ["Feature ID", "Feature Name", "User ID", "Longitudinal Feature Week", "Longitudinal Feature Value", "Date Of Extraction"]
        cols = ["longitudinal_feature_id", "longitudinal_feature_name", "user_id", "longitudinal_feature_week", "longitudinal_feature_value", "date_of_extraction"]
        outfile.write("\t".join(headers) + "\n")
        
        for row in rows:
            strRow = ""
            for colName in cols:
                strRow += str(row[colName])+ "\t"
            outfile.write(strRow.strip())
            outfile.write("\n")
        cursor.close()
    except Exception as error:
        log.log("\n ERROR: writing longitudinal features tall form unsucessful.")
        log.log("\n Traceback: " + traceback.format_exc())
    log.log('done')
    

def writeFeaturesInWideForm(conn,dbName,filePath,featuresToExtract,featureExtractionId,logger=None):
    """
    Writes features in wide format to tab-delimited file

    Args
        conn:       connection to MySQL database \n
        dbName:     name of database \n
        filePath:   where file is \n
        featuresToExport:   what features to include \n
       featureExtractionId: feature_extraction_id from db table \n 
    """
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger

    log.log('writing features in wide format')
    selectStmt = "SELECT DISTINCT features.user_id, features.longitudinal_feature_week, "
    fromStmt = "FROM (SELECT distinct user_id, longitudinal_feature_week "
    fromStmt += "FROM user_longitudinal_feature_values where longitudinal_feature_id in ({}) ".format(",".join(map(str, featuresToExtract)))
    fromStmt += " and feature_extraction_id = {} ".format(featureExtractionId)
    fromStmt += ") features "
    headers = ["User ID", "Longitudinal Feature Week"]
    cols = ["user_id", "longitudinal_feature_week"]
    for feature in featuresToExtract:
        headers.append("{}".format(featureDict[feature]['name']))
        cols.append("feature{}_value".format(feature))
        selectStmt += "features{}.longitudinal_feature_value as feature{}_value,".format(feature, feature)
        fromStmt += "LEFT JOIN user_longitudinal_feature_values AS features{} on ".format(feature)
        fromStmt += "features.user_id = features{}.user_id AND ".format(feature)
        fromStmt += "features.longitudinal_feature_week = features{}.longitudinal_feature_week AND ".format(feature)
        fromStmt += "features{}.longitudinal_feature_id = {} AND ".format(feature,feature)
        fromStmt += "features{}.feature_extraction_id = {} ".format(feature,featureExtractionId)
    selectStmt = selectStmt[:-1]
    orderByStmt = "order by user_id, longitudinal_feature_week "
    qStmt = selectStmt + " " + fromStmt + " " + orderByStmt
    try:
        cursor = conn.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute(qStmt)
        rows = cursor.fetchall()
        outfile = open(filePath, 'w')
        outfile.write("\t".join(headers) + "\n")
        for row in rows:
            strRow = ""
            for colName in cols:
                strRow += str(row[colName])+ "\t"
            outfile.write(strRow.strip())
            outfile.write("\n")
        cursor.close()
    except Exception as error:
        log.log("\n ERROR: writing longitudinal features wide form file unsucessful.")
        log.log("\n Traceback: " + traceback.format_exc())
    log.log('done')

    


def writeFeaturesDescription(conn,dbName,filePath,logger=None):
    """
    Writes features in wide format to tab-delimited file

    Args
        conn:       connection to MySQL database \n
        dbName:     name of database \n
        filePath:   where file is \n
        featuresToExport:   what features to include
    """
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    log.log('writing features description')
    sql = '''SELECT longitudinal_feature_id,
                 longitudinal_feature_name,
                 longitudinal_feature_description
             FROM `%s`.longitudinal_features
             ORDER BY longitudinal_feature_id
          ''' % (dbName)
    try:
        cursor = conn.cursor(MySQLdb.cursors.DictCursor)
        cursor.execute(sql)
        rows = cursor.fetchall()
        outfile = open(filePath, 'w')
        headers = ["longitudinal_feature_id", "longitudinal_feature_name", "longitudinal_feature_description"]
        outfile.write("\t".join(headers) + "\n")
        for row in rows:
            for colName in headers:
                outfile.write(str(row[colName])+ "\t")
            outfile.write("\n")
        cursor.close()
    except Exception as error:
        log.log("\n ERROR: writing longitudinal feature description file unsucessful.")
        log.log("\n Traceback: " + traceback.format_exc())
    log.log('done')


###################################### FEATURE EXTRACTION ######################################################

def extractFeature(conn, dbName, startDate, earliestSubmissionDate, currentDate, numWeeks, dirName, featureExtractionId, featureID, timeout,
                   host=None, port=None, userName=None, passwd=None, logger=None):

    """
    Extract single feature.

    Uses the feature dictionary (feature_dict) made in feature_dict.py and the feature extraction scripts (one for each feature) in feat_extract_scripts.
    
    Args
        conn:                       connection to MySQL database \n
        dbName:                     name of database \n
        startDate:                  Starting date for course data \n
        earliestSubmissionDate:     The earliest submission date in submissions table \n
        currentDate:                Today's date \n
        numWeeks:                   Number of weeks for which course data exists \n
        dirName:                    Directory where feature extraction scripts are held \n
        featureID:                  ID of feature to be extracted \n
        featureExtractionId:        Feature extraction id in feature_extractions table for this extraction configuration \n
        timeout:                    Amount of time allowed to extract single feature \n
        host,port,userName,passwd:  used on windows platform for db connection\n
    """
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    begin = time.time()
    if featureID not in featureDict:
        log.log("unsupported feature: " + str(featureID))
        return False
    feature = featureDict[featureID]
    isSQL = (feature['extension'] == '.sql')
    log.log("extracting feature %s: %s" % (featureID, feature["name"]))
    if isSQL:
        featureFile = dirName+'/'+feature['filename']+feature['extension']
        this_file = os.path.dirname(os.path.realpath(__file__))
        featureFile = this_file+'/'+featureFile
        toBeReplaced = ['moocdb', 'START_DATE_PLACEHOLDER', 'EARLIEST_SUBMISSION_DATE_PLACEHOLDER',
                'CURRENT_DATE_PLACEHOLDER', 'NUM_WEEKS_PLACEHOLDER', 'FEATURE_EXTRACTION_ID_PLACEHOLDER']
        toReplace = [dbName, startDate, earliestSubmissionDate, currentDate, str(numWeeks), featureExtractionId]

        success = runSQLFile(host=host, port=port, userName=userName, passwd=passwd,
                            conn=conn, fileName=featureFile, dbName=dbName, toBeReplaced=toBeReplaced,
                            toReplace=toReplace, timeout=timeout, logger=log)
               
    else:
        # conn2 = openSQLConnectionP(dbName, userName, passwd, host, port) # used to be done with two connections (to revert, also need to change how make connections in above functions)
        featureFile = dirName+'/'+feature['filename']
        success = runPythonFile(conn=conn, conn2=conn, module=dirName, fileName=feature['filename'],
                dbName=dbName, startDate=startDate, currentDate=currentDate,
                numWeeks=numWeeks, featureExtractionId=featureExtractionId,
                timeout=timeout, host=host, port=port, userName=userName, passwd=passwd, logger=log)
    end = time.time()
    log.log("Elapsed time = " + str(end-begin))
    if not success:
        log.log("feature " + feature['name'] + " failed")
        return False
    else:
        return True

    
def compute_features(conn, dbName, startDate, earliestSubmissionDate, currentDate, numWeeks, featuresToExtract, featureExtractionId, timeout,
                     host=None, port=None, userName=None, passwd=None, logger=None):
    """
    Extract features from a database

    Uses the feature dictionary (feature_dict) made in feature_dict.py and the feature extraction scripts (one for each feature) in feat_extract_scripts.
    
    Args
        conn:                       connection to MySQL database \n
        dbName:                     name of database \n
        startDate:                  Starting date for course data \n
        earliestSubmissionDate:     The earliest submission date in submissions table \n
        currentDate:                Today's date \n
        numWeeks:                   Number of weeks for which course data exists \n
        featuresToExtract:          Features to extract when featuresToSkip is None \n
        featureExtractionId:        Feature extraction id in feature_extractions table for this extraction configuration \n
        timeout:                    Amount of time allowed to extract single feature \n
        host,port,userName,passwd:  used on windows platform for establishing database connection\n
    """
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger

    log.log("Extracting features for MOOCdb: " + dbName)
    dirName = 'sql_functions_feature_extract/feat_extract_scripts'
    for feature in featuresToExtract:
        success = extractFeature(conn=conn, host=host, port=port, userName=userName, passwd=passwd, dbName=dbName,
                                 startDate=startDate, earliestSubmissionDate=earliestSubmissionDate, currentDate=currentDate, numWeeks=numWeeks, dirName=dirName,
                                 featureExtractionId=featureExtractionId, featureID=feature, timeout=timeout, logger=log)
        
    log.log("Done with feature extraction: " + str(featuresToExtract))


def get_dependencies():
    dependencies_dict = {}
    for feature_id in featureDict.keys():
        dependencies = featureDict[feature_id]['dependencies']
        if len(dependencies) != 0:
            for dependency in dependencies:
                if dependency in dependencies_dict.keys():
                    dependencies_dict[dependency].append(feature_id)
                else:
                    dependencies_dict.setdefault(dependency,[]).append(feature_id)
                #if feature_id has dependency already, add them
                if feature_id in dependencies_dict.keys():
                    dependencies_dict[dependency].extend(dependencies_dict[feature_id])
    #clean up the list
    for feature_id in dependencies_dict.keys():
        dependencies_dict[feature_id] = sorted(list(set(dependencies_dict[feature_id])))
    return dependencies_dict



###################################### FEATURE DICTIONARY ######################################################

"""
Contains a dictionary of all the potential features that can be extracted, and each feature in turn contains a dictionary of its salient features. This dictionary has the following format: \n

.. code-block:: language

    key -> feature_id

    value -> a dictionary with the following key/value pairs:
    
       KEY              VALUE

       name:            descriptive name of feature,
       filename:        filename containing feature script without extension,
       extension:       either .sql or .py,
       default:         default value of feature,
       dependencies:    later features that depend on this extraction being run first,
       desc:            description of what this feature is looking at
"""
featureDict = {
     1:  {'name': "dropout",
         'filename': 'populate_feature_1_dropout',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': "Whether or not the student has dropped out by this week (this is the label used in prediction)."},
      2:  {'name': "sum_observed_events_duration",
         'filename': 'populate_feature_2_sum_observed_events_duration',
         'extension': '.sql',
         'default': 0,
         'dependencies':[10],
         'desc': "Total time spent on each resource during the week. "},
     3:  {'name': "number_of_forum_posts",
         'filename': 'populate_feature_3_number_of_forum_posts',
         'extension': '.sql',
         'default': 0,
         'dependencies':[103],
         'desc': " Number of forum posts during the week."},
     4:  {'name': "number_of_wiki_edits",
         'filename': 'populate_feature_4_number_of_wiki_edits',
         'extension': '.sql',
         'default': 0,
         'dependencies':[104],
         'desc': "Number of wiki edits during the week. "},
     5:  {'name': "average_length_of_forum_posts",
         'filename': 'populate_feature_5_average_length_of_forum_posts',
         'extension': '.sql',
         'default': 0,
         'dependencies':[105],
         'desc': " Average length of forum posts during the week."},
     6:  {'name': "distinct_attempts",
         'filename': 'populate_feature_6_distinct_attempts',
         'extension': '.sql',
         'default': 0,
         'dependencies':[11,111],
         'desc': "Number of distinct problems attempted during the week. "},
     7:  {'name': "number_of_attempts",
         'filename': 'populate_feature_7_number_of_attempts',
         'extension': '.sql',
         'default': 0,
         'dependencies':[209],
         'desc': " Number of potentially non-distinct problem attempts during the week."},
     8:  {'name': "distinct_problems_correct",
         'filename': 'populate_feature_8_distinct_problems_correct',
         'extension': '.sql',
         'default': 0,
         'dependencies':[10,11,110,111],
         'desc': "Number of distinct problems correct during the week. "},
     9:  {'name': "average_number_of_attempts",
         'filename': 'populate_feature_9_average_number_of_attempts',
         'extension': '.sql',
         'default': 0,
         'dependencies':[109,202,203],
         'desc': "Average number of problem attempts during the week. "},
     10: {'name': "sum_observed_events_duration_per_correct_problem",
         'filename': 'populate_feature_10_sum_observed_events_duration_per_correct_problem',
         'extension': '.sql',
         'default': -1,
         'dependencies':[110],
         'desc': " Total time spent on all resources during the week (feat. 2) divided by number of correct problems (feat. 8)."},
     11: {'name': "number_problem_attempted_per_correct_problem",
         'filename': 'populate_feature_11_number_problem_attempted_per_correct_problem',
         'extension': '.sql',
         'default': -1,
         'dependencies':[111],
         'desc': " Number of problems attempted (feat. 6) divided by number of correct problems (feat. 8)."},
     12: {'name': "average_time_to_solve_problem",
         'filename': 'populate_feature_12_average_time_to_solve_problem',
         'extension': '.sql',
         'default': -1,
         'dependencies':[112],
         'desc': " Average of (max(attempt.timestamp) - min(attempt.timestamp)) for each problem during the week."},
     13: {'name': "observed_event_timestamp_variance",
         'filename': 'populate_feature_13_observed_event_timestamp_variance',
         'extension':  '.py',
         'default': 0,
         'dependencies':[],
         'desc': "Variance of a students observed event timestamps in one week. "},
     14: {'name': "number_of_collaborations",
         'filename': 'populate_feature_14_number_of_collaborations',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Number of collaborations during the week."},
     15: {'name': "max_duration_resources",
         'filename': 'populate_feature_15_max_duration_resources',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Duration of longest observed event"},
    
    16: {'name': "lecture_event_duration",
         'filename': 'populate_feature_16_lecture_event_duration',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Total time spent on all lecture-related resources during the week."},
    #17: {'name': "sum_observed_events_book",
    #   'filename': 'populate_feature_17_sum_observed_events_book',
    #     'extension': '.sql',
    #    'default': 0,
    #     'dependencies':[],
    #     'desc': " Total time spent on all book-related resources during the week."},
     18: {'name': "wiki_event_duration",
         'filename': 'populate_feature_18_wiki_event_duration',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Total time spent on all wiki-related resources during the week."},

     19: {'name': "attempt_duration",
         'filename': 'populate_feature_19_attempt_duration',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Total time spent on attempting questions for quizzes during the week."},

    20: {'name': "number_of_lecture_events",
         'filename': 'populate_feature_20_number_of_lecture_events',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Total number of lecture/video related observed events during the week."},

    21: {'name': "number_of_test_submissions",
         'filename': 'populate_feature_21_number_test_submission',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Total time spent on attempting questions for quizzes during the week."},
         
    
     103:{'name': "difference_feature_3",
         'filename': 'populate_feature_103_difference_feature_3',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Number of forum posts in current week divided by number of forum posts in previous week (difference of feature 3)."},
     104:{'name': "difference_feature_4",
         'filename': 'populate_feature_104_difference_feature_4',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Number of wiki edits in current week divided by number of wiki edits in previous week (difference of feature 4)."},
     105:{'name': "difference_feature_5",
         'filename': 'populate_feature_105_difference_feature_5',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Average length of forum posts in current week divided by average length of forum posts in previous week, where number of forum posts in previous week is > 5 (difference of feature 5)."},
     109:{'name': "difference_feature_9",
         'filename': 'populate_feature_109_difference_feature_9',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Average number of attempts in current week divided by average number of attempts in previous week (difference of feature 9)."},
     110:{'name': "difference_feature_10",
         'filename': 'populate_feature_110_difference_feature_10',
         'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " (Total time spent on all resources during current week (feat. 2) divided by number of correct problems during current week (feat. 8)) divided by same thing from previous week (difference of feature 10)."},
     111:{'name': "difference_feature_11",
             'filename': 'populate_feature_111_difference_feature_11',
             'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " (Number of problems attempted (feat. 6) divided by number of correct problems (feat. 8)) divided by same thing from previous week (difference of feature 11)."},
     112:{'name': "difference_feature_12",
             'filename': 'populate_feature_112_difference_feature_12',
             'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " (Average of (max(attempt.timestamp) - min(attempt.timestamp)) for each problem during current week) divided by same thing from previous week (difference of feature 12)."},
     201:{'name': "number_of_forum_responses",
             'filename': 'populate_feature_201_number_of_forum_responses',
             'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Number of forum responses per week (also known as CF1)."},
     202:{'name': "percentile_of_average_number_of_attempts",
             'filename': 'populate_feature_202_percentile_of_average_number_of_attempts',
             'extension':  '.py',
         'default': 0,
         'dependencies':[],
         'desc': " Each students average number of attempts during the week (feat. 9) compared with other students as a percentile."},
     203:{'name': "percent_of_average_number_of_attempts",
             'filename': 'populate_feature_203_percent_of_average_number_of_attempts',
             'extension':  '.py',
         'default': 0,
         'dependencies':[],
         'desc': " Each students average number of attempts during the week (feat. 9) compared with other students as a percent of max."},
     204:{'name': "pset_grade",
             'filename': 'populate_feature_204_pset_grade',
             'extension': '.sql',
         'default': 0,
         'dependencies':[205],
         'desc': " Number of homework problems correct in a week divided by number of homework problems in the week."},
     205:{'name': "pset_grade_over_time",
             'filename': 'populate_feature_205_pset_grade_over_time',
             'extension': '.sql',
         'default': -1,
         'dependencies':[],
         'desc': " Pset grade from current week (feature 204) - avg(pset grade from previous week)."},
     #206:{'name': "lab_grade",
    #       'filename': 'populate_feature_206_lab_grade',
    #         'extension': '.sql',
    #    'default': 0,
    #     'dependencies':[207],
    #     'desc': " Number of lab problems correct in a week divided by number of lab problems in the week."},
     #207:{'name': "lab_grade_over_time",
      #       'filename': 'populate_feature_207_lab_grade_over_time',
      #       'extension': '.sql',
      #   'default': 0,
      #   'dependencies':[],
      #   'desc': " Lab grade from current week (feature 206) - avg(lab grade from previous week)."},
     208:{'name': "attempts_correct",
             'filename': 'populate_feature_208_attempts_correct',
             'extension': '.sql',
         'default': -1,
         'dependencies':[209],
         'desc': " Number of attempts (any type) that were correct during the week."},
     209:{'name': "percent_correct_submissions",
             'filename': 'populate_feature_209_percent_correct_submissions',
             'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Percentage of total submissions that were correct (feature 208 / feature 7)."},
     210:{'name': "average_predeadline_submission_time",
             'filename': 'populate_feature_210_average_predeadline_submission_time',
             'extension': '.sql',
         'default': -1,
         'dependencies':[],
         'desc': " Average time between problem submissions and problem due date (in seconds)."},
     301:{'name': "std_hours_working",
             'filename': 'populate_feature_301_std_hours_working',
             'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Standard deviation of the hours the user produces events and collaborations. Tries to capture how regular a student is with her schedule while doing a MOOC."},
     
    #302:{'name': "time_to_react",
    #        'filename': 'populate_feature_302_time_to_react',
    #         'extension': '.sql',
    #     'default': 0,
    #     'dependencies':[],
    #     'desc': " Average time in days a student takes to react when a new resource in posted. Tried to capture how fast a student is reacting to new content."},

    303:{'name': "final_grade",
            'filename': 'populate_feature_303_final_grade',
             'extension': '.sql',
         'default': 0,
         'dependencies':[],
         'desc': " Final grade for a student."},  
           
}

def returnFeatures():
    '''
    Returns
        featureDict: feature dictionary described in the file

    '''
    return featureDict

def skippedDependencies(featuresToSkip):
    '''
    Args
        featuresToSkip: features to skip

    Returns
        sd: dependencies that are skipped as a result of left out features
    '''
    sd = set(featuresToSkip)
    for f in featuresToSkip:
        dependencies = returnFeatures()[f]['dependencies']
        for d in dependencies:
            if d not in sd:
                # sd.add(d)
                print "Trying a dependencies that might have been skipped"
    return sd

def featuresFromFeaturesToSkip(featuresToSkip):
   '''

   Args
       featuresToSkip: features to skip

   Returns
       features that are currently not included
   '''
   return sorted([f for f in returnFeatures().keys() if f not in skippedDependencies(featuresToSkip)])
