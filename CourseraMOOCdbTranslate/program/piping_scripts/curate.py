"""
Main curation file - consists of all functions needed to curate the database along with helper functions.
And for WF, include the preprocess of feature extraction in this: users_populate_dropout_week.sql
"""

import MySQLdb
import datetime
import getpass
import time
from sql_functions_curate.sql_functions import *
import csv
import os
import glob
import numpy as np
from utilities import logger
import warnings


########################################### RUN CURATION ###############################################

def curate(vars, dbName = None, userName=None, passwd=None, dbHost=None, dbPort=None,
        startDate=None):

    """
    Curates the database. Only this function needs to be run for curation.

    Args
        dbName:             Name of MySQL database \n
        userName:           Username for MySQL database \n
        passwd:             Password for MySQL database \n
        dbHost:             Host for MySQL database (e.g., 'localhost') \n
        dbPort:             Port for MySQL database (e.g., 3306) \n
        startDate:          Starting date for course data \n
    """

    connection = openSQLConnectionP(dbName, userName, passwd, dbHost, dbPort)
    connection.autocommit(True)

    ####### SQL files ########
    sql_files_to_run = [
        [
         'add_submissions_validity_column.sql',
         ['moocdb'],
         [dbName]
        ],
        [
         'problems_populate_problem_week.sql',
         ['START_DATE_PLACEHOLDER','moocdb'],
         [startDate,dbName]
        ],
        [
         'users_populate_user_last_submission_id.sql',
         ['INT(11)','moocdb'],
         ['VARCHAR(50)',dbName]
        ]

    ]
    with warnings.catch_warnings():
        #warnings.filterwarnings('ignore', 'unknown table')
        warnings.simplefilter('ignore')
        run_sql_curation_files(vars, connection,sql_files_to_run)

    ###### Python files ########

    vars['logger'].Log(vars, "Recalculating durations")
    # durations were originally calculated wrong in observed_events, this fixes that, takes a long time (~3-4 hours per db)
    modify_durations(vars, connection)
    vars['logger'].Log(vars, "done")

    vars['logger'].Log(vars, "curating submissions table")
    curate_submissions(vars,connection,dbName)
    vars['logger'].Log(vars, "done")

    vars['logger'].Log(vars, "Curating observed events table")
    curate_observed_events(vars,connection)
    vars['logger'].Log(vars, "done")

    vars['logger'].Log(vars, "Curating resource table")
    populate_resource_type(vars,connection)
    vars['logger'].Log(vars, "done")

    vars['logger'].Log(vars, "preprocess part of feature extraction")
    preprocess_sql_files_to_run = [
        [
         
         'users_populate_dropout_week.sql',
         ['START_DATE_PLACEHOLDER','moocdb'],
         [startDate,dbName]
        ]
    ]

    with warnings.catch_warnings():
        #warnings.filterwarnings('ignore', 'unknown table')
        warnings.simplefilter('ignore')
        run_sql_curation_files(vars,connection,preprocess_sql_files_to_run)

    closeSQLConnection(connection)
    vars['logger'].Log(vars, "done")


######################################## RUN SQL CURATION FILES ###############################################

def run_sql_curation_files(vars, conn,preprocessing_files):
    '''
    Runs SQL files that execute curation

    Args
        conn: connection to MySQL database \n
        preprocessing_files: list of SQL files to be run and their arguments \n
        logger: for log message
    '''
    
    for fileName, toBeReplaced, replaceBy in preprocessing_files:
        fileLocation = os.path.dirname(os.path.realpath(__file__))+'/sql_functions_curate/'+ fileName
        newFile = replaceWordsInFile(vars,fileLocation, toBeReplaced, replaceBy)
        vars['logger'].Log(vars,"executing: " + fileName)
        executeSQL(vars,conn, newFile)
        #conn.commit()
        vars['logger'].Log(vars,"done")


######################################## MODIFY DURATIONS ###############################################

def modify_durations(vars, connection,MAX_DURATION_SECONDS = 3600,DEFAULT_DURATION_SECONDS = 100,BLOCK_SIZE = 50000):
    '''#use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    Updates duration of events to DEFAULT_DURATION_SECONDS if next event is > MAX_DURATION_SECONDS away

    Args
        connection: connection to MySQL database \n
        MAX_DURATION_SECONDS: maximum duration can be before updating \n
        DEFAULT_DURATION_SECONDS: default duration when updating \n
        BLOCK_SIZE: Number of rows to update at once \n

    '''
    
    cursor = connection.cursor()

    cursor.execute('SELECT DISTINCT(user_id) FROM observed_events')
    user_ids = cursor.fetchall()
    count = 0
    begin = time.time()
    for user_id_tuple in user_ids:
        user_id = user_id_tuple[0]
        #log.log("user_id: {}".format(user_id))
        #make sure we have index on timestamp (check if index makes it faster)
        last_timestamp = datetime.datetime.fromtimestamp(0).isoformat()
        last_block = False
        while not last_block:
            cursor.execute('''SELECT observed_event_id, observed_event_timestamp
                      FROM observed_events
                      WHERE user_id = '%s'
                      AND observed_event_timestamp >= '%s'
                      ORDER BY observed_event_timestamp
                      LIMIT %s''' % (user_id, last_timestamp, BLOCK_SIZE))
            #last block, add max datetime
            rows = list(cursor.fetchall())
            if len(rows) < BLOCK_SIZE:
                rows.append(('', datetime.datetime.max))
                last_block = True
            row_value_strings = []
            query_part1 = "INSERT INTO observed_events (observed_event_id, observed_event_type_id, user_id, item_id, observed_event_timestamp, observed_event_data, observed_event_duration) VALUES "
            query_part2 = " ON DUPLICATE KEY UPDATE observed_event_duration=VALUES(observed_event_duration)"
            for i,row in enumerate(rows[:-1]):
                duration = calc_duration(vars,row[1], rows[i+1][1],MAX_DURATION_SECONDS,DEFAULT_DURATION_SECONDS)
                #-1, -1, -1, '2013-01-01', '' are all just place holder, no effects 
                row_value_strings.append("({},-1, -1, -1, '2013-01-01', '', {})".format(row[0], duration))
                #cursor.execute('''UPDATE observed_events
                #            SET observed_event_duration = '%s'
                #           WHERE observed_event_id = '%s'
                #            ''' % (duration, row[0]))
                #connection.commit()
            query = query_part1 + ",".join(row_value_strings) + query_part2 + ";"
            #vars['logger'].Log(vars,"update query: " + query)
            cur = connection.cursor(MySQLdb.cursors.DictCursor)
            cur.execute(query)
            cur.close()
            last_timestamp = rows[-1][1]
        count += 1
        if count == 50:
            vars['logger'].Log(vars,"elapsed time for 50 users: " + str(time.time() - begin))
            begin = time.time()
            count = 0
    cursor.close()

def calc_duration(vars,timestamp1, timestamp2,MAX_DURATION_SECONDS,DEFAULT_DURATION_SECONDS):
    '''
    Helper function for modify_durations that calculates the duration between two timestamps

    Args
        timestamp1: beginning timestamp \n
        timestamp2: ending timestamp \n
        MAX_DURATION_SECONDS: maximum duration can be before updating \n
        DEFAULT_DURATION_SECONDS: default duration when updating \n

    '''
       
    duration = int((timestamp2 - timestamp1).total_seconds())
    truncated_duration = duration if duration <= MAX_DURATION_SECONDS else DEFAULT_DURATION_SECONDS
    return truncated_duration


######################################## CURATE OBSERVED EVENTS ###############################################

def curate_observed_events(vars, conn,min_time = 10,BLOCK_SIZE=50):
    '''
    Updates the validity column of valid events to 1 and invalid events to 0

    Args
        connection: connection to MySQL database \n
        BLOCK_SIZE: Number of rows to update at once \n

    '''
      
    cursor = conn.cursor()

    # invalidate consecutive repeated events
    # defined as consecutive events with same timestamp
    # AND same duration AND same user_id
    select_potential_events = '''
        SELECT  e.user_id, e.observed_event_timestamp, e.observed_event_duration, e.observed_event_id
        FROM observed_events as e
        WHERE observed_event_duration >= '%s'
        ORDER BY e.user_id, e.observed_event_timestamp ASC
    ''' % (min_time)
# Do we need this?
#        INNER JOIN urls as u
#         ON u.url_id = e.url_id
    cursor.execute(select_potential_events)
    data = cursor.fetchall()
    cursor.close()
    cursor = conn.cursor()

    valid_event_ids = [data[0][-1]]
    invalid_event_ids = []
    for i in range(1,len(data)):
        if events_equal(data[i], data[i-1]):
            invalid_event_ids.append(data[i][-1])
        else:
            valid_event_ids.append(data[i][-1])

    modify_valids = '''
        UPDATE observed_events
        SET validity = 1
        WHERE observed_event_id in (%s)
    '''
    # Edit - fix valid_event_ids list by casting to int
    valid_event_ids = [int(s) for s in valid_event_ids]

    block_sql_command(conn, cursor, modify_valids, valid_event_ids, BLOCK_SIZE)

    cursor.close()
    cursor = conn.cursor()

    if len(invalid_event_ids) > 0:
        modify_invalids = '''
            UPDATE observed_events
            SET validity = 0
            WHERE observed_event_id in (%s)
        '''
        # Edit - fix invalid_event_ids list by casting to int
        invalid_event_ids = [int(s) for s in invalid_event_ids]
        
        block_sql_command(conn, cursor, modify_invalids, invalid_event_ids, BLOCK_SIZE)

        cursor.close()

def events_equal(row1, row2):
    '''
    Helper function for curate_observed_events, tests if two lists pertaining to two events are equal

    Args
        row1: list to compare \n
        row2: list to compare \n

    '''
      
    # 0 = user_id
    # 1 = timestamp
    # 2 = duration
    for i in range(3):
        if row1[i] != row2[i]:
            return False
    return True



######################################## CURATE RESOURCES ###############################################

def extract_NumberEnrollments(conn):
    '''
    Extracts number of distinct user_ids

    Args
        conn: connection to MySQL database \n
    '''
       
    txt='Select count(distinct user_id) from observed_events'
    cursor = conn.cursor()
    cursor.execute(txt)
    c = cursor.fetchone()
    cursor.close()
    if c:
        c = c[0][0]
    return c

def string_contains_word(string,word):
    '''
    Test if string contains word in it
    '''
       
    l_word=len(word)
    l=len(string)
    instance=0
    i=0
    while len(string[i:l])-l_word>-1:
        if string[i:i+l_word]==word:
           instance+=1
        i+=1
    return instance>0

def extract_resource_types(vars,conn):
    '''
    Return the list of the resource_type_names in order of appearance in resource_types table

    Args
        conn: connection to MySQL database \n
    '''
     
    command='select resource_type_id,resource_type_name from resource_types;'
    cursor = conn.cursor()
    cursor.execute(command)
    c = cursor.fetchall()
    cursor.close()
    res_types=[]
    if c:
        for i in range(len(c)):
            res_types.append(c[i][1])
    return res_types

def compute_resource_type_id(res_types,url):
    '''
    Return the resource_type_id (=index in the list of resource_type_names) corresponding to a url
    and 0 if the url does not match any resource_type names
    '''
      
    result=0
    for x in res_types:
        if string_contains_word(url,x):
            result=res_types.index(x)
    return result


def populate_resource_type(vars,conn):
    '''
    Populate the resource_type_id for the resources having resource_type_id=0
    when uri matches one of the resource_type_names in the resource_types table
    '''
    
    # Extract resources types of the database
    res_types=extract_resource_types(vars, conn)

    # Fetching the resource_uri's lacking resource_type_id
    command='select resource_id,resource_uri from resources where resource_type_id=0;'
    cursor = conn.cursor()
    cursor.execute(command)
    c = cursor.fetchall()
    cursor.close()

    # Creating a list of all resource_type_id corresponding in order
    ids=[]
    if c:
        for i in range(len(c)):
            ids.append([c[i][0],compute_resource_type_id(res_types,c[i][1])])

    # Updating data base
    cur = conn.cursor()
    count=0
    for i in range(len(ids)):
        res_id=ids[i][0]
        res_type_id=ids[i][1]
        command='update resources set resource_type_id=%s where resource_id=%s' %(res_type_id,res_id)
        conn.commit()
        cur.execute(command)
        count+=1
    cursor.close()
    
    vars['logger'].Log(vars,"%s rows have been updated" %(count))

    return 0


######################################## CURATE SUBMISSIONS ###############################################

def curate_submissions(vars,conn,dbName,BLOCK_SIZE = 50):
    """
    Created: 5/24/2015 by Ben Schreck

    Curates submissions (and indirectly assessments)

    Args
        conn: connection to MySQL database \n
        dbName: name of database \n
        BLOCK_SIZE: Number of rows to update at once \n
    """

       
    cursor = conn.cursor()

    invalidate_submissions_first_pass = '''
        UPDATE `%s`.submissions
        SET validity = 0
        WHERE submission_attempt_number < 0
        OR   submission_is_submitted != 1
    ''' % (dbName)
    cursor.execute(invalidate_submissions_first_pass)
    conn.commit()
    cursor.close()
    cursor = conn.cursor()


    potential_submissions_query= '''
    SELECT  s.submission_id,
            s.user_id,
            s.problem_id,
            s.submission_timestamp,
            s.submission_answer,
            s.submission_attempt_number,
            s.submission_is_submitted,
            a.assessment_grade

    FROM `%s`.submissions AS s
    LEFT JOIN `%s`.assessments AS a
    ON s.submission_id = a.submission_id
    WHERE s.submission_attempt_number > -1
    AND   s.submission_is_submitted = 1
    ORDER BY s.user_id,
             s.problem_id,
             s.submission_attempt_number,
             s.submission_timestamp
             ASC
    ''' % (dbName, dbName)
    cursor.execute(potential_submissions_query)
    data = cursor.fetchall()
    cursor.close()
    cursor = conn.cursor()


    submission_id, user_id, problem_id, timestamp, \
    answer, attempt_number, is_submitted, grade = data[0]


    invalid_submissions= []

    valid_submissions = {user_id: {
                                problem_id: [(submission_id,answer,attempt_number,grade,timestamp)]
                        }}

    for i in range(1,len(data)):
        submission_id, user_id, problem_id, timestamp, \
        answer, attempt_number, is_submitted, grade = data[i]


        if user_id in valid_submissions:
            if problem_id in valid_submissions[user_id]:
                subs = valid_submissions[user_id][problem_id]
                # correct answer
                correct = [1 for x in subs if x[3] == 1]
                #if there is a correct answer already
                #don't include our current submission
                current_is_valid = True
                if len(correct) == 0:
                    current_is_valid = False
                #if our current submission is a duplicate,
                #don't include it
                #duplicate means user_id, problem_id, timestamps identical
                #if current submission is same answer as previous, don't
                #include it

                #previous submission has same answer
                if subs[-1][1] == answer:
                    current_is_valid = False
                if current_is_valid:
                    for sub in subs:
                        #timestamps identical
                        if sub[-1] == timestamp:
                            current_is_valid = False
                if current_is_valid:
                    #user_id, problem_id, timestamp identical
                    valid_submissions[user_id][problem_id].append((submission_id,answer,attempt_number,grade,timestamp))
                else:
                    invalid_submissions.append(submission_id)
            else:
                valid_submissions[user_id][problem_id] = [(submission_id,answer,attempt_number,grade,timestamp)]
        else:
            valid_submissions[user_id] = {problem_id: [(submission_id,answer,attempt_number,grade,timestamp)]}


    # Modify invalid submissions in sql
    modify_invalids = '''
        UPDATE submissions
        SET validity = 0
        WHERE submission_id in (%s)'''
    # Edit - fix invalid_submissions list by casting to int
    invalid_submissions = [int(s) for s in invalid_submissions]

    block_sql_command(conn, cursor, modify_invalids, invalid_submissions,BLOCK_SIZE)

    cursor.close()
    cursor = conn.cursor()

    # Modify valid submissions in sql
    valid_submission_ids = []
    for user_id in valid_submissions:
        for problem_id in valid_submissions[user_id]:
            for sub in valid_submissions[user_id][problem_id]:
                valid_submission_ids.append(sub[0])

    modify_valids = '''
        UPDATE submissions
        SET validity = 1
        WHERE submission_id in (%s)
    '''
    # Edit - fix valid_submission_ids list by casting to int
    valid_submission_ids = [int(s) for s in valid_submission_ids]
    
    block_sql_command(conn, cursor, modify_valids, valid_submission_ids,BLOCK_SIZE)

    cursor.close()


