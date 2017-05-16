import argparse
from datetime import datetime
from main import *
from helperFuncs import *
import curate as cu
import ConfigParser
import platform

#run this as test on command line
#python run_coursera.py -courseName game_theory_gametheory003 -MOOCdbName moocdb_game_theory_gametheory003

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Coursera to MOOCdb Translator.')
    parser.add_argument('-programDir', type=str, help='the component program directory', default="..")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument('-courseName', type=str, help='Coursera course name')
    parser.add_argument('-MOOCdbName', type=str, help='MOOCdbName name')
    #parser.add_argument('-userName', type=str, help='user name to access db)')
    #parser.add_argument('-password', type=str, help='password to access db)')
    #parser.add_argument('-dbHost', type=str, help='host name to access mooc-db', default="127.0.0.1")
    #parser.add_argument('-dbPort', type=str, help='host port number to access mooc-db', default="3306")
    parser.add_argument('-customMOOCdbName', type=str, help='place holder, no use', default="")
    parser.add_argument('-file0', type=str, help='place holder for WF, no use', default="")
    parser.add_argument('-file1', type=str, help='place holder for WF, no use', default="")
    parser.add_argument('-file2', type=str, help='place holder for WF, no use', default="")
    
    args = parser.parse_args()
    #read config file
    config = ConfigParser.RawConfigParser()
    if args.programDir == "..":
        config.read(args.programDir + '/ConfigFile.properties')
    else:
        config.read(args.programDir + '/program/ConfigFile.properties')
    userName = config.get('database', 'userName');
    password = config.get('database', 'password');
    dbHost = config.get('database', 'dbHost');
    dbPort = config.get('database', 'dbPort');
    
    dbPort = int(dbPort)
    
    vars = {
        'source': {
            'platform_format': 'coursera_2',
            #course_id is not really used
            'course_id': args.courseName,
            'course_url_id': args.courseName,
            'host': dbHost,
            'user': userName,
            'password': password,
            'port': dbPort,
            'hash_mapping_db': args.MOOCdbName + '_hash_mapping',
            'general_db': args.MOOCdbName + '_anonymized_general',
            'forum_db': args.MOOCdbName + '_anonymized_forum',
        },
    
        'core': {
            'host': dbHost,
            'user': userName,
            'password': password,
            'port': dbPort,
        },
    
        'target': {
            'host': dbHost,
            'user': userName,
            'password': password,
            'port': dbPort,
            'db': args.MOOCdbName,
        },
    
        'options': {
            'log_path': args.workingDir,
            'log_to_console': True,
            'debug': False,
            'num_users_debug_mode': 200,
        },
    }
    start_dt = datetime.now()
    vars['task_id'] = "{0:04d}{1:02d}{2:02d}{3:02d}{4:02d}{5:02d}-{6}".format(start_dt.year, start_dt.month, start_dt.day, start_dt.hour, start_dt.minute, start_dt.second, vars['target']['db'])
    vars['task_static_id'] = vars['target']['db']
    
    vars['logger'] = logger
    vars['log_file'] = vars['options']['log_path'] + "/{}.log".format(vars['task_id'])
    
    #translation
    main(vars)
    vars['logger'].Log(vars, "Finish translating")
    #add longitudinal related tables, as in the previous feature extraction's preprocess
    makeFeatureTables(vars)
    vars['logger'].Log(vars, "Finish making feature tables")
    #curate
    vars['logger'].Log(vars, "CURATING DATABASE: " + vars['target']['db'])
    #get the earliest_submission_time from submissions table for curate function
    startDate = getMinSubmissionTimestamp(vars)
    if startDate == None:
        startDate = datetime.now()
    startDate = startDate.strftime('%Y-%m-%d %H:%M:%S')
        
    cu.curate(vars,
              dbName     = vars['target']['db'],
            userName    = userName,
            passwd      = password,
            dbHost      = dbHost,
            dbPort      = dbPort,
            startDate   = startDate,
    )
    #write the new dbname to a MOOCdb text file
    outputFilePath = vars['options']['log_path'] + "/MOOCdbPointer.txt"
    outfile = open(outputFilePath, 'w')
    outfile.write( "MOOCdb_name\n")
    outfile.write( vars['target']['db']+ "\n")
    #write all feaure names to a one line output file
    featureNames = getFeatureNamesInOneLine(vars)
    print featureNames
    #write the new dbname to a MOOCdb text file
    outputFilePath = vars['options']['log_path'] + "/MOOCdbFeatures.txt"
    outfile = open(outputFilePath, 'w')
    outfile.write( featureNames + "\n")
    vars['logger'].Log(vars, "Translation successful.")
