"""

Function to split the dataset into train and test set either according to problem or base sequence id or user id

    input :-
        Data - data to splitted
        split_type - the attribute on which data to be splitted
        train_set - the ercentage of training test

    output :-
        return the training and testing dataframe

    "" Please check if train and test set have some intersection problems""
"""
import sys
import pandas as pd
import random

def split(X, split_type="problem", train_set=0.8):

    splitting_type = split_type
    X[splitting_type] = X[splitting_type].map(str)
    total_samples = list(set(list(map(str, list(X[splitting_type])))))
    total_samples = pd.Series(total_samples)
    temp_range = list(range(0, len(total_samples)))
    random.seed(42)
    random.shuffle(temp_range)
    i = temp_range[:int(len(temp_range)*train_set)]
    j = temp_range[int(len(temp_range)*train_set):]
    training = total_samples.iloc[i]
    X_train = X[X[splitting_type].isin(training)]
    testing = total_samples.iloc[j]
    X_test = X[X[splitting_type].isin(testing)]
    return X_train, X_test

if __name__ == "__main__":

    import os
    dataset = sys.argv[1]
    if not os.path.exists('/home/anant/KCModelImprovement/'+dataset+'/Users/'):
        os.makedirs('/home/anant/KCModelImprovement/'+dataset+'/Users/')

    if dataset == "ASSISTments_12_13":
        data_path = '/research/datasets/assistments_2012_2013/2012-2013-data-with-predictions-4-final_with_free_lunch.csv'
    elif dataset == "ASSISTments_09_10":
        data_path = '/research/datasets/assistments_2009_2010/assistments_2009_2010.csv'
    elif dataset == "CognitiveTutor_bal_06_07":
        data_path  = '/research/datasets/cognitive_tutor_2007_2009_kddcup/bridge_to_algebra_2006_2007_train.txt'
    elif dataset == "CognitiveTutor_bal_08_09":
        data_path  = '/research/datasets/cognitive_tutor_2007_2009_kddcup/bridge_to_algebra_2008_2009_train.txt'
    elif dataset == "CognitiveTutor_al_06_07":
        data_path  = '/research/datasets/cognitive_tutor_2007_2009_kddcup/algebra_2006_2007_train.txt'
    elif dataset == "CognitiveTutor_al_08_09":
        data_path  = '/research/datasets/cognitive_tutor_2007_2009_kddcup/algebra_2008_2009_train.txt'
    elif dataset == "Geometry":
        data_path = '/home/anant/datasets/Geometry96-97.txt'
    else:
        print ("Error not valid dataset")
        sys.exit()
    print (dataset)
    if dataset == "ASSISTments_12_13":
        data = pd.read_csv(data_path, sep=",", encoding='ISO-8859-1')
    elif dataset == "ASSISTments_09_10":
        data = pd.read_csv(data_path, sep=",")
    elif dataset == "Geometry":
        data = pd.read_csv(data_path, sep="\t")
    else:
        data = pd.read_csv(data_path, sep="\t")
    if dataset == "ASSISTments_12_13" or dataset == "ASSISTments_09_10":
        train, test = split(data, split_type="user_id")
        pd.Series(list(set(train["user_id"]))).to_csv(dataset + "/Users/train.csv", sep=",", index=False, header=None)
        pd.Series(list(set(test["user_id"]))).to_csv(dataset + "/Users/test.csv", sep=",", index=False, header=None)
        subtrain, subtest = split(train, split_type="user_id")
        pd.Series(list(set(subtrain["user_id"]))).to_csv(dataset + "/Users/subtrain.csv", sep=",", index=False, header=None)
        pd.Series(list(set(subtest["user_id"]))).to_csv(dataset + "/Users/subtest.csv", sep=",", index=False, header=None)
    else:
        train, test = split(data, split_type="Anon Student Id")
        pd.Series(list(set(train["Anon Student Id"]))).to_csv(dataset + "/Users/train.csv", sep=",", index=False, header=None)
        pd.Series(list(set(test["Anon Student Id"]))).to_csv(dataset + "/Users/test.csv", sep=",", index=False, header=None)
        subtrain, subtest = split(train, split_type="Anon Student Id")
        pd.Series(list(set(subtrain["Anon Student Id"]))).to_csv(dataset + "/Users/subtrain.csv", sep=",", index=False, header=None)
        pd.Series(list(set(subtest["Anon Student Id"]))).to_csv(dataset + "/Users/subtest.csv", sep=",", index=False, header=None)
