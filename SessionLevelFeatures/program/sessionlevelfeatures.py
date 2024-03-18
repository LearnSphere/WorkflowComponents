# Dev-note: this file was put together to integrate session level indicators into LearnSphere
import pandas as pd
import numpy as np
from datetime import datetime as dt, timedelta
from pandas.errors import SettingWithCopyWarning, PerformanceWarning
import warnings
import argparse
import os
from Levenshtein import distance

# warnings.simplefilter(action="ignore", category=SettingWithCopyWarning)
# warnings.simplefilter(action="ignore", category=PerformanceWarning)
warnings.filterwarnings('ignore')


def generate_class_session_info(df_main_min_1):
    # print("Init: initial session id feature generation")
    time_threshold = 10 * 60  # 10 mins

    df_main_min_1['parsed_time'] = df_main_min_1['Time'].apply(lambda x: dt.strptime(x, '%Y-%m-%d %H:%M:%S'))
    df_main_min_1['parsed_time_sec'] = (df_main_min_1['parsed_time'] - pd.Timestamp('1970-01-01')) // pd.Timedelta('1s')
    df_main_min_1['Duration (sec)'] = pd.to_numeric(df_main_min_1['Duration (sec)'], errors='coerce')

    # df_main_min_1.drop(columns=['index', 'row'], inplace=True)

    # dev-note: checking class that can be eliminated
    #  eliminate classes with < 5 students, there is a higher chance that these could be test accounts so dropping them
    df_filter_classes = df_main_min_1.groupby(['Class', 'Anon Student Id']).size().reset_index(name='frequency')
    df_filter_class_students_count = df_filter_classes.groupby(['Class']).size().reset_index(name='student_count')
    smaller_classes = df_filter_class_students_count.loc[
        df_filter_class_students_count.student_count < 5]['Class'].unique()

    df_main_min_1 = df_main_min_1.loc[~df_main_min_1['Class'].isin(smaller_classes)]

    df_main_min_1.sort_values(by=['Class', 'parsed_time'], inplace=True)

    df_main_min_1['time_diff_class'] = df_main_min_1.groupby('Class')['parsed_time_sec'].diff()
    df_main_min_1['mask'] = df_main_min_1['time_diff_class'] > time_threshold
    df_main_min_1['session_reset_count'] = df_main_min_1.groupby('Class')['mask'].transform('cumsum').fillna(
        0).astype(
        int)
    df_main_min_1['class_session_id'] = df_main_min_1['Class'].astype(str) + "_Session_" + df_main_min_1[
        'session_reset_count'].astype(str)

    # dev-note: next let's compute the student session_ids
    df_main_min_1['session_id'] = df_main_min_1['class_session_id'] + "_" + df_main_min_1['Anon Student Id']
    df_main_min_1['time_diff'] = df_main_min_1.groupby('session_id')['parsed_time_sec'].diff()

    df_main_min_1['parsed_year'] = df_main_min_1.parsed_time.apply(lambda x: x.year)
    df_main_min_1['parsed_month'] = df_main_min_1.parsed_time.apply(lambda x: x.month)
    df_main_min_1['parsed_week'] = df_main_min_1.parsed_time.apply(lambda x: x.week)
    df_main_min_1['weekday_name'] = df_main_min_1.parsed_time.apply(lambda x: x.strftime("%A"))
    df_main_min_1['day_of_week'] = df_main_min_1.parsed_time.apply(lambda x: timedelta(x.weekday()))
    df_main_min_1['parsed_hour'] = df_main_min_1.parsed_time.apply(lambda x: x.hour)
    df_main_min_1['parsed_mins'] = df_main_min_1.parsed_time.apply(lambda x: x.minute)
    df_main_min_1['start_week_mon_3am'] = df_main_min_1.parsed_time - df_main_min_1.day_of_week
    df_main_min_1['start_week_mon_3am'] = df_main_min_1.start_week_mon_3am.apply(
        lambda x: x.replace(hour=3, minute=0, second=0, microsecond=0))

    df_main_min_1 = df_main_min_1[
        ['Transaction Id', 'School', 'Class', 'Anon Student Id', 'Duration (sec)', 'Level (Unit)', 'Level (Section)',
         'Problem Name', 'Problem Start Time', 'Step Name', 'Attempt At Step', 'Is Last Attempt', 'Help Level',
         'Outcome', 'Input', 'class_session_id', 'session_id', 'parsed_time', 'Time', 'parsed_year', 'parsed_month',
         'parsed_hour', 'start_week_mon_3am', 'parsed_time_sec']]
    df_main_min_1.fillna({"Duration (sec)": 0.00001, "Help Level": 0}, inplace=True)
    df_main_min_1['parsed_endtime_sec'] = df_main_min_1.parsed_time_sec + df_main_min_1['Duration (sec)']
    df_main_min_1.rename(columns={'session_id': 'Session Id'}, inplace=True)
    df_main_min_1.sort_values(by=['Class', 'parsed_time'], inplace=True) #, ignore_index=True)
    df_main_min_1.reset_index(drop=True, inplace=True)
    # print("Completed: initial session id feature generation")
    return df_main_min_1


def generate_class_session_info_agg_class(df_main_min_3, school_start_hour=7, school_end_hour=16, min_class_size=5):
    # df_main_min_1['parsed_time'] = df_main_min_1['Time'].apply(lambda x: dt.strptime(x, '%Y-%m-%d %H:%M:%S'))
    #df_class_sessions = df_main_min_3.groupby(['School', 'Class', 'class_session_id']).agg(
    #   session_student_count=('Anon Student Id', 'nunique'),
    #   class_session_start_hour=('parsed_hour', 'min'),
    #    class_session_end_hour=('parsed_hour', 'max'),
    #    class_session_start_timestamp=('parsed_time', 'min'),
    #    class_session_end_timestamp=('parsed_time', 'max')
    #).reset_index()
    
    df_class_sessions = df_main_min_3.groupby(['School', 'Class', 'class_session_id']).agg(
		{'Anon Student Id': [('session_student_count','nunique')],
		'parsed_hour': [('class_session_start_hour','min'), ('class_session_end_hour','max')],
		'parsed_time': [('class_session_start_timestamp','min'), ('class_session_end_timestamp','max')]}
    ).reset_index()
    df_class_sessions.columns = ["&".join(col_name).rstrip('&') for col_name in df_class_sessions.columns]
    df_class_sessions.columns = [col_name[col_name.find('&')+1:] for col_name in df_class_sessions.columns]
    df_class_sessions_classwork_ids = df_class_sessions.loc[
        (df_class_sessions['session_student_count'] >= min_class_size) &
        (df_class_sessions['class_session_start_hour'] >= school_start_hour) &
        (df_class_sessions['class_session_end_hour'] < school_end_hour)]['class_session_id'].unique()

    df_class_sessions['class_session_start_time'] = \
        df_class_sessions.class_session_start_timestamp.apply(lambda x: x.strftime('%H:%M'))

    df_class_sessions['class_session_end_time'] = \
        df_class_sessions.class_session_end_timestamp.apply(lambda x: x.strftime('%H:%M'))

    df_class_sessions['Classwork'] = 0
    df_class_sessions.loc[df_class_sessions.class_session_id.isin(df_class_sessions_classwork_ids), 'Classwork'] = 1

    df_class_sessions.sort_values(
        by=['Class', 'class_session_start_timestamp'], inplace=True)        #, ignore_index=True)
    df_class_sessions.reset_index(drop=True, inplace=True)
    return df_class_sessions


def generate_student_session_info_agg_class(df_main_min_4, df_class_sessions_agg_classes):
    df_class_sessions_agg_classes = df_class_sessions_agg_classes[['class_session_id', 'Classwork']]

    #df_student_session_agg = df_main_min_4.groupby(['School', 'Class', 'Anon Student Id', 'class_session_id', 'Session Id']).agg(
    #    student_session_start_hour=('parsed_hour', 'min'),
    #    student_session_end_hour=('parsed_hour', 'max'),
    #    student_session_start_timestamp=('parsed_time', 'min'),
    #    student_session_end_timestamp=('parsed_time', 'max')
    #).reset_index()
    
    df_student_session_agg = df_main_min_4.groupby(['School', 'Class', 'Anon Student Id', 'class_session_id', 'Session Id']).agg(
        {'parsed_hour': [('student_session_start_hour','min'), ('student_session_end_hour','max')],
        'parsed_time': [('student_session_start_timestamp','min'), ('student_session_end_timestamp','max')]}
    ).reset_index()
    df_student_session_agg.columns = ["&".join(col_name).rstrip('&') for col_name in df_student_session_agg.columns]
    df_student_session_agg.columns = [col_name[col_name.find('&')+1:] for col_name in df_student_session_agg.columns]

    df_student_session_agg['student_session_start_time'] = \
        df_student_session_agg['student_session_start_timestamp'].apply(lambda x: x.strftime('%H:%M'))
    df_student_session_agg['student_session_end_time'] = \
        df_student_session_agg.student_session_end_timestamp.apply(lambda x: x.strftime('%H:%M'))

    df_student_session_agg = df_student_session_agg.merge(
        df_class_sessions_agg_classes, on=['class_session_id'], how='left')
    df_student_session_agg.sort_values(
        by=['Class', 'student_session_start_timestamp'], inplace=True) #, ignore_index=True)
    df_student_session_agg.reset_index(drop=True, inplace=True)        
    return df_student_session_agg


def generate_session_level_features(df_main_min_2, school_start_hour=7, school_end_hour=16, min_class_size=5):
    # df_main_min_1['parsed_time'] = df_main_min_1['Time'].apply(lambda x: dt.strptime(x, '%Y-%m-%d %H:%M:%S'))
    # print([school_start_hour, school_end_hour, min_class_size])
    #df_class_sessions = df_main_min_2.groupby(['School', 'Class', 'class_session_id']).agg(
    #    session_student_count=('Anon Student Id', 'nunique'),
    #    class_session_start_hour=('parsed_hour', 'min'),
    #    class_session_end_hour=('parsed_hour', 'max')
    #).reset_index()
    
    df_class_sessions = df_main_min_2.groupby(['School', 'Class', 'class_session_id']).agg(
        {'Anon Student Id': [('session_student_count','nunique')],
        'parsed_hour': [('class_session_start_hour','min'), ('class_session_end_hour','max')]}
    ).reset_index()
    df_class_sessions.columns = ["&".join(col_name).rstrip('&') for col_name in df_class_sessions.columns]
    df_class_sessions.columns = [col_name[col_name.find('&')+1:] for col_name in df_class_sessions.columns]


    # dev-note: identify the classwork sessions
    df_classwork_sessions = df_class_sessions.loc[
        (df_class_sessions.session_student_count >= min_class_size) &
        (df_class_sessions.class_session_start_hour >= school_start_hour) &
        (df_class_sessions.class_session_end_hour < school_end_hour)]

    # dev-note: let's generate the session time related features
    df_main_classworks = df_main_min_2.loc[
        df_main_min_2.class_session_id.isin(df_classwork_sessions.class_session_id.unique())]

    # dev-note: first calculate student session stats
    #df_classwork_student_session_stats = df_main_classworks.groupby(
    #    ['Class', 'class_session_id', 'Session Id', 'Anon Student Id']).agg(
    #    student_session_start_time=('parsed_time_sec', 'min'),
    #    student_session_end_time=('parsed_time_sec', 'max')
    #).reset_index()
    
    df_classwork_student_session_stats = df_main_classworks.groupby(
        ['Class', 'class_session_id', 'Session Id', 'Anon Student Id']).agg(
        {'parsed_time_sec': [('student_session_start_time','min'), ('student_session_end_time','max')]}
    ).reset_index()
    df_classwork_student_session_stats.columns = ["&".join(col_name).rstrip('&') for col_name in df_classwork_student_session_stats.columns]
    df_classwork_student_session_stats.columns = [col_name[col_name.find('&')+1:] for col_name in df_classwork_student_session_stats.columns]


    df_classwork_student_session_stats['student_session_length_sec'] = \
        df_classwork_student_session_stats.student_session_end_time - \
        df_classwork_student_session_stats.student_session_start_time

    # dev-note: now let's calculate class session stats
    #df_classwork_class_session_stats_1 = df_classwork_student_session_stats.groupby(
    #    ['Class', 'class_session_id']).agg(
    #    class_session_start_time=('student_session_start_time', 'min'),
    #    class_session_end_time=('student_session_end_time', 'max'),
    #    mean_student_session_length=('student_session_length_sec', 'mean'),
    #    std_student_session_length=('student_session_length_sec', 'std')
    #).reset_index()
    
    df_classwork_class_session_stats_1 = df_classwork_student_session_stats.groupby(
        ['Class', 'class_session_id']).agg(
        {'student_session_start_time': [('class_session_start_time','min')],
		'student_session_end_time': [('class_session_end_time','max')],
        'student_session_length_sec': [('mean_student_session_length','mean'), ('std_student_session_length','std')]}
    ).reset_index()
    df_classwork_class_session_stats_1.columns = ["&".join(col_name).rstrip('&') for col_name in df_classwork_class_session_stats_1.columns]
    df_classwork_class_session_stats_1.columns = [col_name[col_name.find('&')+1:] for col_name in df_classwork_class_session_stats_1.columns]


    df_classwork_class_session_stats_1['class_session_length_sec'] = \
        df_classwork_class_session_stats_1.class_session_end_time - \
        df_classwork_class_session_stats_1.class_session_start_time

    # dev-note: join the class and student session stats
    df_classwork_student_session_stats = df_classwork_student_session_stats.merge(
        df_classwork_class_session_stats_1, on=['Class', 'class_session_id'], how='inner')

    # dev-note: let's compute delayed start and early stop
    df_classwork_student_session_stats['student_session_delayed_start'] = \
        df_classwork_student_session_stats['student_session_start_time'] - \
        df_classwork_student_session_stats['class_session_start_time']
    df_classwork_student_session_stats['student_session_early_stop'] = \
        df_classwork_student_session_stats['class_session_end_time'] - \
        df_classwork_student_session_stats['student_session_start_time']

    # dev-note: let's compute the mean and std of the delayed start and early stop times
    #df_classwork_class_session_stats_2 = df_classwork_student_session_stats.groupby(
    #   ['class_session_id']).agg(
    #    mean_student_session_delayed_start=('student_session_delayed_start', 'mean'),
    #    std_student_session_delayed_start=('student_session_delayed_start', 'std'),
    #    mean_student_session_early_stop=('student_session_early_stop', 'mean'),
    #    std_student_session_early_stop=('student_session_early_stop', 'std')
    #).reset_index()
    
    df_classwork_class_session_stats_2 = df_classwork_student_session_stats.groupby(
        ['class_session_id']).agg({
        'student_session_delayed_start': [('mean_student_session_delayed_start','mean'), ('std_student_session_delayed_start','std')],
		'student_session_early_stop': [('mean_student_session_early_stop','mean'), ('std_student_session_early_stop','std')]}
    ).reset_index()
    df_classwork_class_session_stats_2.columns = ["&".join(col_name).rstrip('&') for col_name in df_classwork_class_session_stats_2.columns]
    df_classwork_class_session_stats_2.columns = [col_name[col_name.find('&')+1:] for col_name in df_classwork_class_session_stats_2.columns]


    # dev-note: connect with the main df
    df_classwork_student_session_stats = df_classwork_student_session_stats.merge(
        df_classwork_class_session_stats_2, on=['class_session_id'], how='inner')

    # dev-note: compute the relative scores
    df_classwork_student_session_stats['relative_student_session_length'] = \
        (df_classwork_student_session_stats['student_session_length_sec'] -
         df_classwork_student_session_stats['mean_student_session_length']) / \
        df_classwork_student_session_stats['std_student_session_length']

    df_classwork_student_session_stats['relative_student_session_delayed_start'] = \
        (df_classwork_student_session_stats['student_session_delayed_start'] -
         df_classwork_student_session_stats['mean_student_session_delayed_start']) / \
        df_classwork_student_session_stats['std_student_session_delayed_start']

    df_classwork_student_session_stats['relative_student_session_early_stop'] = \
        (df_classwork_student_session_stats['student_session_early_stop'] -
         df_classwork_student_session_stats['mean_student_session_early_stop']) / \
        df_classwork_student_session_stats['std_student_session_early_stop']

    #df_classwork_student_session_agg = df_classwork_student_session_stats.groupby(
    #    ['Anon Student Id']).agg(
    #    avg_student_session_length_sec=('student_session_length_sec', 'mean'),
    #    avg_student_session_delayed_start=('student_session_delayed_start', 'mean'),
    #    avg_student_session_early_stop=('student_session_early_stop', 'mean'),
    #    avg_relative_student_session_length=('relative_student_session_length', 'mean'),
    #    avg_relative_student_session_delayed_start=('relative_student_session_delayed_start', 'mean'),
    #    avg_relative_student_session_early_stop=('relative_student_session_early_stop', 'mean')
    #).reset_index()
    
    df_classwork_student_session_agg = df_classwork_student_session_stats.groupby(
        ['Anon Student Id']).agg(
        {'student_session_length_sec': [('avg_student_session_length_sec','mean')], 
        'student_session_delayed_start': [('avg_student_session_delayed_start','mean')], 
        'student_session_early_stop': [('avg_student_session_early_stop','mean')], 
        'relative_student_session_length': [('avg_relative_student_session_length','mean')], 
        'relative_student_session_delayed_start': [('avg_relative_student_session_delayed_start','mean')],
        'relative_student_session_early_stop': [('avg_relative_student_session_early_stop','mean')]}
    ).reset_index()
    df_classwork_student_session_agg.columns = ["&".join(col_name).rstrip('&') for col_name in df_classwork_student_session_agg.columns]
    df_classwork_student_session_agg.columns = [col_name[col_name.find('&')+1:] for col_name in df_classwork_student_session_agg.columns]


    # dev-note: compute student time use ratio
    #  i.e. (total time in class)/(general total time)
    #df_student_timeontask_stats = df_main_min_2.groupby(
    #    ['School', 'Class', 'class_session_id', 'Anon Student Id', 'Session Id']).agg(
    #    student_session_start_time=('parsed_time_sec', 'min'),
    #    student_session_end_time=('parsed_time_sec', 'max')
    #).reset_index()
    
    df_student_timeontask_stats = df_main_min_2.groupby(
        ['School', 'Class', 'class_session_id', 'Anon Student Id', 'Session Id']).agg(
        {'parsed_time_sec': [('student_session_start_time','min'), ('student_session_end_time','max')]}
    ).reset_index()
    df_student_timeontask_stats.columns = ["&".join(col_name).rstrip('&') for col_name in df_student_timeontask_stats.columns]
    df_student_timeontask_stats.columns = [col_name[col_name.find('&')+1:] for col_name in df_student_timeontask_stats.columns]


    df_student_timeontask_stats['student_session_length'] = \
        df_student_timeontask_stats['student_session_end_time'] - \
        df_student_timeontask_stats['student_session_start_time']

    #df_student_timeontask_agg = df_student_timeontask_stats.groupby(
    #    ['Class', 'Anon Student Id']).agg(
    #    total_timeontask=('student_session_length', 'sum')
    #).reset_index()
    
    df_student_timeontask_agg = df_student_timeontask_stats.groupby(
        ['Class', 'Anon Student Id']).agg(
        {'student_session_length': [('total_timeontask','sum')]}
    ).reset_index()
    df_student_timeontask_agg.columns = ["&".join(col_name).rstrip('&') for col_name in df_student_timeontask_agg.columns]
    df_student_timeontask_agg.columns = [col_name[col_name.find('&')+1:] for col_name in df_student_timeontask_agg.columns]


    #df_student_classwork_time_stats = df_classwork_class_session_stats_1.groupby(
    #    ['Class']).agg(
    #   total_classwork_time=('class_session_length_sec', 'sum')
    #).reset_index()
    
    df_student_classwork_time_stats = df_classwork_class_session_stats_1.groupby(
        ['Class']).agg(
        {'class_session_length_sec': [('total_classwork_time','sum')]}
    ).reset_index()
    df_student_classwork_time_stats.columns = ["&".join(col_name).rstrip('&') for col_name in df_student_classwork_time_stats.columns]
    df_student_classwork_time_stats.columns = [col_name[col_name.find('&')+1:] for col_name in df_student_classwork_time_stats.columns]

    
    df_student_timeontask_agg = df_student_timeontask_agg.merge(
        df_student_classwork_time_stats, on=['Class'], how='inner')

    df_student_timeontask_agg['student_time_use_ratio'] = \
        df_student_timeontask_agg['total_timeontask'] / \
        df_student_timeontask_agg['total_classwork_time']

    df_classwork_student_session_agg = df_classwork_student_session_agg.merge(
        df_student_timeontask_agg, on=['Anon Student Id'], how='inner')

    return df_classwork_student_session_agg


def generate_rulebased_gaming_features(df_main_min_5):
    # print("Init: rule based gaming behavior feature generation")
    is_bug = ["BUG"]
    is_correct = ["OK", "OK_AMBIGUOUS"]
    is_error = ["ERROR", "BUG"]
    is_hint = ["INITIAL_HINT", "HINT_LEVEL_CHANGE"]

    # Semantic Labels
    no_think_help_col = "Did not think before help"
    think_help_col = "Thought before help"
    read_help_col = "Read help messages"
    scan_help_col = "Scanning help messages"
    find_bottom_help_col = "Searching for bottom out hint"
    think_try_col = "Thought before attempt"
    plan_ahead_col = "Planned ahead"
    guess_col = "Guessed"
    no_success_try_col = "Unsuccessful but sincere attempt"
    guess_bug_col = "Guess with values from problem"
    read_error_col = "Read error message"
    no_read_error_col = "Did not read error message"
    thought_error_col = "Thought about error message"
    same_ans_diff_context_col = "Same answer but different context"
    similar_ans_col = "Answer is similar to last answer"
    switch_context_right_col = "Switched contexts before answering right"
    same_context_col = "Same context as last action"
    repeat_step_col = "Answer and context same as last action"
    diff_ans_diff_context_col = "Answer or context are not the same"

    df_main_min_5['prev_input'] = df_main_min_5.groupby(['Session Id']).Input.shift(1)
    df_main_min_5['prev_problem_name'] = df_main_min_5.groupby(['Session Id'])['Problem Name'].shift(1)
    df_main_min_5['prev_step_name'] = df_main_min_5.groupby(['Session Id'])['Step Name'].shift(1)
    df_main_min_5['prev_duration_sec'] = df_main_min_5.groupby(['Session Id'])['Duration (sec)'].shift(1)
    df_main_min_5['prev_outcome'] = df_main_min_5.groupby(['Session Id'])['Outcome'].shift(1)

    # dev-note:
    #  did not think before help
    #  pause <= 5 seconds before help req
    threshold_help_req = 5

    df_main_min_5['did_not_think_before_help_req'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] <= threshold_help_req) & (df_main_min_5['Outcome'].isin(is_hint))),
                    'did_not_think_before_help_req'] = True

    # dev-note:
    #  thought before help
    #  pause > 5 seconds before help req
    df_main_min_5['thought_before_help_req'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] > threshold_help_req) & (df_main_min_5['Outcome'].isin(is_hint))),
                    'thought_before_help_req'] = True

    # dev-note:
    #  read help message
    #  pause >= 9 seconds before help req
    threshold_help_req_2 = 9
    df_main_min_5['read_help_message'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] >= threshold_help_req_2) & (df_main_min_5.prev_outcome.isin(is_hint))),
                    'read_help_message'] = True

    # dev-note:
    #  scanning help message
    #  pause >= 4 and pause < 9 seconds before help req
    threshold_scan_help_req_upper = 9
    threshold_scan_help_req_lower = 4
    df_main_min_5['scanning_help_message'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] >= threshold_scan_help_req_lower) &
                       (df_main_min_5['Duration (sec)'] < threshold_scan_help_req_upper) &
                       (df_main_min_5.prev_outcome.isin(is_hint))),
                    'scanning_help_message'] = True

    # dev-note:
    #  searching for bottom out hint
    #  pause < 4 seconds before help req
    df_main_min_5['searching_for_bottom_out_hint'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] < threshold_scan_help_req_lower) &
                       (df_main_min_5.prev_outcome.isin(is_hint))),
                    'searching_for_bottom_out_hint'] = True

    # dev-note:
    #  thought before attempt
    #  pause >= 6 before attempt
    threshold_attempt_1 = 6
    df_main_min_5['thought_before_attempt'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] >= threshold_attempt_1) &
                       (df_main_min_5['Outcome'].isin(is_correct + is_error))),
                    'thought_before_attempt'] = True

    # dev-note:
    #  planned ahead
    #  pause >= 11 for a correct attempt in the previous step
    threshold_attempt_2 = 11
    df_main_min_5['planned_ahead'] = False
    df_main_min_5.loc[((df_main_min_5.prev_duration_sec >= threshold_attempt_2) &
                       (df_main_min_5.prev_outcome.isin(is_correct))),
                    'planned_ahead'] = True

    # dev-note:
    #  guessed
    #  pause < 6 seconds before step attempt
    df_main_min_5['guessed'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] < threshold_attempt_1) &
                       (df_main_min_5['Outcome'].isin(is_correct + is_error))),
                    'guessed'] = True

    # dev-note:
    #  unsuccessful but sincere
    #  pause >= 6 seconds before step attempt
    df_main_min_5['unsuccessful_but_sincere'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] >= threshold_attempt_1) &
                       (df_main_min_5['Outcome'].isin(is_error))),
                    'unsuccessful_but_sincere'] = True

    # dev-note:
    #  guessing with values from problem
    #  pause <= 6 seconds before bug
    df_main_min_5['guessing_with_values_from_poblem'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] < threshold_attempt_1) &
                       (df_main_min_5['Outcome'].isin(is_bug))),
                    'guessing_with_values_from_poblem'] = True

    # dev-note:
    #  did not read error message
    #  pause < 9 after bug in previous outcome
    threshold_attempt_3 = 9
    df_main_min_5['did_not_read_error_msg'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] < threshold_attempt_3) &
                       (df_main_min_5.prev_outcome.isin(is_bug))),
                      #  What is the difference between is_error and is_bug?
                      #  on the tool and conceptually
                    'did_not_read_error_msg'] = True

    # dev-note:
    #  thought about error
    #  pause >= 6 after incorrect attempt
    df_main_min_5['thought_about_error'] = False
    df_main_min_5.loc[((df_main_min_5['Duration (sec)'] >= threshold_attempt_1) &
                       (df_main_min_5.prev_outcome.isin(is_error))),
                    'thought_about_error'] = True

    # dev-note:
    #  same answer different context
    #  answer was the same as the previous action but in a diff context
    df_main_min_5['same_answer_diff_context'] = False
    df_main_min_5.loc[((df_main_min_5.prev_input == df_main_min_5.Input) &
                       ((df_main_min_5.prev_problem_name != df_main_min_5['Problem Name']) |
                        (df_main_min_5.prev_step_name != df_main_min_5['Step Name']))),
                    'same_answer_diff_context'] = True

    # dev-note:
    #  similar answer
    #  Answer was similar to the prev action (Levenshtein distance of 1 or 2)

    # Levenstein distance threshold for similarity
    dist_thres = 2
    df_main_min_5.Input.fillna("", inplace=True)
    df_main_min_5.prev_input.fillna("", inplace=True)

    df_main_min_5['similarity_distance'] = df_main_min_5.apply(
        lambda x: distance(str(x['Input']), str(x['prev_input'])), axis=1)

    df_main_min_5['similar_answer'] = False
    df_main_min_5.loc[(df_main_min_5.similarity_distance <= dist_thres), 'similar_answer'] = True

    # dev-note:
    #  switched context before right(correct)
    #  the context of the current action doesn't match the context of the previous action
    #  but the previous attempt was incorrect
    df_main_min_5['switched_context_before_correct'] = False
    df_main_min_5.loc[((df_main_min_5.prev_outcome.isin(is_error)) &
                       ((df_main_min_5.prev_problem_name != df_main_min_5['Problem Name']) |
                        (df_main_min_5.prev_step_name != df_main_min_5['Step Name']))),
                    'switched_context_before_correct'] = True

    # dev-note:
    #  same context
    #  context of the current action is the same as the previous action
    df_main_min_5['same_context'] = False
    df_main_min_5.loc[((df_main_min_5.prev_problem_name == df_main_min_5['Problem Name']) |
                       (df_main_min_5.prev_step_name == df_main_min_5['Step Name'])),
                    'same_context'] = True

    # dev-note:
    #  Repeated Step
    #  Answer and context of the current action are the same as the previous action
    df_main_min_5['repeated_step'] = False
    df_main_min_5.loc[(((df_main_min_5.prev_problem_name == df_main_min_5['Problem Name']) |
                        (df_main_min_5.prev_step_name == df_main_min_5['Step Name'])) &
                       (df_main_min_5.prev_input == df_main_min_5.Input)),
                    'repeated_step'] = True

    # dev-note:
    #  diff answer or diff context
    #  Answer or context of the current action is not the same as the previous action
    df_main_min_5['diff_answer_and_or_diff_context'] = False
    df_main_min_5.loc[(((df_main_min_5.prev_problem_name != df_main_min_5['Problem Name']) |
                        (df_main_min_5.prev_step_name != df_main_min_5['Step Name'])) &
                       (df_main_min_5.prev_input != df_main_min_5.Input)),
                    'diff_answer_and_or_diff_context'] = True

    # Important: there are 13 patterns
    #   P1 - incorrect → [guess] & [same answer/diff. context] & incorrect
    #   P2 - incorrect → [similar answer] [same context] & incorrect → [similar answer] & [same context] & attempt
    #   P3 - incorrect → [similar answer] & incorrect → [same answer/diff. context] & attempt
    #   P4 - [guess] & incorrect → [guess] & [diff. answer AND/OR diff. context] & incorrect → [guess] &
    #        [diff. answer AND/OR diff. context & attempt
    #   P5 - incorrect → [similar answer] & incorrect → [guess] & attempt
    #   P6 - help & [searching for bottom-out hint] → incorrect → [similar answer] & incorrect
    #   P7 - incorrect → [same answer/diff. context] & incorrect → [switched context before correct] & attempt/help
    #   P8 - bug → [same answer/diff. context] & correct → bug
    #   P9 - incorrect → [similar answer] & incorrect → [switched context before correct] & incorrect
    #   P10 - incorrect → [switched context before correct] & incorrect → [similar answer] & incorrect
    #   P11 - incorrect → [similar answer] & incorrect → [did not think before help] & help → incorrect
    #         (with first or second answer similar to the last one)
    #   P12 - help → incorrect → incorrect → incorrect (with at least one similar answer between steps)
    #   P13 - incorrect → incorrect → incorrect → [did not think before help request] & help
    #         (at least one similar answer between steps)

    engineered_features = ['did_not_think_before_help_req', 'thought_before_help_req', 'read_help_message',
                           'scanning_help_message', 'searching_for_bottom_out_hint', 'thought_before_attempt',
                           'planned_ahead', 'guessed', 'unsuccessful_but_sincere', 'guessing_with_values_from_poblem',
                           'did_not_read_error_msg', 'thought_about_error', 'same_answer_diff_context',
                           'similarity_distance', 'similar_answer', 'switched_context_before_correct', 'same_context',
                           'repeated_step', 'diff_answer_and_or_diff_context']
    additional_target = ['Input', 'Outcome']

    # TODO: redo this in a cleaner fashion
    #  causing confusion when implementing the 13 patterns
    previous_1_step_prefix = "col_1_step_prior_"
    previous_2_step_prefix = "col_2_step_prior_"
    previous_3_step_prefix = "col_3_step_prior_"
    previous_0_2_step_prefix = "col_0_vs_2_step_prior_"
    previous_0_3_step_prefix = "col_0_vs_3_step_prior_"
    previous_1_3_step_prefix = "col_1_vs_3_step_prior_"

    df_main_min_5[
        [previous_1_step_prefix + feature for feature in engineered_features + additional_target]] = \
        df_main_min_5.groupby(
            ['Session Id'])[engineered_features + additional_target].shift(1)

    df_main_min_5[
        [previous_2_step_prefix + feature for feature in engineered_features + additional_target]] = \
        df_main_min_5.groupby(
            ['Session Id'])[engineered_features + additional_target].shift(2)

    df_main_min_5[
        [previous_3_step_prefix + feature for feature in engineered_features + additional_target]] = \
        df_main_min_5.groupby(
            ['Session Id'])[engineered_features + additional_target].shift(3)

    # also compute the Levenshtein distance between current input and col_2/3_steps_prior_inputs
    df_main_min_5[previous_1_step_prefix + additional_target[0]].fillna("", inplace=True)
    df_main_min_5[previous_2_step_prefix + additional_target[0]].fillna("", inplace=True)
    df_main_min_5[previous_3_step_prefix + additional_target[0]].fillna("", inplace=True)

    # compute similarity scores between 0 vs 2
    df_main_min_5[previous_0_2_step_prefix + "similarity_distance"] = df_main_min_5.apply(
        lambda x: distance(str(x[(additional_target[0])]),
                           str(x[(previous_2_step_prefix + additional_target[0])])), axis=1)
    df_main_min_5[previous_0_2_step_prefix + 'similar_answer'] = False
    df_main_min_5.loc[(df_main_min_5[previous_0_2_step_prefix + 'similarity_distance'] <= dist_thres),
                      previous_0_2_step_prefix + 'similar_answer'] = True

    # compute similarity scores between 0 vs 3
    df_main_min_5[previous_0_3_step_prefix + "similarity_distance"] = df_main_min_5.apply(
        lambda x: distance(str(x[(additional_target[0])]),
                           str(x[(previous_3_step_prefix + additional_target[0])])), axis=1)
    df_main_min_5[previous_0_3_step_prefix + 'similar_answer'] = False
    df_main_min_5.loc[(df_main_min_5[previous_0_3_step_prefix + 'similarity_distance'] <= dist_thres),
                      previous_0_3_step_prefix + 'similar_answer'] = True

    # compute similarity scores between 1 vs 3
    df_main_min_5[previous_1_3_step_prefix + "similarity_distance"] = df_main_min_5.apply(
        lambda x: distance(str(x[(previous_1_step_prefix + additional_target[0])]),
                           str(x[(previous_3_step_prefix + additional_target[0])])), axis=1)
    df_main_min_5[previous_1_3_step_prefix + 'similar_answer'] = False
    df_main_min_5.loc[(df_main_min_5[previous_1_3_step_prefix + 'similarity_distance'] <= dist_thres),
                      previous_1_3_step_prefix + 'similar_answer'] = True

    df_main_min_5.fillna(
        dict.fromkeys([previous_1_step_prefix + feature for feature in engineered_features], False), inplace=True)
    df_main_min_5.fillna(
        dict.fromkeys([previous_1_step_prefix + feature for feature in additional_target], ""), inplace=True)

    df_main_min_5.fillna(
        dict.fromkeys([previous_2_step_prefix + feature for feature in engineered_features], False), inplace=True)
    df_main_min_5.fillna(
        dict.fromkeys([previous_2_step_prefix + feature for feature in additional_target], ""), inplace=True)

    df_main_min_5.fillna(
        dict.fromkeys([previous_3_step_prefix + feature for feature in engineered_features], False), inplace=True)
    df_main_min_5.fillna(
        dict.fromkeys([previous_3_step_prefix + feature for feature in additional_target], ""), inplace=True)

    df_main_min_5.fillna(
        dict.fromkeys(additional_target, ""), inplace=True)

    # print("Completed: rule based gaming behavior feature generation")
    return df_main_min_5


def generate_rulebased_gaming_labels(df_main_min_6):
    # print("Init: rule based gaming behavior label generation")
    df_main_min_6['clip_index'] = df_main_min_6.groupby(['Session Id']).cumcount()
    df_main_min_6['clip_index_group'] = df_main_min_6['clip_index'].apply(lambda x: x // 5)
    df_main_min_6['index_in_clip'] = df_main_min_6['clip_index'].apply(lambda x: x % 5 + 1)

    is_bug = ["BUG"]
    is_correct = ["OK", "OK_AMBIGUOUS"]
    is_error = ["ERROR", "BUG"]
    is_hint = ["INITIAL_HINT", "HINT_LEVEL_CHANGE"]

    engineered_features = ['did_not_think_before_help_req', 'thought_before_help_req', 'read_help_message',
                           'scanning_help_message', 'searching_for_bottom_out_hint', 'thought_before_attempt',
                           'planned_ahead', 'guessed', 'unsuccessful_but_sincere', 'guessing_with_values_from_poblem',
                           'did_not_read_error_msg', 'thought_about_error', 'same_answer_diff_context',
                           'similarity_distance', 'similar_answer', 'switched_context_before_correct', 'same_context',
                           'repeated_step', 'diff_answer_and_or_diff_context']

    # Important: there are 13 patterns
    #   P1 - incorrect → [guess] & [same answer/diff. context] & incorrect
    #   P2 - incorrect → [similar answer] [same context] & incorrect → [similar answer] & [same context] & attempt
    #   P3 - incorrect → [similar answer] & incorrect → [same answer/diff. context] & attempt
    #   P4 - [guess] & incorrect → [guess] & [diff. answer AND/OR diff. context] & incorrect → [guess] &
    #        [diff. answer AND/OR diff. context & attempt
    #   P5 - incorrect → [similar answer] & incorrect → [guess] & attempt
    #   P6 - help & [searching for bottom-out hint] → incorrect → [similar answer] & incorrect
    #   P7 - incorrect → [same answer/diff. context] & incorrect → [switched context before correct] & attempt/help
    #   P8 - bug → [same answer/diff. context] & correct → bug
    #   P9 - incorrect → [similar answer] & incorrect → [switched context before correct] & incorrect
    #   P10 - incorrect → [switched context before correct] & incorrect → [similar answer] & incorrect
    #   P11 - incorrect → [similar answer] & incorrect → [did not think before help] & help → incorrect
    #         (with first or second answer similar to the last one)
    #   P12 - help → incorrect → incorrect → incorrect (with at least one similar answer between steps)
    #   P13 - incorrect → incorrect → incorrect → [did not think before help request] & help
    #         (at least one similar answer between steps)

    df_main_min_6.guessed.fillna(False, inplace=True)
    df_main_min_6.same_answer_diff_context.fillna(False, inplace=True)

    previous_1_step_prefix = "col_1_step_prior_"
    previous_2_step_prefix = "col_2_step_prior_"
    previous_3_step_prefix = "col_3_step_prior_"
    previous_1_2_step_prefix = "col_1_vs_2_step_prior_"
    previous_1_3_step_prefix = "col_1_vs_3_step_prior_"
    previous_2_3_step_prefix = "col_2_vs_3_step_prior_"

    # TODO:
    #  fix the similar answer measures the similar answer measures are incorrect before P11

    # dev-note:
    #  P1 - incorrect → [guess] & [same answer/diff. context] & incorrect

    df_main_min_6['gaming_pattern_1'] = False
    df_main_min_6['gaming_pattern_1'] = ((df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.guessed &
                                         df_main_min_6.same_answer_diff_context &
                                         (df_main_min_6['Outcome'].isin(is_error)))

    # If index_in_clip is smaller than 2, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_1'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 2 else x['gaming_pattern_1'], axis=1)

    # dev-note:
    #  P2 - incorrect → [similar answer] [same context] & incorrect → [similar answer] & [same context] & attempt
    # dist_thres = 2

    df_main_min_6['gaming_pattern_2'] = False
    df_main_min_6['gaming_pattern_2'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_similar_answer &
                                         df_main_min_6.col_1_step_prior_same_context &
                                         (df_main_min_6['Outcome'].isin(is_error + is_correct)) &
                                         df_main_min_6.same_context)

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_2'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_2'], axis=1)

    # dev-note:
    #  P3 - incorrect → [similar answer] & incorrect → [same answer/diff. context] & attempt
    df_main_min_6['gaming_pattern_3'] = False
    df_main_min_6['gaming_pattern_3'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_similar_answer &
                                         (df_main_min_6['Outcome'].isin(is_error + is_correct)) &
                                         df_main_min_6.same_answer_diff_context)

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_3'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_3'], axis=1)

    # dev-note:
    #  P4 - [guess] & incorrect → [guess] & [diff. answer AND/OR diff. context] & incorrect → [guess] &
    #       [diff. answer AND/OR diff. context & attempt
    df_main_min_6['gaming_pattern_4'] = False
    df_main_min_6['gaming_pattern_4'] = (df_main_min_6.col_2_step_prior_guessed &
                                         (df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_guessed &
                                         df_main_min_6.col_1_step_prior_diff_answer_and_or_diff_context &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.guessed &
                                         df_main_min_6.diff_answer_and_or_diff_context &
                                         (df_main_min_6['Outcome'].isin(is_error + is_correct)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_4'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_4'], axis=1)

    # dev-note:
    #  P5 - incorrect → [similar answer] & incorrect → [guess] & attempt
    df_main_min_6['gaming_pattern_5'] = False
    df_main_min_6['gaming_pattern_5'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_similar_answer &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.guessed &
                                         (df_main_min_6['Outcome'].isin(is_error + is_correct)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_5'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_5'], axis=1)

    # dev-note:
    #  P6 - help & [searching for bottom-out hint] → incorrect → [similar answer] & incorrect
    df_main_min_6['gaming_pattern_6'] = False
    df_main_min_6['gaming_pattern_6'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_hint)) &
                                         df_main_min_6.col_2_step_prior_searching_for_bottom_out_hint &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.similar_answer &
                                         (df_main_min_6['Outcome'].isin(is_error + is_correct)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_6'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_6'], axis=1)

    # dev-note:
    #  P7 - incorrect → [same answer/diff. context] & incorrect → [switched context before correct] & attempt/help
    df_main_min_6['gaming_pattern_7'] = False
    df_main_min_6['gaming_pattern_7'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_same_answer_diff_context &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.switched_context_before_correct &
                                         (df_main_min_6['Outcome'].isin(is_error + is_correct + is_hint)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_7'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_7'], axis=1)

    # dev-note:
    #  P8 - bug → [same answer/diff. context] & correct → bug
    df_main_min_6['gaming_pattern_8'] = False
    df_main_min_6['gaming_pattern_8'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_bug)) &
                                         df_main_min_6.col_1_step_prior_same_answer_diff_context &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_correct)) &
                                         (df_main_min_6['Outcome'].isin(is_bug)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_8'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_8'], axis=1)

    # dev-note:
    #  P9 - incorrect → [similar answer] & incorrect → [switched context before correct] & incorrect
    df_main_min_6['gaming_pattern_9'] = False
    df_main_min_6['gaming_pattern_9'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_similar_answer &
                                         (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                         df_main_min_6.col_1_step_prior_switched_context_before_correct &
                                         (df_main_min_6['Outcome'].isin(is_error)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_9'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_9'], axis=1)

    # dev-note:
    #  P10 - incorrect → [switched context before correct] & incorrect → [similar answer] & incorrect
    df_main_min_6['gaming_pattern_10'] = False
    df_main_min_6['gaming_pattern_10'] = ((df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                          df_main_min_6.col_1_step_prior_switched_context_before_correct &
                                          (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                          df_main_min_6.similar_answer &
                                          (df_main_min_6['Outcome'].isin(is_error)))

    # If index_in_clip is smaller than 3, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_10'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 3 else x['gaming_pattern_10'], axis=1)

    # dev-note:
    #  P11 - incorrect → [similar answer] & incorrect → [did not think before help] & help → incorrect
    #         (with first or second answer similar to the last one)
    df_main_min_6['gaming_pattern_11'] = False
    df_main_min_6['gaming_pattern_11'] = ((df_main_min_6.col_3_step_prior_Outcome.isin(is_error)) &
                                          df_main_min_6.col_2_step_prior_similar_answer &
                                          (df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                          df_main_min_6.col_1_step_prior_did_not_think_before_help_req &
                                          (df_main_min_6.col_1_step_prior_Outcome.isin(is_hint)) &
                                          (df_main_min_6['Outcome'].isin(is_error)) &
                                          (df_main_min_6.col_0_vs_3_step_prior_similar_answer |
                                           df_main_min_6.col_0_vs_2_step_prior_similar_answer))

    # If index_in_clip is smaller than 4, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_11'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 4 else x['gaming_pattern_11'], axis=1)

    # dev-note:
    #  P12 - help → incorrect → incorrect → incorrect (with at least one similar answer between steps)
    df_main_min_6['gaming_pattern_12'] = False
    df_main_min_6['gaming_pattern_12'] = ((df_main_min_6.col_3_step_prior_Outcome.isin(is_hint)) &
                                          (df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                          (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                          (df_main_min_6['Outcome'].isin(is_error)) &
                                          (df_main_min_6.col_1_step_prior_similar_answer |
                                           df_main_min_6.col_0_vs_2_step_prior_similar_answer |
                                           df_main_min_6.similar_answer))

    # If index_in_clip is smaller than 4, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_12'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 4 else x['gaming_pattern_12'], axis=1)

    # dev-note:
    #  P13 - incorrect → incorrect → incorrect → [did not think before help request] & help
    #        (at least one similar answer between steps)
    df_main_min_6['gaming_pattern_13'] = False
    df_main_min_6['gaming_pattern_13'] = ((df_main_min_6.col_3_step_prior_Outcome.isin(is_error)) &
                                          (df_main_min_6.col_2_step_prior_Outcome.isin(is_error)) &
                                          (df_main_min_6.col_1_step_prior_Outcome.isin(is_error)) &
                                          (df_main_min_6['Outcome'].isin(is_hint)) &
                                          df_main_min_6.did_not_think_before_help_req &
                                          (df_main_min_6.col_1_step_prior_similar_answer |
                                           df_main_min_6.col_2_step_prior_similar_answer |
                                           df_main_min_6.col_1_vs_3_step_prior_similar_answer))

    # If index_in_clip is smaller than 4, turn the value to False, otherwise don't make any change.

    df_main_min_6['gaming_pattern_13'] = df_main_min_6.apply(
        lambda x: False if x['index_in_clip'] < 4 else x['gaming_pattern_13'], axis=1)

    df_main_min_6['is_gaming'] = (df_main_min_6['gaming_pattern_1'] | df_main_min_6['gaming_pattern_2'] |
                                  df_main_min_6['gaming_pattern_3'] | df_main_min_6['gaming_pattern_4'] |
                                  df_main_min_6['gaming_pattern_5'] | df_main_min_6['gaming_pattern_6'] |
                                  df_main_min_6['gaming_pattern_7'] | df_main_min_6['gaming_pattern_8'] |
                                  df_main_min_6['gaming_pattern_9'] | df_main_min_6['gaming_pattern_10'] |
                                  df_main_min_6['gaming_pattern_11'] | df_main_min_6['gaming_pattern_12'] |
                                  df_main_min_6['gaming_pattern_13'])

    # print(df_main_min.is_gaming.value_counts())

    # dev-note: gaming is also analyzed as an interval during which the student was in a state identified as
    #  gaming from the total session time
    # important: from what I can tell the idea here is to retroactively label the prior steps as gaming as well.
    #  This is done because while the gaming flag was triggered after the final action in the pattern occured the gaming
    #  was initiated from the first action in the sequence
    pattern_steps_involved = {
        'gaming_pattern_1': 2,
        'gaming_pattern_2': 3,
        'gaming_pattern_3': 3,
        'gaming_pattern_4': 3,
        'gaming_pattern_5': 3,
        'gaming_pattern_6': 3,
        'gaming_pattern_7': 3,
        'gaming_pattern_8': 3,
        'gaming_pattern_9': 3,
        'gaming_pattern_10': 3,
        'gaming_pattern_11': 4,
        'gaming_pattern_12': 4,
        'gaming_pattern_13': 4
    }

    ref_key_array = []
    for key, value in pattern_steps_involved.items():
        # print(key, value)
        for shift_index in range(1, value):
            ref_key_array.append(key + "_is_gaming_shifted_back_" + str(shift_index))
            df_main_min_6[key + "_is_gaming_shifted_back_" + str(shift_index)] = df_main_min_6.groupby(
                ['clip_index_group', 'Session Id'])[key].shift((-1 * shift_index))
            df_main_min_6[key + "_is_gaming_shifted_back_" + str(shift_index)].fillna(False, inplace=True)

    # important: is gaming would technically only label the final action, but they were gaming form the first action
    #  in the sequence. As such we can retroactively relabel the sequence.
    df_main_min_6['is_gaming_2'] = df_main_min_6['is_gaming']
    for ref_key in ref_key_array:
        df_main_min_6['is_gaming_2'] = (df_main_min_6['is_gaming_2'] | df_main_min_6[ref_key])

    df_main_min_6.drop(columns=ref_key_array, inplace=True)

    drop_columns_2 = [
        'prev_input', 'prev_problem_name', 'prev_step_name', 'prev_duration_sec',
        'prev_outcome', 'did_not_think_before_help_req', 'thought_before_help_req', 'read_help_message',
        'scanning_help_message', 'searching_for_bottom_out_hint', 'thought_before_attempt', 'planned_ahead', 'guessed',
        'unsuccessful_but_sincere', 'guessing_with_values_from_poblem', 'did_not_read_error_msg', 'thought_about_error',
        'same_answer_diff_context', 'similarity_distance', 'similar_answer', 'switched_context_before_correct',
        'same_context', 'repeated_step', 'diff_answer_and_or_diff_context',
        'col_1_step_prior_did_not_think_before_help_req', 'col_1_step_prior_thought_before_help_req',
        'col_1_step_prior_read_help_message', 'col_1_step_prior_scanning_help_message',
        'col_1_step_prior_searching_for_bottom_out_hint', 'col_1_step_prior_thought_before_attempt',
        'col_1_step_prior_planned_ahead', 'col_1_step_prior_guessed', 'col_1_step_prior_unsuccessful_but_sincere',
        'col_1_step_prior_guessing_with_values_from_poblem', 'col_1_step_prior_did_not_read_error_msg',
        'col_1_step_prior_thought_about_error', 'col_1_step_prior_same_answer_diff_context',
        'col_1_step_prior_similarity_distance', 'col_1_step_prior_similar_answer',
        'col_1_step_prior_switched_context_before_correct', 'col_1_step_prior_same_context',
        'col_1_step_prior_repeated_step', 'col_1_step_prior_diff_answer_and_or_diff_context', 'col_1_step_prior_Input',
        'col_1_step_prior_Outcome', 'col_2_step_prior_did_not_think_before_help_req',
        'col_2_step_prior_thought_before_help_req', 'col_2_step_prior_read_help_message',
        'col_2_step_prior_scanning_help_message', 'col_2_step_prior_searching_for_bottom_out_hint',
        'col_2_step_prior_thought_before_attempt', 'col_2_step_prior_planned_ahead', 'col_2_step_prior_guessed',
        'col_2_step_prior_unsuccessful_but_sincere', 'col_2_step_prior_guessing_with_values_from_poblem',
        'col_2_step_prior_did_not_read_error_msg', 'col_2_step_prior_thought_about_error',
        'col_2_step_prior_same_answer_diff_context', 'col_2_step_prior_similarity_distance',
        'col_2_step_prior_similar_answer', 'col_2_step_prior_switched_context_before_correct',
        'col_2_step_prior_same_context', 'col_2_step_prior_repeated_step',
        'col_2_step_prior_diff_answer_and_or_diff_context', 'col_2_step_prior_Input', 'col_2_step_prior_Outcome',
        'col_3_step_prior_did_not_think_before_help_req', 'col_3_step_prior_thought_before_help_req',
        'col_3_step_prior_read_help_message', 'col_3_step_prior_scanning_help_message',
        'col_3_step_prior_searching_for_bottom_out_hint', 'col_3_step_prior_thought_before_attempt',
        'col_3_step_prior_planned_ahead', 'col_3_step_prior_guessed', 'col_3_step_prior_unsuccessful_but_sincere',
        'col_3_step_prior_guessing_with_values_from_poblem', 'col_3_step_prior_did_not_read_error_msg',
        'col_3_step_prior_thought_about_error', 'col_3_step_prior_same_answer_diff_context',
        'col_3_step_prior_similarity_distance', 'col_3_step_prior_similar_answer',
        'col_3_step_prior_switched_context_before_correct', 'col_3_step_prior_same_context',
        'col_3_step_prior_repeated_step', 'col_3_step_prior_diff_answer_and_or_diff_context',
        'col_3_step_prior_Input', 'col_3_step_prior_Outcome', 'col_0_vs_2_step_prior_similarity_distance',
        'col_0_vs_2_step_prior_similar_answer', 'col_0_vs_3_step_prior_similarity_distance',
        'col_0_vs_3_step_prior_similar_answer', 'col_1_vs_3_step_prior_similarity_distance',
        'col_1_vs_3_step_prior_similar_answer', 'gaming_pattern_1', 'gaming_pattern_2', 'gaming_pattern_3',
        'gaming_pattern_4', 'gaming_pattern_5', 'gaming_pattern_6', 'gaming_pattern_7', 'gaming_pattern_8',
        'gaming_pattern_9', 'gaming_pattern_10', 'gaming_pattern_11', 'gaming_pattern_12', 'gaming_pattern_13',
        'clip_index', 'index_in_clip', 'clip_index_group', 'is_gaming']

    df_main_min_6.drop(columns=drop_columns_2, inplace=True)

    # print("Complete: rule based gaming behavior label generation")

    return df_main_min_6


parser = argparse.ArgumentParser(description="Session level feature engineering.")
parser.add_argument('-programDir', type=str, help="the component program directory")
parser.add_argument('-workingDir', type=str, help="the component instance working directory")
parser.add_argument('-minClassSize', type=int, help="the threshold for class size", default=5)
parser.add_argument('-schoolHoursStartTime', type=int, help="the start time for the school day (24hrs format)", default=7)
parser.add_argument('-schoolHoursStopTime', type=int, help="the end time for the school day (24hrs format)", default=16)
parser.add_argument('-datasetPath', type=str, required=True, help="the file to be processed")

# catch any trailing additional unncessary command the wrapper might have included in the call
# parser.add_argument('remainder', nargs=argparse.REMAINDER, help="catch-all for any other arguments")

args, remaining_args = parser.parse_known_args()
# print("================")
# print("passed arguments:", args)

programDir = args.programDir
workingDir = args.workingDir
minClassSize = args.minClassSize
schoolHoursStartTime = args.schoolHoursStartTime
schoolHoursStopTime = args.schoolHoursStopTime
datasetPath = args.datasetPath


def logToWfl(msg):
    # log_file_name = os.path.join(workingDir, 'sessionlevelfeaturesLog.wfl')
    log_file_name = os.path.join(programDir, 'WorkflowComponent.log')
    now = dt.now()
    # Use a context manager to open the file, write the log, and automatically close the file
    with open(log_file_name, 'a') as logFile:  # 'a' opens the file for appending
        logFile.write(f"[python] {now}: {msg}\n")


logToWfl(args)

output_files = ["classSessionInfo.txt", "studentSessionInfo.txt", "txSessionAndGamingInfo.txt", "sessionLevelAggFeatures.txt"]

logToWfl(os.path.join(workingDir, output_files[0]))
logToWfl(os.path.join(workingDir, output_files[1]))
logToWfl(os.path.join(workingDir, output_files[2]))
logToWfl(os.path.join(workingDir, output_files[3]))


def remove_tabs (df):
    for col in df.select_dtypes(include=[object]):
        df[col] = df[col].str.replace('\t', ' ', regex=False)
        df[col] = df[col].str.replace('\n', ' ', regex=False)
        df[col] = df[col].str.replace('\"', ' ', regex=False)
        df[col] = df[col].str.replace('\'', ' ', regex=False)
    return df


try:
    df_main = pd.read_csv(datasetPath, sep='\t')
    # df_main = df_main.loc[df_main.Class.isin(['cls_aaeb6f'])]
    # df_main = pd.read_csv("../data/processed_data/test_input.txt", sep="\t")


    logToWfl("df_tx_session_info")
    df_tx_session_info = generate_class_session_info(df_main)
    logToWfl("df_class_session_info")
    df_class_session_info = generate_class_session_info_agg_class(df_tx_session_info)
    logToWfl("df_student_session_info")
    df_student_session_info = generate_student_session_info_agg_class(
        df_tx_session_info, df_class_session_info)

    logToWfl("df_agg_session_level_features")
    df_agg_session_level_features = generate_session_level_features(
        df_tx_session_info, schoolHoursStartTime, schoolHoursStopTime, minClassSize)

    df_tx_session_info_gaming_features = generate_rulebased_gaming_features(df_tx_session_info.copy())
    df_tx_session_info_gaming_labels = generate_rulebased_gaming_labels(df_tx_session_info_gaming_features.copy())

    os.makedirs(workingDir, exist_ok=True)
    # output_file = os.path.join(workingDir, 'sessionlevelfeaturesResult.txt')
    # df_agg_session_level_features.to_csv(output_file, sep='\t', index=False)
    df_class_session_info = remove_tabs(df_class_session_info)
    df_class_session_info.to_csv(os.path.join(workingDir, output_files[0]), sep="\t", index=False, encoding='utf-8', na_rep='NaN')

    df_student_session_info = remove_tabs(df_student_session_info)
    df_student_session_info.to_csv(os.path.join(workingDir, output_files[1]), sep="\t", index=False, encoding='utf-8', na_rep='NaN')

    df_tx_session_info_gaming_labels = remove_tabs(df_tx_session_info_gaming_labels)
    df_tx_session_info_gaming_labels.to_csv(os.path.join(workingDir, output_files[2]), sep="\t", index=False, encoding='utf-8', na_rep='NaN')

    df_agg_session_level_features = remove_tabs(df_agg_session_level_features)
    df_agg_session_level_features.to_csv(os.path.join(workingDir, output_files[3]), sep="\t", index=False, encoding='utf-8', na_rep='NaN')


    # df_class_session_info.to_csv(os.path.join(workingDir, output_files[0]), sep="\t", index=False, )
    # df_student_session_info.to_csv(os.path.join(workingDir, output_files[1]), sep="\t", index=False, )
    # df_tx_session_info_gaming_labels.to_csv(os.path.join(workingDir, output_files[2]), sep="\t", index=False, )
    # df_agg_session_level_features.to_csv(os.path.join(workingDir, output_files[3]), sep="\t", index=False, )

    logToWfl("Output Shape (df_class_session_info) : " + str(df_class_session_info.shape))
    logToWfl("Output Shape (df_student_session_info) : " + str(df_student_session_info.shape))
    logToWfl("Output Shape (df_tx_session_info_gaming_labels) : " + str(df_tx_session_info_gaming_labels.shape))
    logToWfl("Output Shape (df_agg_session_level_features) : " + str(df_agg_session_level_features.shape))
except Warning as e:
    logToWfl(e)

