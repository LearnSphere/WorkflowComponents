import featuretools as ft
import pandas as pd
import csv
import os
import sys
import datetime
import dateutil
import time
import pickle
import operator

def generate_moocdb_features(moocdb_es):

    #test variables
    '''
    tsv_file_locations = "/mnt/groups/dai-group/class_dbs/203x_2013_3t/"
    class_name = "203x_2013_3t"
    class_start_date = "Oct 28 2013"
    class_end_date = "Dec 15 2013"
    '''

    print "Generating features for " + moocdb_es.id

    # FIND THE MAX AND MIN DATE VALUES for class_start_date | class_end_date using observed_events
    observed_event_timestamps = moocdb_es['observed_events']['observed_event_timestamp'].series
    print "Observed event? ", observed_event_timestamps
    
    class_start_date = min(observed_event_timestamps)
    class_end_date = max(observed_event_timestamps)

    print "class_start_date? ", class_start_date
    print "class_end_date? ", class_end_date
    
    print "Generating weekly cutoff times for each user."

    #Create cutoff times (big list of tuples, (user_id) x (timestamp for week #) for every user_id and timestamp pair)
    start_datetime = class_start_date #dateutil.parser.parse(class_start_date)
    end_datetime = class_end_date #dateutil.parser.parse(class_end_date)
    weekly_timestamps = []

    curr_datetime = start_datetime
    while curr_datetime < end_datetime:
        curr_datetime += datetime.timedelta(days=7)
        weekly_timestamps.append(str(curr_datetime)) #intentionally ignore first week, as no data by then
    weekly_timestamps.append(str(end_datetime))
    
    cutoff_times = []
    for user in moocdb_es['users']['user_id'].series:
        for timestamp in weekly_timestamps:
            cutoff_times.append((user, timestamp))

    cutoff_times = pd.DataFrame.from_records(data=cutoff_times, columns=['user_id', 'time'])

    print "Generating deep features. This step may take a very long time..."
    time_start = time.time()
    feature_matrix, feature_defs = ft.dfs(entityset=moocdb_es, target_entity="users", cutoff_time=cutoff_times)
    total_time = time.time() - time_start
    print "DFS complete. Total time elapsed: " + str(total_time/60.0)
    return feature_matrix, feature_defs
