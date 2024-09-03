#!/usr/bin/env python
# coding: utf-8

# In[1]:


from datetime import datetime
import argparse
import os
import datetime as dt
import warnings
import re
import numpy as np
import pandas as pd
from scipy import sparse
from scipy.sparse import load_npz, csr_matrix
from collections import defaultdict
from sklearn.preprocessing import OneHotEncoder
from sklearn.linear_model import LogisticRegression
from math import sqrt

from sklearn.metrics import roc_auc_score, accuracy_score, log_loss, brier_score_loss, mean_squared_error

import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

from utils.queue import TimeWindowQueue


# In[9]:


def prepare_data(data_file, working_dir, min_interactions_per_user, kc_col_name, remove_nan_skills, train_split_type=None, train_split=0.8, cv_student=None, cv_item=None, cv_fold=3):
    """Preprocess dataset.

    Arguments:
        data_file (str): student-step rollup data file location
        min_interactions_per_user (int): minimum number of interactions per student
        kc_col_name (str): Skills id column
        remove_nan_skills (bool): if True, remove interactions with no skill tag
        train_split_type (str): student or item, when None, don't split
        train_split (float): proportion of data to use for training, when None, don't split
        cv_fold_type (str): student or item; if None, don't run.
        cv_fold (int): cv fold; if None, don't run.

    Outputs:
        df (pandas DataFrame): preprocessed dataset with user_id, item_id,
            timestamp, correct and unique skill features
        Q_mat (item-skill relationships sparse array): corresponding q-matrix
    """
    
    df = pd.read_csv(data_file, delimiter='\t')
    df = df.rename(columns={'Anon Student Id': 'user_id',
                            'First Attempt': 'correct'})
    
    # Create item from problem and step
    df["Problem Hierarchy"] = df["Problem Hierarchy"].astype(str)
    df["Problem Name"] = df["Problem Name"].astype(str)
    df["Step Name"] = df["Step Name"].astype(str)
    df["item_id"] = df["Problem Hierarchy"] + ";" + df["Problem Name"] + ";" + df["Step Name"]

    # Add timestamp
    df["timestamp"] = pd.to_datetime(df["First Transaction Time"])
    df["timestamp"] = df["timestamp"] - df["timestamp"].min()
    df["timestamp"] = df["timestamp"].apply(lambda x: x.total_seconds()).astype(np.int64)
    
    # change to 1 and 0
    df['correct'] = df['correct'].astype(str).str.lower()
    df.loc[df['correct'].isin(['correct','true','1']), 'correct'] = 1
    df.loc[df['correct'] != 1, 'correct'] = 0
    df['correct'] = df['correct'].astype(np.int32)
    
    # Filter nan skills
    if remove_nan_skills:
        df = df[~df[kc_col_name].isnull()]
    else:
        df.ix[df[kc_col_name].isnull(), kc_col_name] = 'NaN'
    # Drop duplicates
    df.drop_duplicates(subset=["user_id", "item_id", "timestamp"], inplace=True)
    # Filter too short sequences
    df = df.groupby("user_id").filter(lambda x: len(x) >= min_interactions_per_user)
    df[kc_col_name] = df[kc_col_name].astype('str')
    # Extract KCs
    kc_list = []
    for kc_str in df[kc_col_name].unique():
        for kc in kc_str.split('~~'):
            kc_list.append(kc)
    kc_set = set(kc_list)
    #kc2idx is skill name to skill id mapping dict
    kc2idx = {kc: i for i, kc in enumerate(kc_set)}
    
    df["user_id"] = df["user_id"].astype(str)
    df["user_id_orig"] = df["user_id"].copy()
    df["user_id"] = np.unique(df["user_id"], return_inverse=True)[1]
    df["item_id_orig"] = df["item_id"].copy()
    df["item_id"] = np.unique(df["item_id"], return_inverse=True)[1]
    
    user2idx = (df[["user_id_orig", "user_id"]]).drop_duplicates()
    user2idx = user2idx.set_index('user_id_orig').to_dict()['user_id']
    
    item2idx = (df[["item_id_orig", "item_id"]]).drop_duplicates()
    item2idx = item2idx.set_index('item_id_orig').to_dict()['item_id']
    
#     print(kc2idx)
#     print(user2idx)
#     print(item2idx)
    
    # Build Q-matrix
    #item is the rows and skill is the column
    Q_mat = np.zeros((len(df["item_id"].unique()), len(kc_set)))
    for item_id, kc_str in df[["item_id", kc_col_name]].values:
        for kc in kc_str.split('~~'):
            Q_mat[item_id, kc2idx[kc]] = 1 
    
    # Get unique skill id from combination of all skill ids
    unique_skill_ids = np.unique(Q_mat, axis=0, return_inverse=True)[1]
    #print(np.unique(Q_mat, axis=0, return_inverse=True))
    
    df["skill_id"] = unique_skill_ids[df["item_id"]]
    
    # Sort data temporally
    df.sort_values(by="timestamp", inplace=True)

    # Sort data by users, preserving temporal order for each user
    df = pd.concat([u_df for _, u_df in df.groupby("user_id")])

    df = df[["user_id", "item_id", "timestamp", "correct", "skill_id"]]
    df.reset_index(inplace=True, drop=True)

    # Text files for BKT implementation (https://github.com/robert-lindsey/WCRP/)
    bkt_dataset = df[["user_id", "item_id", "correct"]]
    bkt_skills = unique_skill_ids
    #reshape (1,-1): one row and python figure out how many columns
    #split students into 5 groups
    bkt_split = np.random.randint(low=0, high=5, size=df["user_id"].nunique()).reshape(1, -1)
    
    # Save data
    sparse.save_npz(os.path.join(working_dir, "q_mat.npz"), sparse.csr_matrix(Q_mat))
    df.to_csv(os.path.join(working_dir, "preprocessed_data.txt"), sep="\t", index=False)
    np.savetxt(os.path.join(working_dir, "bkt_dataset.txt"), bkt_dataset, fmt='%i')
    np.savetxt(os.path.join(working_dir, "bkt_expert_labels.txt"), bkt_skills, fmt='%i')
    np.savetxt(os.path.join(working_dir, "bkt_splits.txt"), bkt_split, fmt='%i')
    
    if train_split_type is not None:
        # Train-test split
        #this is split by item
        if train_split_type == 'item':
            #item_id in order: 0,1,.... 
            items = df["item_id"].unique()
            #item_id in order randomized: 137,11,96....
            np.random.shuffle(items)
            #default is 80% in training, 20% in testing
            split = int(train_split * len(items))
            train_df = df[df["item_id"].isin(items[:split])]
            test_df = df[df["item_id"].isin(items[split:])]
        #all other split is defaulted to user
        else:   
            #user_id in order: 0,1,.... 
            users = df["user_id"].unique()
            #user_id in order randomized: 137,11,96....
            np.random.shuffle(users)
            #default is 80% in training, 20% in testing
            split = int(train_split * len(users))
            train_df = df[df["user_id"].isin(users[:split])]
            test_df = df[df["user_id"].isin(users[split:])]
        train_df.to_csv(os.path.join(working_dir, "preprocessed_data_train.txt"), sep="\t", index=False)
        test_df.to_csv(os.path.join(working_dir, "preprocessed_data_test.txt"), sep="\t", index=False)
        
    if cv_item is not None or cv_student is not None:
        # cv split
        test_split = 1/cv_fold
        #this is split by item
        if cv_item:
            #item_id in order: 0,1,.... 
            items = df["item_id"].unique()
            #item_id in order randomized: 137,11,96....
            np.random.shuffle(items)
            split_unit = int(test_split * len(items))
            for x in range(cv_fold):
                if x < cv_fold-1:
                    item_ids_in_fold = items[x*split_unit:(x+1)*split_unit]
                else:
                    item_ids_in_fold = items[x*split_unit:]
                test_df = df.loc[df['item_id'].isin(item_ids_in_fold)]
                train_df = df.loc[~df['item_id'].isin(item_ids_in_fold)]
                train_df.to_csv(os.path.join(working_dir, f"preprocessed_data_cv_item_train_fold_{(x+1)}.txt"), sep="\t", index=False)
                test_df.to_csv(os.path.join(working_dir, f"preprocessed_data_cv_item_test_fold_{(x+1)}.txt"), sep="\t", index=False)
        if cv_student:    
            #user_id in order: 0,1,.... 
            users = df["user_id"].unique()
            #user_id in order randomized: 137,11,96....
            np.random.shuffle(users)
            split_unit = int(test_split * len(users))
            for x in range(cv_fold):
                if x < cv_fold-1:
                    user_ids_in_fold = users[x*split_unit:(x+1)*split_unit]
                else:
                    user_ids_in_fold = users[x*split_unit:]
                test_df = df.loc[df['user_id'].isin(user_ids_in_fold)]
                train_df = df.loc[~df['user_id'].isin(user_ids_in_fold)]
                train_df.to_csv(os.path.join(working_dir, f"preprocessed_data_cv_student_train_fold_{(x+1)}.txt"), sep="\t", index=False)
                test_df.to_csv(os.path.join(working_dir, f"preprocessed_data_cv_student_test_fold_{(x+1)}.txt"), sep="\t", index=False)
    return df, Q_mat, kc2idx, user2idx, item2idx
#test
# print("before time for preparing data: ", datetime.now().strftime("%H:%M:%S"))    
# prepare_data(data_file="ds76_student_step_All_Data_74_2020_0926_034727.txt", working_dir=".",
#             min_interactions_per_user=1,
#             kc_col_name="KC (Circle-Collapse)",
#             remove_nan_skills=True, 
#             train_split_type='user',
#              cv_student=True, cv_item=True, cv_fold=3
#             )
# print("after time for preparing data: ", datetime.now().strftime("%H:%M:%S"))  


# In[10]:


def phi(x):
    return np.log(1 + x)

###### opportunity always starts at 1 and logged!!! This is done in phi()
#seconds in month, week, day, hour
WINDOW_LENGTHS = [3600 * 24 * 30, 3600 * 24 * 7, 3600 * 24, 3600]
NUM_WINDOWS = len(WINDOW_LENGTHS) + 1


# In[11]:


def df_to_sparse(df, Q_mat, active_features):
    """Build sparse dataset from dense dataset and q-matrix.

    Arguments:
        df (pandas DataFrame): output by prepare_data
        Q_mat (sparse array): q-matrix, output by prepare_data
        active_features (list of str): features

    Output:
        sparse_df (sparse array): sparse dataset where first 5 columns are the same as in df
    """
    num_items, num_skills = Q_mat.shape
    features = {}

    # Counters for continuous time windows
    counters = defaultdict(lambda: TimeWindowQueue(WINDOW_LENGTHS))
    
    #when a key is not found in counters, a new entry will be entered to counter as key:(a new class of TimeWindowQueue)
    #TimeWindowQueue is defined in util/queue.py
#     test_counters = defaultdict(lambda: TimeWindowQueue(WINDOW_LENGTHS))
#     print(test_counters["mine"].window_lengths)
#     print(test_counters["mine"].cursors)

    # Transform q-matrix into dictionary for fast lookup
    # Q_mat_dict has item_id as key, set of skill id as value, e.g., 0: {103, 79}, 1: {103, 79},
    Q_mat_dict = {i: set() for i in range(num_items)}
    for i, j in np.argwhere(Q_mat == 1):
        Q_mat_dict[i].add(j)
    #print(Q_mat_dict)
    

    # Keep track of original dataset
    features['df'] = np.empty((0, len(df.keys())))
    #features's df element is an empty array with o rows and 5 columns
    
    # Skill features
    if 's' in active_features:
        features["s"] = sparse.csr_matrix(np.empty((0, num_skills)))
    #features's s is an empty sparse matrix with o rows and num_skills columns
    
    # Past attempts and wins features
    #-w: historical counts include wins
    #-a: historical counts include attempts
    #-tw: historical counts are encoded as time windows
    for key in ['a', 'w']:
        if key in active_features:
            if 'tw' in active_features: #with tw and a or/and w
                features[key] = sparse.csr_matrix(np.empty((0, (num_skills + 2) * NUM_WINDOWS)))
            else: #without tw and a or/and w
                features[key] = sparse.csr_matrix(np.empty((0, num_skills + 2)))
    #features's a or w element is an empty array with o rows and num_skills + 2 columns if without tw
    #features'sa or w element is an empty array with o rows and (num_skills+2)*5 columns if with tw (5 is NUM_WINDOWS)
    
    # Build feature rows by iterating over users
    #count = 1
    for user_id in df["user_id"].unique():
#         if count >2:
#             break
#         count = count +1
        df_user = df[df["user_id"] == user_id][["user_id", "item_id", "timestamp", "correct", "skill_id"]].copy()
        #turn dataframe to array of list, as rows are array, values of each column is elements of list
        df_user = df_user.values
        num_items_user = df_user.shape[0]
        #get skills from q_mat based on item_id: skills's format: [[0. 0. 0. ... 0. 0. 0.][0. 0. 0. ... 0. 0. 0.].....]
        skills = Q_mat[df_user[:, 1].astype(int)].copy()
        #vstack: stack arrays in sequence vertically, features['df'] keeps entire df
        features['df'] = np.vstack((features['df'], df_user))
        #put item_ids in matrix row x 1
        item_ids = df_user[:, 1].reshape(-1, 1)
        #put success in matrix row x 1
        labels = df_user[:, 3].reshape(-1, 1)
        # Current skills one hot encoding
        if 's' in active_features:
            #turn skills into 
            #(0,20) 1 
            #(1,20) 1  
            #(2, 49) 1.0
            #(2, 53) 1.0 formated matrix, i.e. third row has multi skills: 49 and 53
            #save to features['s']
            features['s'] = sparse.vstack([features["s"], sparse.csr_matrix(skills)])
            
        
        # Attempts
        if 'a' in active_features:
            # Time windows
            if 'tw' in active_features:
                #make a matrix of row_count x (each skill + 2)*5 (5 is seconds in month, week, day, hour, <hour)
                attempts = np.zeros((num_items_user, (num_skills + 2) * NUM_WINDOWS))
                #df_user[:,1:3]: item_id and timestamp
                for i, (item_id, ts) in enumerate(df_user[:, 1:3]):
                    # Past attempts for relevant skills: skills historical counts
                    if 'sc' in active_features:
                        #Q_mat_dict[item_id]: gives skill_id(s) for item like: {12,41,109}
                        for skill_id in Q_mat_dict[item_id]:
                            #for this feature, counters dict user key=(user_id, skill_id, "skill"); value=TimeWindowQueue
                            #TimeWindowQueue is defined in queue.py file 
                            counts = phi(np.array(counters[user_id, skill_id, "skill"].get_counters(ts)))
                            #in attempts, for each row, there are 5 (i.e. NUM_WINDOWS) columns for each skill that the item has
                            attempts[i, skill_id * NUM_WINDOWS:(skill_id + 1) * NUM_WINDOWS] = counts
                            #reset the counters for (user_id, skill_id, "skill")
                            counters[user_id, skill_id, "skill"].push(ts)

                    # Past attempts for item: items historical counts
                    if 'ic' in active_features:
                        #for this feature, counters dict uses key=(user_id, skill_id, "item"); value=TimeWindowQueue
                        #TimeWindowQueue is defined in queue.py file
                        counts = phi(np.array(counters[user_id, item_id, "item"].get_counters(ts)))
                        #add to the second last set of attempts' columns (remember: attempts is row X (num_skills+2))
                        attempts[i, -2 * NUM_WINDOWS:-1 * NUM_WINDOWS] = counts
                        #reset the counters for (user_id, skill_id, "item")
                        counters[user_id, item_id, "item"].push(ts)

                    # Past attempts for all items
                    if 'tc' in active_features:
                        #for this feature, counters dict uses key=user_id; value=TimeWindowQueue
                        #TimeWindowQueue is defined in queue.py file
                        counts = phi(np.array(counters[user_id].get_counters(ts)))
                        #add to the last set of attempts' columns (remember: attempts is row X (num_skills+2))
                        attempts[i, -1 * NUM_WINDOWS:] = counts
                        counters[user_id].push(ts)

            # Counts
            else:
                #attempts is all 0 array of row x (num_skills + 2)
                attempts = np.zeros((num_items_user, num_skills + 2))

                # Past attempts for relevant skills
                if 'sc' in active_features:
                    #add an all 0 rows to skills (rowsxnum_skills); and delete last row
                    tmp = np.vstack((np.zeros(num_skills), skills))[:-1]
                    #np.cumsum(tmp, 0):
                    #add all previous rows together to get opportunity count for each skill: e.g. 6 skills
                    #90 80 0 52 18 0 second last row 
                    #90 81 1 52 18 0 meaning last row sees two skills: skill#2 and skill#3
                    attempts[:, :num_skills] = phi(np.cumsum(tmp, 0) * skills)
                    #attempts is row x (num_skills + 2)
                    #each row represent skill log(opportunity+1)
                    #e.g. 5 skills,  second row  log2= 0.6931
                    #0 0 0 0 0 0 0 (row 1 see skill#2)
                    #0 0.69314718 0 0 0 0 0 (row 2 skill#2 again)
                    #0 0 0 0 0 0 0 (row 3 skill#1)
                    #0.69314718 1.0986 0 0 0 0 0 (row 3 skill#1 again and skill#2 third time)
                    

                # Past attempts for item
                if 'ic' in active_features:
                    #OneHotEncoder is from sklearn. 
                    #The input to this transformer should be an array-like of integers or strings, denoting the values taken on by categorical (discrete) features. 
                    #The features are encoded using a one-hot (aka ‘one-of-K’ or ‘dummy’) encoding scheme. 
                    #This creates a binary column for each category and returns a sparse matrix or dense array (depending on the sparse parameter)
                    #onehot is to get the code for each item_id. if there are 3 items, there are 3 columns, 100 is 1; 010 is 2, 001 is 3 
                    onehot = OneHotEncoder(n_values=df_user[:, 1].max() + 1)
                    #item_ids is a matrix of rowX1 to store item_id for each row
                    #item_ids_onehot is a matirx row X item_id.max+1; each row represent which item_id it is. e.g. if total 6 items
                    #0, 0, 0, 1, 0, 0: this row has item#4
                    #1, 0, 0, 0, 0, 0: this row has item#1
                    item_ids_onehot = onehot.fit_transform(item_ids).toarray()
                    #np.cumsum(item_ids_onehot, 0): add all previous rows count for each column to get the item opportunity 
                    #tmp is add one row of all 0 array (size of item_id.max+1) to  and delete the last row, matrix of row X item_id.max+1
                    tmp = np.vstack((np.zeros(item_ids_onehot.shape[1]), np.cumsum(item_ids_onehot, 0)))[:-1]
                    #add to the second last column of attempts with opportunity count for the item of that row, e.g.
                    #0 row 1 for item#1
                    #0.0 row 2 for item#2
                    #1.0 row 3 for item#1
                    #2.0 row 3 for item#1
                    attempts[:, -2] = phi(tmp[np.arange(num_items_user), df_user[:, 1]])
                    
                    
            
                # Past attempts for all items
                if 'tc' in active_features:
                    #the last column is the log of row No. for the student starting from log(1) to log(total row) for the student 
                    attempts[:, -1] = phi(np.arange(num_items_user))
                

            features['a'] = sparse.vstack([features['a'], sparse.csr_matrix(attempts)])

        # Wins
        if "w" in active_features:
            # Time windows
            if 'tw' in active_features:
                wins = np.zeros((num_items_user, (num_skills + 2) * NUM_WINDOWS))

                for i, (item_id, ts, correct) in enumerate(df_user[:, 1:4]):
                    # Past wins for relevant skills
                    if 'sc' in active_features:
                        for skill_id in Q_mat_dict[item_id]:
                            counts = phi(np.array(counters[user_id, skill_id, "skill", "correct"].get_counters(ts)))
                            wins[i, skill_id * NUM_WINDOWS:(skill_id + 1) * NUM_WINDOWS] = counts
                            if correct:
                                counters[user_id, skill_id, "skill", "correct"].push(ts)

                    # Past wins for item
                    if 'ic' in active_features:
                        counts = phi(np.array(counters[user_id, item_id, "item", "correct"].get_counters(ts)))
                        wins[i, -2 * NUM_WINDOWS:-1 * NUM_WINDOWS] = counts
                        if correct:
                            counters[user_id, item_id, "item", "correct"].push(ts)

                    # Past wins for all items
                    if 'tc' in active_features:
                        counts = phi(np.array(counters[user_id, "correct"].get_counters(ts)))
                        wins[i, -1 * NUM_WINDOWS:] = counts
                        if correct:
                            counters[user_id, "correct"].push(ts)

            # Counts
            else:
                wins = np.zeros((num_items_user, num_skills + 2))

                # Past wins for relevant skills
                if 'sc' in active_features:
                    tmp = np.vstack((np.zeros(num_skills), skills))[:-1]
                    corrects = np.hstack((np.array(0), df_user[:, 3])).reshape(-1, 1)[:-1]
                    wins[:, :num_skills] = phi(np.cumsum(tmp * corrects, 0) * skills)
                    

                # Past wins for item
                if 'ic' in active_features:
                    onehot = OneHotEncoder(n_values=df_user[:, 1].max() + 1)
                    item_ids_onehot = onehot.fit_transform(item_ids).toarray()
                    tmp = np.vstack((np.zeros(item_ids_onehot.shape[1]), np.cumsum(item_ids_onehot * labels, 0)))[:-1]
                    wins[:, -2] = phi(tmp[np.arange(num_items_user), df_user[:, 1]])

                # Past wins for all items
                if 'tc' in active_features:
                    wins[:, -1] = phi(np.concatenate((np.zeros(1), np.cumsum(df_user[:, 3])[:-1])))

            features['w'] = sparse.vstack([features['w'], sparse.csr_matrix(wins)])

    # User and item one hot encodings
    onehot = OneHotEncoder()
    if 'u' in active_features:
        features['u'] = onehot.fit_transform(features["df"][:, 0].reshape(-1, 1))
    if 'i' in active_features:
        features['i'] = onehot.fit_transform(features["df"][:, 1].reshape(-1, 1))
        
    '''
    for features: s, a, sc
    features['df']
    0	159661	616236	1	190 
    0	159662	616298	1	190 
    0	159665	616339	1	114
    0	159666	616393	1	42
    0	159668	616498	1	42
    0	159667	616533	1	108
    0	159669	616574	1	108
    0	159670	616585	1	42

    skill combination vs skill_id
    190: 20
    114: 49, 53
    42: 76, 91, 100
    108: 49

    featrues['a']
    (1, 20)	0.6931471805599453
    (4, 76)	0.6931471805599453
    (4, 91)	0.6931471805599453
    (4, 100)	0.6931471805599453
    (5, 49)	0.6931471805599453
    (6, 49)	1.0986122886681098
    (7, 76)	1.0986122886681098
    (7, 91)	1.0986122886681098
    (7, 100)	1.0986122886681098

    features['s']
    (0, 20)	1.0
    (1, 20)	1.0
    (2, 49)	1.0
    (2, 53)	1.0
    (3, 76)	1.0
    (3, 91)	1.0
    (3, 100)	1.0
    (4, 76)	1.0
    (4, 91)	1.0
    (4, 100)	1.0
    (5, 49)	1.0
    (6, 49)	1.0
    (7, 76)	1.0
    (7, 91)	1.0
    (7, 100)	1.0
    
    when 2 students and each have 1131 4630 rows
    for feature s,a,sc:
    df: 5761x5
    a: 5761x114
    s: 5761x112
    for feature s,a,sc,u,i,ic,tc,w:
    df: 5761x5
    a: 5761x114
    s: 5761x112
    u: 5761x2
    i: 5761x5764
    w: 5761x114
    '''

    X = sparse.hstack([sparse.csr_matrix(features['df']),
                       sparse.hstack([features[x] for x in features.keys() if x != 'df'])]).tocsr()
    return X

#test
# print("before time for encoding data: ", datetime.now().strftime("%H:%M:%S"))    
# df = pd.read_csv('preprocessed_data.txt', sep="\t")
# df = df[["user_id", "item_id", "timestamp", "correct", "skill_id"]]
# Q_mat = sparse.load_npz('q_mat.npz').toarray()
# active_features = ['s','a', 'sc']
# features_suffix = ''.join(active_features)
# X = df_to_sparse(df, Q_mat, active_features)
# sparse.save_npz(f"X-{features_suffix}", X)
# print("after time for encoding data: ", datetime.now().strftime("%H:%M:%S"))    


# In[12]:


def df_to_sparse_afm(df, Q_mat):
    """Build sparse dataset from dense dataset and q-matrix for AFM

    Arguments:
        df (pandas DataFrame): output by prepare_data
        Q_mat (sparse array): q-matrix, output by prepare_data

    Output:
        sparse_df (sparse array): sparse dataset where first 5 columns are the same as in df, next set of columns are for each students;
        followed by skillX2 columns that a pair of columns for each skill (skill existence + opportunity)
    """
    num_items, num_skills = Q_mat.shape
    features = {}
    
    num_users = len(df["user_id"].unique())

    # Transform q-matrix into dictionary for fast lookup
    # Q_mat_dict has item_id as key, set of skill id as value, e.g., 0: {103, 79}, 1: {103, 79},
    Q_mat_dict = {i: set() for i in range(num_items)}
    for i, j in np.argwhere(Q_mat == 1):
        Q_mat_dict[i].add(j)

    # Keep track of original dataset
    features['df'] = np.empty((0, len(df.keys())))
    #features's df element is an empty array with o rows and 5 columns
    
    #features's u is an empty sparse matrix with o rows and num_users columns
    features["u"] = sparse.csr_matrix(np.empty((0, num_users)))
    #features's s is an empty sparse matrix with o rows and num_skills columns
    features["s"] = sparse.csr_matrix(np.empty((0, num_skills)))
    #features's a is an empty sparse matrix with o rows and num_skills columns
    features['a'] = sparse.csr_matrix(np.empty((0, num_skills)))
    
    # Build feature rows by iterating over users
    #count = 1
    for user_id in df["user_id"].unique():
#         if count >2:
#             break
#         count = count +1
        df_user = df[df["user_id"] == user_id][["user_id", "item_id", "timestamp", "correct", "skill_id"]].copy()
        #turn dataframe to array of list, as rows are array, values of each column is elements of list
        df_user = df_user.values
        num_items_user = df_user.shape[0]
        #get skills from q_mat based on item_id: skills's format: [[0. 0. 0. ... 0. 0. 0.][0. 0. 0. ... 0. 0. 0.].....]
        skills = Q_mat[df_user[:, 1].astype(int)].copy()
        #vstack: stack arrays in sequence vertically, features['df'] keeps entire df
        features['df'] = np.vstack((features['df'], df_user))
        features['s'] = sparse.vstack([features["s"], sparse.csr_matrix(skills)])
        #attempts is all 0 array of row x (num_skills)
        attempts = np.zeros((num_items_user, num_skills))
        #add an all 0 rows to skills (rowsxnum_skills); and delete last row
        tmp = np.vstack((np.zeros(num_skills), skills))[:-1]
        #np.cumsum(tmp, 0):
        #add all previous rows together to get opportunity count for each skill: e.g. 6 skills
        #90 80 0 52 18 0 second last row 
        #90 81 1 52 18 0 meaning last row sees two skills: skill#2 and skill#3
        #attempts is row x (num_skills)
        #each row represent skill opportunity starting with 0
        #e.g. 5 skills,  second row  log2= 0.6931
        #0 0 0 0 0 (row 1 see skill#2)
        #0 1 0 0 0 (row 2 skill#2 again)
        #0 0 0 0 0 (row 3 skill#1)
        #1 2 0 0 0 (row 3 skill#1 again and skill#2 third time)
        attempts[:, :num_skills] = np.cumsum(tmp, 0) * skills
        features['a'] = sparse.vstack([features['a'], sparse.csr_matrix(attempts)])
    
    #add users
    onehot = OneHotEncoder()
    features['u'] = onehot.fit_transform(features["df"][:, 0].reshape(-1, 1))
    
    X = sparse.hstack([sparse.csr_matrix(features['df']), sparse.csr_matrix(features['u']), sparse.csr_matrix(features['s']),
                       sparse.csr_matrix(features['a'])]).tocsr()
    
    ''' X looks like this:
    features['df']
    0	159661	616236	1	190 
    0	159662	616298	1	190 
    0	159665	616339	1	114
    0	159666	616393	1	42
    0	159668	616498	1	42
    0	159667	616533	1	108
    0	159669	616574	1	108
    0	159670	616585	1	42

    skill combination vs skill_id
    190: 20
    114: 49, 53
    42: 76, 91, 100
    108: 49

    featrues['a']
    (1, 20)	0.6931471805599453
    (4, 76)	0.6931471805599453
    (4, 91)	0.6931471805599453
    (4, 100)	0.6931471805599453
    (5, 49)	0.6931471805599453
    (6, 49)	1.0986122886681098
    (7, 76)	1.0986122886681098
    (7, 91)	1.0986122886681098
    (7, 100)	1.0986122886681098

    features['s']
    (0, 20)	1.0
    (1, 20)	1.0
    (2, 49)	1.0
    (2, 53)	1.0
    (3, 76)	1.0
    (3, 91)	1.0
    (3, 100)	1.0
    (4, 76)	1.0
    (4, 91)	1.0
    (4, 100)	1.0
    (5, 49)	1.0
    (6, 49)	1.0
    (7, 76)	1.0
    (7, 91)	1.0
    (7, 100)	1.0
    
    when 2 students and each have 1131 4630 rows
    for feature s,a,sc:
    df: 5761x5
    a: 5761x114
    s: 5761x112
    for feature s,a,sc,u,i,ic,tc,w:
    df: 5761x5
    a: 5761x114
    s: 5761x112
    u: 5761x2
    i: 5761x5764
    w: 5761x114
    '''
    
    return X

#test
#first get data ready
# print("before time for preparing data: ", datetime.now().strftime("%H:%M:%S"))    
# df, Q_mat, kc2idx, user2idx, item2idx = prepare_data(data_file="ds76_student_step_All_Data_74_2020_0926_034727.txt", working_dir=".",
#             min_interactions_per_user=1,
#             kc_col_name="KC (Circle-Collapse)",
#             remove_nan_skills=True
#             )
# print("after time for preparing data: ", datetime.now().strftime("%H:%M:%S"))                 

# print(kc2idx)
# print(user2idx)

# print("before time for AFM encoding data: ", datetime.now().strftime("%H:%M:%S"))    
# df = pd.read_csv('preprocessed_data.txt', sep="\t")
# df = df[["user_id", "item_id", "timestamp", "correct", "skill_id"]]
# X = df_to_sparse_afm(df, Q_mat)
# print(X)
# sparse.save_npz(f"X-afm", X)
# print("after time for AFM encoding data: ", datetime.now().strftime("%H:%M:%S"))    


# In[13]:


def compute_metrics(y, y_pred):
    acc = accuracy_score(y, np.round(y_pred))
    auc = roc_auc_score(y, y_pred)
    nll = log_loss(y, y_pred)
    mse = brier_score_loss(y, y_pred)
    return acc, auc, nll, mse

def compute_rmse(y_actual, y_pred):
    return sqrt(mean_squared_error(y_actual, y_pred))

# calculate ll for regression
def calculate_ll(y_actual, y_pred):
    ll = -log_loss(y_actual, y_pred)*len(y_actual)
    return ll

# calculate aic for regression by ll
def calculate_aic_by_ll(ll, num_params):
    aic = -2*ll + 2*num_params
    return aic
    
# calculate bic for regression by ll
def calculate_bic_by_ll(ll, n, num_params):
    bic = -2*ll + num_params*np.log(n)
    return bic

# calculate aic for regression by mse
#AIC = n*log(residual sum of squares/n) + 2K
def calculate_aic_by_mse(n, mse, num_params):
    aic = n * np.log(mse) + 2 * num_params
    return aic

# calculate bic for regression by mse
def calculate_bic_by_mse(n, mse, num_params):
    bic = n * np.log(mse) + num_params * np.log(n)
    return bic


# In[14]:


def logToWfl(msg):
    logFile = open("learner_performnce_prediction.wfl", "a")
    now = dt.datetime.now()
    logFile.write(str(now) + ": " + msg + "\n");
    logFile.close();
    
def logProgressToWfl(progressMsg):
    logFile = open("learner_performnce_prediction.wfl", "a")
    now = dt.datetime.now()
    progressPrepend = "%Progress::"
    logFile.write(progressPrepend + "@" + str(now) + "@" + progressMsg + "\n");
    logFile.close();


# In[15]:


#test command from WF component:
#C:/ProgramData/Anaconda3/Python learner_performance_prediction.py -programDir . -workingDir . -userId 1 -includeCv Yes -kcModel_nodeIndex 0 -kcModel_fileIndex 0 -kcModel "KC (Circle-Collapse)" -numFold 3 -tpCV "item blocked" -node 0 -fileIndex 0 "ds76_student_step_export.txt"
#C:/ProgramData/Anaconda3/Python learner_performance_prediction.py -programDir . -workingDir . -userId 1 -includeCv Yes -kcModel_nodeIndex 0 -kcModel_fileIndex 0 -kcModel "KC (Circle-Collapse)" -kcModel_nodeIndex 0 -kcModel_fileIndex 0 -kcModel "KC (Concepts)" -kcModel_nodeIndex 0 -kcModel_fileIndex 0 -kcModel "KC (DecompArithDiam)" -numFold 3 -tpCV "both student and item blocked" -node 0 -fileIndex 0 "ds76_student_step_export.txt"

warnings.filterwarnings("ignore")
#fresh new log file
logFile = open("learner_performnce_prediction.wfl", "w")
logFile.close();

#command line
command_line = True
cv_student_number_error = False
cv_item_number_error = False
if command_line:
    parser = argparse.ArgumentParser(description='Python program to compute logistic regression.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-includeCv", type=str, choices=["Yes", "No"], default="No")
    parser.add_argument("-kcModel", type=str, action='append')
    parser.add_argument("-numFold", type=int, default=3)
    parser.add_argument("-tpCV", type=str, choices=["student blocked", "item blocked", "both student and item blocked" ], default='item blocked')
    parser.add_argument('-userId', type=str,  help='placeholder for WF', default='')
    args, option_file_index_args = parser.parse_known_args()
    #no train_split_type
    working_dir = args.workingDir
    train_split_type = None
    train_split = None
    file_name = args.fileIndex[0][1]
    kc_col_names = args.kcModel
    cv_student = False
    cv_item = False
    cv_fold = 3
    if (args.includeCv).lower() == "yes":
        if (args.tpCV).lower() == "item blocked" or (args.tpCV).lower() == "both student and item blocked":
            cv_item = True
        if (args.tpCV).lower() == "student blocked" or (args.tpCV).lower() == "both student and item blocked":
            cv_student = True
        cv_fold = args.numFold
else:
    #student_step file
    file_name = "ds6160_student_step_All_Data_8579_2024_0831_214024.txt"
    #file_name = "ds7_student_step_All_Data_7_2024_0723_172529.txt"
    #file_name = "ds76_student_step_export.txt"
    #file_name = "C:/WPIDevelopment/dev06_dev/test/AFM_improved_optimization/runFastAFM/ds392_student_step_All_Data_1310_2019_0802_022703.txt"
    #file_name = "ds76_student_step_All_Data_test_fastAFM.txt"
    #file_name = "C:/WPIDevelopment/dev06_dev/LFA/testData/search/input/DS 76 Geometry Area (1996-97)/Lasso ModelSSSVTS2.txt"
    #file_name = "C:/WPIDevelopment/dev06_dev/LFA/testData/search/input/DS 613 Hopewell 2011-2012/multiskill_converted_All_Data_1884_2013_0215_193821.txt"
    #file_name = "fast_afm_cv_ds_7_kcm_50_same_time.txt"
    #file_name = "./data/algebra05/data_DS_format.txt"
    #kc mode
    #kc_col_names=['KC (Circle-Collapse)', 'KC (Concepts)', 'KC (Lasso Model)']
    #kc_col_names=['KC (Lasso Model)']
    #kc_col_names=['KC(KTracedSkills)']
    #kc_col_name="KC (DecompArithDiam)"
    #kc_col_name="KC (Lasso Model)"
    kc_col_names=["KC (Concept)"]
    #kc_col_name="KC (Default)"
    #working_dir
    working_dir = "."
    #working_dir = "./data/algebra05"
    
    #for spliting data
    train_split_type='item'
    train_split=0.8

    #for cv
    cv_student = True
    #cv_student = False
    cv_item = True
    #cv_item = False
    cv_fold = 3
    
#min_interactions_per_user
min_interactions_per_user = 1
#min_interactions_per_user = 10
#remove_nan_skills
remove_nan_skills = True

#get kc_name
kc_names = []
for kc_col_name in kc_col_names:
    kc_name = re.search(r'\((.*?)\)',kc_col_name).group(1)
    kc_names.append(kc_name)

logToWfl("Calling prepare_data.")  
logProgressToWfl("0%")
kc_count = 0
prog = 0
analysis_summary_content = ""
model_values_content = ""
parameters_content = ""
#analysis-summary file
analysis_summary_file_name = os.path.join(working_dir, "analysis_summary.txt")
#model-values file
model_values_file_name = os.path.join(working_dir, "model_values.xml")
#parameters file
parameters_file_name = os.path.join(working_dir, "parameters.xml")
#prediction file
prediction_file_name = os.path.join(working_dir, "student_step_with_prediction.txt")
for kc_col_name in kc_col_names:
    kc_name = kc_names[kc_count]
    kc_count = kc_count + 1
    df, Q_mat, kc2idx, user2idx, item2idx = prepare_data(data_file=file_name, 
                                                     working_dir=working_dir,
                                                     min_interactions_per_user=min_interactions_per_user,
                                                     kc_col_name=kc_col_name,
                                                     remove_nan_skills=remove_nan_skills,
                                                     train_split_type=train_split_type, 
                                                     train_split=train_split,
                                                     cv_student=cv_student, 
                                                     cv_item=cv_item,
                                                     cv_fold=cv_fold)
    skill_df = pd.DataFrame(list(kc2idx.items()), columns =['skill_name', 'skill_id'])
    student_df = pd.DataFrame(list(user2idx.items()), columns =['student_name', 'student_id'])
    item_cnt = df['item_id'].nunique()
    #numbers of skill and student have to be >= cv_fold
    if cv_student and student_df.shape[0] < cv_fold:
        cv_student = False
        cv_student_number_error = True
        logToWfl("Can't run student-blocked CV because there are less students ({}) than CV fold ({})".format(student_df.shape[0], cv_fold))  
    if cv_item and item_cnt < cv_fold:
        cv_item = False
        cv_item_number_error = True
        logToWfl("Can't run item-blocked CV because there are less items ({}) than CV fold ({})".format(skill_df.shape[0], cv_fold))  

    logToWfl("Finished prepare data.")
    #approximate % of time
    if cv_student or cv_item: 
        #approximate % of time
        prog = prog + 1/(3*len(kc_col_names))
        logProgressToWfl( "{:.0%}".format(prog))
    else:
        prog = prog + 1/(2*len(kc_col_names))
        logProgressToWfl( "{:.0%}".format(prog))
    
    logToWfl("Calling df_to_sparse_afm to encode data.")  
    #df = pd.read_csv('preprocessed_data.txt', sep="\t")
    df = df[["user_id", "item_id", "timestamp", "correct", "skill_id"]]
    X = df_to_sparse_afm(df, Q_mat)
    sparse.save_npz(f"X-afm", X)
    logToWfl("Finished df_to_sparse_afm to encode data.")
    
    logToWfl("Starting training data.")  
    iter = 1000
    #do model on all data and get AIC, BIC
    X_all, y_all = X[:, 5:], X[:, 3].toarray().flatten()
    # Train
    #solver{‘newton-cg’, ‘lbfgs’, ‘liblinear’, ‘sag’, ‘saga’}, default=’lbfgs’
    #https://scikit-learn.org/stable/modules/generated/sklearn.linear_model.LogisticRegression.html
    model = LogisticRegression(solver="lbfgs", max_iter=iter)
    model.fit(X_all, y_all)
    
    #get overall intercept for skill
    overall_skill_intercept = model.intercept_
    #get params
    params = model.coef_[0]
    num_params = len(params)
    #attach to student_name and skill_name
    student_param = params[:student_df.shape[0]]
    student_df["intercept"] = student_param
    student_df = student_df[['student_name', 'intercept']]
    #print(student_df)
    end_ind_student = student_df.shape[0]
    end_ind_skill_intercept = end_ind_student+skill_df.shape[0]
    skill_intercept_param = params[end_ind_student:end_ind_skill_intercept]
    skill_slope_param = params[end_ind_skill_intercept:]
    skill_df["intercept"] = skill_intercept_param + overall_skill_intercept
    skill_df["slope"] = skill_slope_param
    skill_df = skill_df[['skill_id', 'skill_name', 'intercept', 'slope']]
    skill_df = skill_df.sort_values(by=['skill_name'])
    pd.set_option("display.max_rows", None, "display.max_columns", None)
    #print(skill_df)
    skill_df['intercept_exp'] = np.exp(skill_df['intercept'])
    skill_df['intercept_probability'] = skill_df['intercept_exp']/(1+skill_df['intercept_exp'])
    skill_df = skill_df.drop(['skill_id', 'intercept_exp'], axis=1)
    skill_df = skill_df[['skill_name', 'intercept', 'intercept_probability', 'slope']]

    #get prediction
    y_all_pred = pd.DataFrame(model.predict_proba(X_all))
    #use only the prediction to success
    y_all_pred = y_all_pred[y_all_pred.columns[1]]
    df["prediction"] = y_all_pred
    #get prediction and attached to original file
    df_original = pd.read_csv(file_name, delimiter='\t')
    #if the original file has already been modified for prediction
    if kc_count > 1:
        df_original = pd.read_csv(prediction_file_name, delimiter='\t')
    if remove_nan_skills:
        df_original = df_original[~df_original[kc_col_name].isnull()]
        #drop duplicate because prepare_data function is doing this
        df_original = df_original.drop_duplicates(subset=["Anon Student Id", "Problem Hierarchy", "Problem Name","Step Name", "First Transaction Time"]).reset_index(drop=True)
    #error rate prediction!
    #df_original[f"Predicted Error Rate ({kc_name})"] = 1-y_all_pred
    df_original["Predicted Error Rate (" + str(kc_name) + ")"] = 1-y_all_pred
    df_original.to_csv(prediction_file_name, sep="\t", index=False)

    #get ll, aic, bic
    ll = calculate_ll(y_all, y_all_pred)
    aic = calculate_aic_by_ll(ll, num_params)
    bic = calculate_bic_by_ll(ll, len(y_all), num_params)
    # print('LL: %.3f' % ll)
    # print('AIC: %.3f' % aic)
    # print('BIC: %.3f' % bic)
    #get other measures
    # acc, auc, nll, mse = compute_metrics(y_all, y_all_pred)
    # print('ACC: %.3f' % acc)
    # print('AUC: %.3f' % auc)
    # print('MSE: %.3f' % mse)
    logToWfl("After training data.")
    #approximate % of time
    if cv_student or cv_item: 
        #approximate % of time
        prog = prog + 1/(3*len(kc_col_names))
        logProgressToWfl( "{:.0%}".format(prog))
    else:
        prog = prog + 1/(2*len(kc_col_names))
        logProgressToWfl( "{:.0%}".format(prog))
    
    
    #handle train split
    if train_split_type is not None:
        train_df = pd.read_csv(os.path.join(working_dir, "preprocessed_data_train.txt"), sep="\t")
        test_df = pd.read_csv(os.path.join(working_dir, "preprocessed_data_test.txt"), sep="\t")
        if train_split_type == 'item':
            item_ids = X[:, 1].toarray().flatten()
            items_train = train_df["item_id"].unique()
            items_test = test_df["item_id"].unique()
            #np.isin(item_ids, items_train) gives True and False for all rows
            #np.where(np.isin(item_ids, items_train)) gives row id for all True
            #train is all X in train
            train = X[np.where(np.isin(item_ids, items_train))]
            test = X[np.where(np.isin(item_ids, items_test))]
        else:
            # Student-wise train-test split
            user_ids = X[:, 0].toarray().flatten()
            users_train = train_df["user_id"].unique()
            users_test = test_df["user_id"].unique()
            #np.isin(user_ids, users_train) gives True and False for all rows
            #np.where(np.isin(user_ids, users_train)) gives row id for all True
            #train is all X in train
            train = X[np.where(np.isin(user_ids, users_train))]
            test = X[np.where(np.isin(user_ids, users_test))]
        X_train, y_train = train[:, 5:], train[:, 3].toarray().flatten()
        X_test, y_test = test[:, 5:], test[:, 3].toarray().flatten()
        model = LogisticRegression(solver="lbfgs", max_iter=iter)
        model.fit(X_train, y_train)
        y_pred_train = model.predict_proba(X_train)[:, 1]
        y_pred_test = model.predict_proba(X_test)[:, 1]
        acc_train, auc_train, nll_train, mse_train = compute_metrics(y_train, y_pred_train)
        acc_test, auc_test, nll_test, mse_test = compute_metrics(y_test, y_pred_test)
    #     print('ACC for training set: %.3f' % acc_train)
    #     print('AUC for training set: %.3f' % auc_train)
    #     print('MSE for training set: %.3f' % mse_train)
    #     print('ACC for testing set: %.3f' % acc_test)
    #     print('AUC for testing set: %.3f' % auc_test)
    #     print('MSE for testing set: %.3f' % mse_test)

    #handle student CV
    if cv_student:
        logToWfl("Start student blocked cross validation.")
        y_all = None
        y_pred_all = None
        for i in range(1, cv_fold+1):
            train_file_name = f"preprocessed_data_cv_student_train_fold_{i}.txt"
            test_file_name = f"preprocessed_data_cv_student_test_fold_{i}.txt"
            train_df = pd.read_csv(os.path.join(working_dir, train_file_name), sep="\t")
            test_df = pd.read_csv(os.path.join(working_dir, test_file_name), sep="\t")
            user_ids = X[:, 0].toarray().flatten()
            users_train = train_df["user_id"].unique()
            users_test = test_df["user_id"].unique()
            #np.isin(user_ids, users_train) gives True and False for all rows
            #np.where(np.isin(user_ids, users_train)) gives row id for all True
            #train is all X in train
            train = X[np.where(np.isin(user_ids, users_train))]
            test = X[np.where(np.isin(user_ids, users_test))]
            X_train, y_train = train[:, 5:], train[:, 3].toarray().flatten()
            X_test, y_test = test[:, 5:], test[:, 3].toarray().flatten()
            model = LogisticRegression(solver="lbfgs", max_iter=iter)
            model.fit(X_train, y_train)
            y_pred_test = model.predict_proba(X_test)[:, 1]
            if i == 1:
                y_all = y_test
                y_pred_all = y_pred_test
            else:
                y_all = np.concatenate((y_all,y_test), axis=0)
                y_pred_all = np.concatenate((y_pred_all,y_pred_test), axis=0)
        student_cv_rmse = compute_rmse(y_all, y_pred_all)
        logToWfl("Finished student blocked cross validation.")


    if cv_item:
        logToWfl("Start item blocked cross validation.")
        y_all = None
        y_pred_all = None
        for i in range(1, cv_fold+1):
            train_file_name = f"preprocessed_data_cv_item_train_fold_{i}.txt"
            test_file_name = f"preprocessed_data_cv_item_test_fold_{i}.txt"
            train_df = pd.read_csv(os.path.join(working_dir, train_file_name), sep="\t")
            test_df = pd.read_csv(os.path.join(working_dir, test_file_name), sep="\t")
            item_ids = X[:, 1].toarray().flatten()
            items_train = train_df["item_id"].unique()
            items_test = test_df["item_id"].unique()
            #np.isin(item_ids, items_train) gives True and False for all rows
            #np.where(np.isin(item_ids, items_train)) gives row id for all True
            #train is all X in train
            train = X[np.where(np.isin(item_ids, items_train))]
            test = X[np.where(np.isin(item_ids, items_test))]
            X_train, y_train = train[:, 5:], train[:, 3].toarray().flatten()
            X_test, y_test = test[:, 5:], test[:, 3].toarray().flatten()
            model = LogisticRegression(solver="lbfgs", max_iter=iter)
            model.fit(X_train, y_train)
            y_pred_test = model.predict_proba(X_test)[:, 1]
            if i == 1:
                y_all = y_test
                y_pred_all = y_pred_test
            else:
                y_all = np.concatenate((y_all,y_test), axis=0)
                y_pred_all = np.concatenate((y_pred_all,y_pred_test), axis=0)
        item_cv_rmse = compute_rmse(y_all, y_pred_all)
        logToWfl("Finished item blocked cross validation.")
        
    
    #write analysis_summary, model_values content
    if kc_count > 1:
        analysis_summary_content = analysis_summary_content + "\n\n\n"
    analysis_summary_content = analysis_summary_content + "KC Model Values for {} model\n".format(kc_name)
    analysis_summary_content = analysis_summary_content + "AIC\tBIC\tLog Likelihood\tNumber of Parameters\tNumber of Observations\n"
    analysis_summary_content = analysis_summary_content + "{:.8f}\t{:.8f}\t{:.8f}\t{}\t{}\n\n".format(aic, bic, ll, num_params, len(y_all))

    if kc_count == 1:
        model_values_content = model_values_content + "<model_values>\n"
    model_values_content = model_values_content + "<model>\n"
    model_values_content = model_values_content + "<name>{}</name>\n".format(kc_col_name)
    model_values_content = model_values_content + "<AIC>{:.8f}</AIC>\n".format(aic)
    model_values_content = model_values_content + "<BIC>{:.8f}</BIC>\n".format(bic)
    model_values_content = model_values_content + "<log_likelihood>{:.8f}</log_likelihood>\n".format(ll)

    #write CV to analysis-summary
    if cv_item and cv_student:
        analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
        analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (student blocked)\tCross Validation RMSE (item blocked)\n"
        analysis_summary_content = analysis_summary_content + "{:.8f}\t{:.8f}\n\n".format(student_cv_rmse, item_cv_rmse)
        model_values_content = model_values_content + "<student_blocked_cv>{:.8f}</student_blocked_cv>\n".format(student_cv_rmse)
        model_values_content = model_values_content + "<item_blocked_cv>{:.8f}</item_blocked_cv>\n".format(item_cv_rmse)
    if cv_item and not cv_student:
        if cv_student_number_error:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (student blocked)\tCross Validation RMSE (item blocked)\n"
            analysis_summary_content = analysis_summary_content + "NA*\t{:.8}\n".format(item_cv_rmse)
            analysis_summary_content = analysis_summary_content + "*Number of students is less than that of CV folds\n\n"
            model_values_content = model_values_content + "<item_blocked_cv>{:.8f}</item_blocked_cv>\n".format(item_cv_rmse)
        else:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (item blocked)\n"
            analysis_summary_content = analysis_summary_content + "{:.8}\n\n".format(item_cv_rmse)
            model_values_content = model_values_content + "<item_blocked_cv>{:.8f}</item_blocked_cv>\n".format(item_cv_rmse)
    elif cv_student and not cv_item:
        if cv_item_number_error:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (student blocked)\tCross Validation RMSE (item blocked)\n"
            analysis_summary_content = analysis_summary_content + "{:.8f}\tNA*\n".format(student_cv_rmse)
            analysis_summary_content = analysis_summary_content + "*Number of skills is less than that of CV folds\n\n"
            model_values_content = model_values_content + "<student_blocked_cv>{:.8f}</student_blocked_cv>\n".format(student_cv_rmse)
        else:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (student blocked)\n"
            analysis_summary_content = analysis_summary_content + "{:.8f}\n\n".format(student_cv_rmse)
            model_values_content = model_values_content + "<student_blocked_cv>{:.8f}</student_blocked_cv>\n".format(student_cv_rmse)        
    else:
        if cv_item_number_error and cv_student_number_error:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (student blocked)\tCross Validation RMSE (item blocked)\n"
            analysis_summary_content = analysis_summary_content + "NA*\tNA**\n"
            analysis_summary_content = analysis_summary_content + "*Number of students is less than that of CV folds\n"
            analysis_summary_content = analysis_summary_content + "**Number of skills is less than that of CV folds\n\n"
        elif cv_student_number_error:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (student blocked)\n"
            analysis_summary_content = analysis_summary_content + "NA*\n"
            analysis_summary_content = analysis_summary_content + "*Number of students is less than that of CV folds\n\n"
        elif cv_item_number_error:
            analysis_summary_content = analysis_summary_content + "Cross Validation Values (Blocked)\n"
            analysis_summary_content = analysis_summary_content + "Cross Validation RMSE (item blocked)\n"
            analysis_summary_content = analysis_summary_content + "NA*\n"
            analysis_summary_content = analysis_summary_content + "*Number of skills is less than that of CV folds\n\n"


    model_values_content = model_values_content + "</model>\n"
    if kc_count == len(kc_col_names):
        model_values_content = model_values_content + "</model_values>\n"
    model_values = open(model_values_file_name, "w")
    model_values.write(model_values_content)        
    model_values.close();

    #write kc values to analysis-summary
    analysis_summary_content = analysis_summary_content + "KC Values for {} model\n".format(kc_name)
    analysis_summary_content = analysis_summary_content + "KC Name\tIntercept (logit)\tIntercept (probability)\tSlope\n"
    for index, row in skill_df.iterrows():
        #skill_df columns are: 'skill_name', 'intercept', 'intercept_probability', 'slope'
        analysis_summary_content = analysis_summary_content + str(row['skill_name']) + "\t" + str(row['intercept']) + "\t" + str(row['intercept_probability']) + "\t" + str(row['slope']) + "\n"
    #write student values to analysis-summary
    analysis_summary_content = analysis_summary_content + "\nStudent Values for {} model\n".format(kc_name)
    analysis_summary_content = analysis_summary_content + 'A student intercept value of "N/A" means the student did not perform any steps associated with any of the KCs in the selected KC model.\n'
    analysis_summary_content = analysis_summary_content + 'Anon Student Id\tIntercept\n'
    for index, row in student_df.iterrows():
        #student_df columns are: 'student_name', 'intercept'
        analysis_summary_content = analysis_summary_content + str(row['student_name']) + "\t" + str(row['intercept']) + "\n"
    
    analysis_summary = open(analysis_summary_file_name, "w")
    analysis_summary.write(analysis_summary_content)
    analysis_summary.close()
    
    #write parameters.xml content
    if kc_count == 1:
        parameters_content = parameters_content + "<parameters>\n"
    if len(kc_col_names) > 1:
        parameters_content = parameters_content + "<model>\n<name>" + str(kc_name) + "</name>\n"
    #loop through skills
    for index, row in skill_df.iterrows():
        parameters_content = parameters_content + "<parameter>\n<type>Skill</type>\n"
        parameters_content = parameters_content + "<name>{}</name>\n".format(row['skill_name'])
        parameters_content = parameters_content + "<intercept>{:.8f}</intercept>\n".format(row['intercept'])
        parameters_content = parameters_content + "<slope>{:.8f}</slope>\n".format(row['slope'])
        parameters_content = parameters_content + "</parameter>\n"
    for index, row in student_df.iterrows():
        parameters_content = parameters_content + "<parameter>\n<type>Student</type>\n"
        parameters_content = parameters_content + "<name>{}</name>\n".format(row['student_name'])
        parameters_content = parameters_content + "<intercept>{:.8f}</intercept>\n".format(row['intercept'])
        parameters_content = parameters_content + "<slope></slope>\n"
        parameters_content = parameters_content + "</parameter>\n"
    if len(kc_col_names) > 1:
        parameters_content = parameters_content + "</model>\n"
    if kc_count == len(kc_col_names):
        parameters_content = parameters_content + "</parameters>\n"
    parameters = open(parameters_file_name, "w")
    parameters.write(parameters_content) 
    parameters.close();
    #approximate % of time
    if cv_student or cv_item: 
        logToWfl("After cross validation.")
        #approximate % of time
        prog = prog + 1/(3*len(kc_col_names))
        logProgressToWfl( "{:.0%}".format(prog))


# In[ ]:




