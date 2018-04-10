'''
Created on Nov 21, 2013
@author: Colin Taylor colin2328@gmail.com
Feature 202- A student's average number of attempts as compared with other students as a percentile
Requires that populate_feature_9_average_number_of_attempts.sql has already been run!
'''

from sql_functions_feature_extract.sql_functions import *
from  scipy.stats import percentileofscore
BLOCK_SIZE=1000

def main(conn, conn2, dbName, startDate, currentDate, numWeeks, featureExtractionId, parent_conn = None):
    #numWeeks doesn't do anything here, but python scripts are automatically
    #called so we need the arg
    cursor = conn.cursor()


    sql = '''SELECT user_id, longitudinal_feature_week,longitudinal_feature_value
          FROM `%s`.user_longitudinal_feature_values
          WHERE longitudinal_feature_id = 9
          AND feature_extraction_id = %s
          ''' % (dbName, featureExtractionId)

    cursor.execute(sql)

    week_values = {}
    data = []
    for [user_id, week, value] in cursor:
        data.append((user_id, week, value))
        if week in week_values:
            week_values[week].append(value)
        else:
            week_values[week] = [value]


    data_to_insert = []
    for i, [user_id, week, value] in enumerate(data):
        data_to_insert.append((featureExtractionId, user_id, week,
            percentileofscore(week_values[week], value),currentDate))
    cursor.close()

    sql = "INSERT INTO `%s`.user_longitudinal_feature_values(longitudinal_feature_id, feature_extraction_id, user_id," % dbName

    sql = sql + '''
                longitudinal_feature_week,
                longitudinal_feature_value,
                date_of_extraction)
                VALUES (202, %s, %s, %s, %s, %s)
                '''

    cursor = conn.cursor()
    block_sql_command(conn, cursor, sql, data_to_insert, BLOCK_SIZE)
    cursor.close()
    conn.commit()
    
    if parent_conn:
        parent_conn.send(True)
    return True

def main_windows(host, port, userName, passwd, dbName,startDate, currentDate, numWeeks, featureExtractionId, parent_conn = None):
    ''' Used by windows platform
    '''
    conn = MySQLdb.connect(host=host, port=port, user=userName, passwd=passwd, db=dbName, cursorclass=cursors.SSCursor)
    resultMain = main(conn, conn, dbName,startDate, currentDate, numWeeks, featureExtractionId, parent_conn = None)
    conn.close()
    return resultMain
