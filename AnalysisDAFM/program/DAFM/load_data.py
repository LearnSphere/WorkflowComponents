from collections import defaultdict, Counter
import math
import pandas as pd
import numpy as np
import sys
import os
import random
from datetime import datetime

class DAFM_data:

    def __init__(self, args, complete_data, user_train, user_test, df_user_responses=None, path="", section="no", use_batches=False):

        self.range_batch()
        self.bsize = 16
        self.use_batches = use_batches
        self.complete_data = complete_data
        self.path = path
        self.args = args
        self.d_student = []
        if not (self.args.theta[0]=="False"):
            student = sorted(list(set(self.complete_data["user_id"].map(str))))
            self.d_student = {j:i for i, j in enumerate(student)}
        problem_ids = sorted(list(set(self.complete_data["problem_id"])))
        d_problem = {j:i for i,j in enumerate(problem_ids)}
        self.d_problem = d_problem
        self.steps = len(d_problem)

        g = list(self.complete_data.groupby(["user_id"]))
        responses = []
        for i in range(len(g)):
            responses.append(len(g[i][1]))
        self.max_responses = max(responses)
        if section == "concat":
            sections = []
            for skills, section in zip(self.complete_data["skill_name"], self.complete_data["section"]):
                if "~~" in skills:
                    temp = ""
                    for skill in str(skills).split("~~"):
                        temp += skill + "@@" + section + "~~"
                    temp = temp[:-2]
                    sections.append(temp)
                else:
                    sections.append(skills+"__"+section)
            self.complete_data["old_skill_name"] = self.complete_data["skill_name"]
            self.complete_data["skill_name"] = sections
            d_section = []
        elif section == "onehot":
            sections = sorted(list(set(self.complete_data["section"].map(str))))
            d_section = {j:i for i, j in enumerate(sections)}
        else:
            d_section = []
        self.d_section = d_section
        self.section_count = len(self.d_section)
        total_skills = []
        for skills in list(self.complete_data["skill_name"]):
            for skill in str(skills).split("~~"):
                total_skills.append(skill)
        total_skills = sorted(list(set(total_skills)))
        d_skill = {j:i for i, j in enumerate(total_skills)}
        self.d_skill = d_skill
        self.skills = len(total_skills)
        Q_jk_initialize = np.zeros((len(d_problem), len(total_skills)),dtype=np.float32 )
        for problem, skills in zip(self.complete_data["problem_id"], self.complete_data["skill_name"]):
            for skill in str(skills).split("~~"):
                Q_jk_initialize[d_problem[problem], d_skill[skill]] = 1
        self.Q_jk_initialize = Q_jk_initialize

        users = set(list(self.complete_data["user_id"]))
        self.user_train = list(users.intersection(set(user_train)))
        self.user_test = list(users.intersection(set(user_test)))
        self.df_user_responses = df_user_responses
        if (not self.args.item_wise[0]=="False") and (args.puser[0]=="sub"):
            self.complete_data = complete_data[complete_data["user_id"].isin(self.user_train+self.user_test)]
            self.complete_data.index = list(range(len(self.complete_data)))
            total_datapoints = len(self.complete_data)
            items = list(range(total_datapoints))
            self.response_path = self.path + "/log/Responses/"
            if not os.path.exists(self.response_path):
                print ("Creating Response Data")
                os.makedirs(self.response_path)
                random.shuffle(items)
                self.training_items = items[:int(0.8*total_datapoints)]
                self.testing_items = items[int(0.8*total_datapoints):]
                pd.Series(self.training_items).to_csv(self.response_path+"train.csv", sep=",", header=None, index=False)
                pd.Series(self.testing_items).to_csv(self.response_path+"test.csv", sep=",", header=None, index=False)
            else:
                print ("Loading Response Data")
                train_r = pd.read_csv(self.response_path+"train.csv", sep=",", header=None)
                self.training_items = train_r[0].map(int)
                test_r = pd.read_csv(self.response_path+"test.csv", sep=",", header=None)
                self.testing_items = test_r[0].map(int)

    def range_batch(self):

        d = {}
        d['0'], d['1'] = 64, 32
        d['2'], d['3'] = 32, 16
        d['4'], d['5'], d['6'] = 8, 8, 8
        d['7'], d['8'], d['9'], d['10'] = 4, 4, 4, 4
        for i in range(11, 40):
            d[str(i)] = 2
        for i in range(40, 100):
            d[str(i)] = 1
        self.d_batch_response = d

        d = {}
        d['0'], d['1'] = 64, 32
        d['2'], d['3'] = 32, 32
        d['4'], d['5'], d['6'] = 32, 32, 16
        d['7'], d['8'], d['9'], d['10'] = 16, 16, 16, 16
        for i in range(11, 100):
            d[str(i)] = 8
        self.s_batch_response = d

    def onehot(self, data_onehot, d_problem, max_responses, d_section=[], d_student=[], ttype="train"):

        data_train = data_onehot
        train_users = sorted(list(set(data_train["new_id"])))
        d_train_users = {j:i for i, j in enumerate(train_users)}
        d_response_counter = {j:0 for i, j in enumerate(train_users)}

        if not (d_section == []):
            x_train_section = np.zeros((len(d_train_users), max_responses, len(d_section)))
            section_list = data_train["section"]
        else:
            x_train_section = []
            section_list = [0]*len(data_train)
        if not (d_student == []):
            x_train_student = np.zeros((len(d_train_users), max_responses, len(d_student)))
            student_list = data_train["new_id"]
        else:
            x_train_student = []
            student_list = [0]*len(data_train)

        x_train = np.zeros((len(d_train_users), max_responses, len(d_problem)), dtype=np.uint8)
        y_train = -np.ones((len(d_train_users), max_responses, 1), dtype=np.int8)
        count = 0
        count_test = 0
        count_train = 0
        for user, problem, correct, section in zip(data_train["new_id"], data_train["problem_id"], data_train["correct"], section_list):
            if d_response_counter[user] < max_responses:
                x_train[d_train_users[user], d_response_counter[user], d_problem[problem]]=1
                if self.args.puser[0] == "sub" and (not self.args.item_wise[0] == "False") and ttype=="train" and (not (count in list(self.training_items))):
                    y_train[d_train_users[user], d_response_counter[user], 0] = -1
                    count_test += 1
                else:
                    y_train[d_train_users[user], d_response_counter[user], 0]=int(correct)
                    count_train += 1
                if not (d_section == []):
                    x_train_section[d_train_users[user], d_response_counter[user], d_section[section]]=1
                if not (d_student == []):
                    x_train_student[d_train_users[user], d_response_counter[user], d_student[user]]=1
                d_response_counter[user] += 1
            count += 1
        if ((self.args.puser[0]=="orig" or self.args.theta[0]=="valnohot") and (not self.args.theta[0] == "False") and ttype=="test"):
            x_train_student = np.zeros((len(d_train_users), max_responses, len(d_student)))
        return x_train, y_train, x_train_section, x_train_student

    def data_generator1(self, request):

        data = self.complete_data
        if self.use_batches:
            if request == "train":
                self.save_batches(self.user_train, batch_type="train")
            elif request == "test":
                self.save_batches(self.user_test, batch_type="test")
            else:
                print ("Error!")
            self.fname = self.args.fname+"$"+self.args.dafm[0] + "$" + self.args.dense_size[0]
            #'$'.join([self.args.dafm[0], self.args.dense_size[0], self.args.dafm_params[0], self.args.dafm_params[1], str(self.args.dafm_params[2])])
            request += "$"+self.fname
            files_list = os.listdir(self.batch_path+request)
            files_list = list(map(lambda x:self.batch_path+request+"/"+x, files_list))
            if request == "train":
                random.shuffle(files_list)

            for file in files_list:
                users = pd.read_csv(file, header=None)[0]
                t_data = data[data["new_id"].isin(users.map(str))]
                gpu = True
                bsize = self.bsize
                if not gpu:
                    x, y, x_s, x_student = self.onehot(data_onehot=t_data, d_problem=self.d_problem, max_responses=int(file.split('_')[-1]), d_section=self.d_section, d_student=self.d_student, ttype=request)
                    batch_size = 16
                else:
                    x, y, x_s, x_student = self.onehot(data_onehot=t_data, d_problem=self.d_problem, max_responses=100*(int(file.split('_')[-1])+1), d_section=self.d_section, d_student=self.d_student, ttype=request)
                    batch_size = self.s_batch_response[file.split('_')[-1]]
                yield x, y, x_s, x_student, batch_size

    def data_generator(self):

            data = self.complete_data
            if self.args.puser[0] == "orig":
                training_data = data[data["user_id"].isin(self.user_train)]
                testing_data = data[data["user_id"].isin(self.user_test)]
            else:
                if self.args.item_wise[0] == "False":
                    training_data = data[data["user_id"].isin(self.user_train)]
                    testing_data = data[data["user_id"].isin(self.user_test)]
                else:
                    training_data = self.complete_data
                    testing_data = data.iloc[self.testing_items][:]
            if self.args.skill_wise[0] == "True":
                x_train, y_train, x_train_section, x_train_student = [], [], [], []
            else:
                x_train, y_train, x_train_section, x_train_student = self.onehot(data_onehot=training_data, d_problem=self.d_problem, max_responses=self.max_responses, d_section=self.d_section, d_student=self.d_student)

            x_test, y_test, x_test_section, x_test_student = self.onehot(data_onehot=testing_data, d_problem=self.d_problem, max_responses=self.max_responses, d_section=self.d_section,d_student=self.d_student, ttype="test")
            print ("Shapes of the training and test items in the order x, y, x_section, x_student:", np.shape(x_train), np.shape(y_train), np.shape(x_train_section), np.shape(x_train_student), np.shape(x_test), np.shape(y_test), np.shape(x_test_section), np.shape(x_test_student))
            return x_train, y_train, x_train_section, x_train_student, x_test, y_test, x_test_section, x_test_student

    def save_batches(self, users, batch_type="train"):

        self.batch_path = self.path + "/log/Batches/"
        self.fname =self.args.fname+"$"+self.args.dafm[0] + "$" + self.args.dense_size[0]
        # '$'.join([self.args.dafm[0], self.args.dense_size[0], self.args.dafm_params[0], self.args.dafm_params[1], str(self.args.dafm_params[2])])
        batch_path = self.batch_path + batch_type+"$"+self.fname
        import shutil
        shutil.rmtree(batch_path, ignore_errors=True)

        os.makedirs(batch_path)
        df = self.df_user_responses
        def f(x):
            return x.split("@")[0]
        df["user_id"] = df["users"].map(f)
        df = df[df["user_id"].isin(users)]
        users_responses = dict(zip(df["users"].map(str), df["responses"].map(int)))
        d_cat_batch = defaultdict(list)
        for i, j in users_responses.items():
                d_cat_batch[str(int(j//100))].append(i)
        d_cat_batch = dict(d_cat_batch)
        gpu = True
        if gpu:
            for i, j in d_cat_batch.items():
                max_user_length = self.s_batch_response[i]
                b = max_user_length
                for k, _ in enumerate(range(math.ceil(len(j)/b))):
                    pd.Series(j[ k*b : (k+1) * b ]).to_csv(batch_path+"/batch"+str(k)+"_"+i, sep=",", index=False, header=None)
        else:
            import operator
            user_sorted = sorted(users_responses.items(), key=operator.itemgetter(1))
            bsize = self.bsize
            for i, j in enumerate(range(0, len(user_sorted)+bsize, bsize)):
                users = list(map(lambda x: x[0], list(user_sorted[j:j+bsize])))
                if not (len(users) == 0):
                    max_responses = list(user_sorted[j:j+bsize][-1])[1]
                    pd.Series(users).to_csv(batch_path+"/batch"+str(i)+'_'+str(max_responses), sep=",", index=False, header=None)
