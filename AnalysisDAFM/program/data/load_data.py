import pandas as pd
import sys, os
import random

def preprocessing(dataset_path, columns_list):

    if os.path.exists(os.getcwd()+'/'+ "datasets/"+dataset_path):
        dataset_path = os.getcwd()+'/'+ "datasets/"+dataset_path
    elif os.path.exists(dataset_path):
        pass
    else:
        print (dataset_path, " does not exists")
        sys.exit()

    print ("Data is loaded from:", dataset_path)
    data = pd.read_csv(dataset_path, sep='\t')

    ## any preprocessing can de done here

    data = data[columns_list]
    return data

def split(X, split_type="user_id", train_set=0.8):

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

"""Concatenate unit and users to increase number of samples instead of responses in single rnn"""
def concat_unit_users(data, make_unit_users="No"):

        if make_unit_users == "all":

            data["new_id"] = data["user_id"] + "@" + data["Unit"]
            g = list(data.groupby("new_id"))
            responses = []
            users = []
            for i in range(len(g)):
                users.append(g[i][0])
                responses.append(len(g[i][1]))
            df_user_response = pd.DataFrame({"responses":responses, "users":users})

        elif make_unit_users == "No":
            data["new_id"] = data["user_id"]
            g = list(data.groupby("new_id"))
            responses = []
            users = []
            for i in range(len(g)):
                users.append(g[i][0])
                responses.append(len(g[i][1]))
            df_user_response = pd.DataFrame({"responses":responses, "users":users})

        else:
            u_data = data[data["Unit"].isin([make_unit_users])]
            users = list(set(u_data["user_id"]))
            data = data[data["user_id"].isin(users)]
            data["new_id"] = data["user_id"] + "@" + data["Unit"]
            g = list(data.groupby("new_id"))
            responses = []
            users = []
            for i in range(len(g)):
                users.append(g[i][0])
                responses.append(len(g[i][1]))
            df_user_response = pd.DataFrame({"responses": responses, "users": users})
        return data, df_user_response

def train_test_user(data, args):

    train_test_path = args.workingDir[0] + "datasets/" + args.dataset[0]+ "/Users/"
    if not os.path.exists(train_test_path):
        os.makedirs(train_test_path)
    if not os.path.exists(train_test_path + "train.csv"):
        train, test = split(data, split_type = 'user_id')
        pd.Series(list(set(train["user_id"]))).to_csv(train_test_path + "train.csv", sep=",", index=False, header=None)
    if not os.path.exists(train_test_path + "test.csv"):
        pd.Series(list(set(test["user_id"]))).to_csv(train_test_path + "test.csv", sep=",", index=False, header=None)

    if not os.path.exists(train_test_path + "subtrain.csv"):
        subtrain, subtest = split(train, split_type="user_id")
        pd.Series(list(set(subtrain["user_id"]))).to_csv(train_test_path + "subtrain.csv", sep=",", index=False, header=None)
    if not os.path.exists(train_test_path + "subtest.csv"):
        pd.Series(list(set(subtest["user_id"]))).to_csv(train_test_path + "subtest.csv", sep=",", index=False, header=None)

def f(args, make_unit_users="No"):

    columns_dict = {args.user_id[0]: 'user_id', args.problem_id[0]: 'problem_id', \
                    args.skill_name[0]: 'skill_name', args.correctness[0]: 'correct', \
                    args.unit[1]: 'unit', args.section[1]: 'section'}
    del columns_dict[None]
    data = preprocessing(args.data_path, list(columns_dict.keys()))
    data.rename(columns=columns_dict, inplace=True)
    train_test_user(data, args)
    data, df_user_response = concat_unit_users(data, make_unit_users)
    return data, df_user_response
