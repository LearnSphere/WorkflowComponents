'''
Created on July 02, 2013

@author: Colin for ALFA, MIT lab: colin2328@gmail.com
Feature 13- Variance of a students observed event timestamps in one week

Modifications:
 2013-07-04 - Franck Dernoncourt - franck.dernoncourt@gmail.com - fixed a few typos + add a TODO which needs to be fixed


 Takes 275 Seconds to run

'''

import MySQLdb as mdb
import math
import os
import time as t
from sql_functions_feature_extract.sql_functions import *
#make this as high as possible until MySQL quits on you
BLOCK_SIZE = 1000


def main(conn, conn2, dbName, startDate, currentDate, numWeeks, featureExtractionId, parent_conn = None):
    cursor2 = conn2.cursor()

    first_row = '''SELECT observed_events.user_id,
             FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp) -
             UNIX_TIMESTAMP('%s')) / (3600 * 24 * 7))
             AS week, observed_event_timestamp
             FROM `%s`.observed_events AS observed_events
             INNER JOIN `%s`.users as u
             ON u.user_id = observed_events.user_id
             WHERE
             u.user_dropout_week IS NOT NULL
             AND
             observed_events.validity = 1
             AND FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp)
                - UNIX_TIMESTAMP('%s')) / (3600 * 24 * 7)) < '%s'
             GROUP BY observed_events.user_id, week, observed_event_timestamp
             ASC LIMIT 1
          ''' % (startDate, dbName, dbName, startDate, numWeeks)

    cursor2.execute(first_row)
    first = cursor2.fetchone()
    cursor2.close()
    if first is None or len(first)==0:
        raise  ValueError('Feature 13 returnes an empty result set.', 'startDate:'+startDate, 'numWeeks:'+str(numWeeks)) 
        return False
    times = []
    old_week = first[1]
    old_user_id = first[0]



    cursor = conn.cursor()

    sql = '''SELECT COUNT(*)
            FROM `%s`.observed_events AS observed_events''' % dbName
    cursor.execute(sql)
    n = int(cursor.fetchone()[0])
    cursor.close()

    cursor = conn.cursor()

    #get all the observed events times for a user for a week
    print "Executing query (get all the observed events times for a user for a week)"
    sql = '''SELECT observed_events.user_id,
             FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp) -
             UNIX_TIMESTAMP('%s')) / (3600 * 24 * 7))
             AS week, observed_event_timestamp
             FROM `%s`.observed_events AS observed_events
             INNER JOIN `%s`.users as u
             ON u.user_id = observed_events.user_id
             WHERE
             u.user_dropout_week IS NOT NULL
             AND
             observed_events.validity = 1
             AND FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp)
                - UNIX_TIMESTAMP('%s')) / (3600 * 24 * 7)) < '%s'
             GROUP BY observed_events.user_id, week, observed_event_timestamp
             ASC
          ''' % (startDate, dbName, dbName, startDate, numWeeks)




    cursor.execute(sql)

    #print "Starting parsing results"

    data_to_insert = []

    used_index = 0
    #print "N = ", n
    for i in xrange(n):
        row = cursor.fetchone()
        if not row:
            break
        user_id = row[0]
        week = row[1]
        timestamp = row[2]

        if (week != old_week or user_id != old_user_id):
            #if either have changed, compute entropy for list, insert entropy into database, and clear list before adding
            entropy = compute_deviation(times)
            data_to_insert.append((featureExtractionId,user_id,week,entropy,currentDate))
            times = []
            used_index += 1

        time = timestamp.time()
        seconds = ((time.hour * 60 + time.minute) * 60) + time.second
        times.append(seconds)
        old_week = week
        old_user_id = user_id

    cursor.close()


    #print "Inserting new feature"


    sql = "INSERT INTO `%s`.user_longitudinal_feature_values(longitudinal_feature_id," % dbName
    sql = sql+ '''
        feature_extraction_id,
        user_id,
        longitudinal_feature_week,
        longitudinal_feature_value,
        date_of_extraction)
        VALUES (13, %s, %s, %s, %s, %s)
        '''
    cursor = conn.cursor()
    block_sql_command(conn, cursor, sql, data_to_insert, BLOCK_SIZE)
    cursor.close()
    conn.commit()

    if parent_conn:
        parent_conn.send(True)
    return True


def main_windows(host, port, userName, passwd, dbName, startDate, currentDate, numWeeks, featureExtractionId, parent_conn = None):
    ''' Used by windows platform
    '''
    conn = MySQLdb.connect(host=host, port=port, user=userName, passwd=passwd, db=dbName, cursorclass=cursors.SSCursor)
    resultMain = main(conn, conn, dbName, startDate, currentDate, numWeeks, featureExtractionId, parent_conn = None)
    conn.close()
    return resultMain


def compute_deviation(times):
    mean = sum(times, 0.0) / len(times)
    d = [(i - mean) ** 2 for i in times]
    std_dev = math.sqrt(sum(d) / len(d))
    return std_dev
