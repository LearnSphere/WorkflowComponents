"""
Main function to be called for the prediction :-

        train_predict using the given four below inputs.

Modules used :-

        __init__ :-
                new_path: variable that stores current directory

        read_load :-
                f(): function used for the data

Functions :-

        one_hot(data) :- takes data as input and returns the one hot representation
                         using student, skill, opportunity and correctness of that
                         response.
                         also returns the dictionary of userids and their index to
                         separate test and training data from the data matrix.

        train_afm() :- train the logistic regrssion model on liblinear and returns the model.

                inputs :-
                        X_train :- training data in one hot representation
                        Y_train :- target of the training set
                        model_disc - if model is saved on disc
                                     (False / name of model)
                        model_save - if want to save the model
                                     (False / name of model to save)
                output :-
                        prints and returns the accuracy on training and testing data

        load_data() :- loads the training and test set in the one hot representation format using
                        one hot function in this module.

                inputs :-
                        dtype - (train/test/both) which data you wants
                        data - data in terms of dataframe if different from original data.
                        utype - ("" / "sub") user type on which model shuld be trained and tested

                output :-
                        return training rows, testing rows and the pandas dataframe as the train and test set of logistic regression

        save_data() :-
                        l, l1, X, fname, utype=""
                        saves the training and testing data in tha Data/ directory with given fname

        read_data() :-  fname, utype
                        read and returns the training and testing data in tha Data/ directory with given fname

        "make" should be executed in liblinear/python directory
        liblinear should be in "__init__new_path + Data" directory

"""

import random
import pandas as pd
import numpy as np
from collections import defaultdict
import h5py
import sys
import os

from datetime import datetime
from dateutil.relativedelta import relativedelta

def one_hot(data, d_u):

    total_skills = []
    skill_train = list(data["skill_name"])
    multi_skills = 0
    for skill in skill_train:
        if "~~" in skill:
            total_skills.extend(skill.split('~~'))
            multi_skills += 1
        else:
            total_skills.append(skill)

    total_skills = sorted(list(set(total_skills)))
    user_ids = list(data["user_id"])
    d = {j:i for i, j in enumerate(total_skills)}
    u = {j:i for i, j in enumerate(sorted(list(set(user_ids))))}

    skill_onehot = np.zeros([len(data), len(total_skills)])
    opportunity_onehot = np.zeros([len(data), len(total_skills)])
    Y = np.reshape(np.array(list(data["correct"])), (len(data), 1))
    d_t = {}
    s_t = {}
    for i,j in d_u.items():
        if d_u[i] == "test":
            d_t[i] = []
    for i, j in d.items():
        s_t[i] = []

    row = 0
    counter = 0
    opportunity = list(data["Opportunity"])
    for skill, opp in zip(skill_train, opportunity):
        for multi_skill, op in zip(skill.split('~~'), str(opp).split('~~')):
            skill_onehot[row][d[multi_skill]] = 1
            opportunity_onehot[row][d[multi_skill]] = int(op)
            s_t[multi_skill].append(row)
        if d_u[user_ids[row]] == 'test':
            d_t[user_ids[row]].append(counter)
            counter += 1
        row += 1

    l, l1 = [], []
    for row in range(len(user_ids)):
        if d_u[user_ids[row]] == "train":
            l.append(row)
        if d_u[user_ids[row]] == "test":
            l1.append(row)
    X_train = [skill_onehot[l, :], opportunity_onehot[l, :], Y[l, 0]]
    X_test = [(d_t, s_t), skill_onehot[l1, :], opportunity_onehot[l1, :], Y[l1, 0]]
    return X_train, X_test

def load_data(data, user_train, user_test):

    users = set(list(data["user_id"]))
    user_train = sorted(list(users.intersection(set(user_train))))
    user_test = sorted(list(users.intersection(set(user_test))))
    print (len(user_test), len(user_train), len(users))
    d_u = {j:'train' for j in list(user_train)}
    for j in list(user_test):
        d_u[j] = 'test'
    
    X_train, X_test = one_hot(data[data["user_id"].isin(user_train+user_test)], d_u)
    return X_train, X_test

def save_hd5f(fname, dname, data):

    print ("HDF5 Saving Started")
    h5f = h5py.File(fname, 'w')
    h5f.create_dataset(dname, data=data)
    h5f.close()
    print ("HDF5 Saving Done")

def save_data(X_train, X_test, fname, utype=""):

    pd.DataFrame(X_train).to_hdf(__init__.new_path + "Saved/Model/afm/"+fname+"."+utype+"train", "train")
    pd.DataFrame(X_test).to_hdf(__init__.new_path + "Saved/Model/afm/"+fname+"."+utype+"test", "test")

def read_data(fname, utype):

    X_train = pd.read_hdf(__init__.new_path + "Data/afm/"+fname+"."+utype+"train", "train")
    X_test = pd.read_hdf(__init__.new_path + "Data/afm/"+fname+"."+utype+"test", "test")
    return X_train.as_matrix(), X_test.as_matrix()