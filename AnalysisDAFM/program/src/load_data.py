import numpy as np
import pandas as pd
import os, sys
from data.load_data import f
from Representation.dkt import DKT
from Representation.problem2vec import P2V
from Qmatrix.qmatrix import Qmatrix
from AFM.load_data import load_data
from DAFM.load_data import DAFM_data
import pdb

class afm_data_generator():

    def __init__(self, args):

        self.args = args
        self.use_batches = True if self.args.dafm[1] == "Yes" else False

    def create_dict(self):

        representation_obj = {}
        representation_obj['rnn-dense'] = DKT(self.original_data, 'dense','rnn')
        representation_obj['rnn-correct'] = DKT(self.original_data, 'correct','rnn')
        representation_obj['rnn-incorrect'] = DKT(self.original_data, 'incorrect','rnn')
        representation_obj['rnn-correct-incorrect'] = DKT(self.original_data, 'correct-incorrect','rnn')
        # representation_obj['lstm-dense'] = DKT(self.original_data, 'dense','lstm')
        # representation_obj['lstm-correct'] = DKT(self.original_data, 'correct','lstm')
        # representation_obj['lstm-incorrect'] = DKT(self.original_data, 'incorrect','lstm')
        # representation_obj['lstm-correct-incorrect'] = DKT(self.original_data, 'correct-incorrect','lstm')
        representation_obj['w2v-withCorrectness'] = P2V('withCorrectness')
        representation_obj['w2v-withoutCorrectness'] = P2V('withoutCorrectness')
        self.representation_obj = representation_obj

    def generate_representation(self, input_data):

        """ Appending vectors for problem using W2V or RNN """
        data_for_repr = input_data
        repr_object =  self.representation_obj[self.args.representation[0]]
        if self.args.representation[0][:3] == "w2v":
            param = {'ws':int(self.args.w2v_params[1]) , 'vs':int(self.args.w2v_params[0]), 'mt':1, 'mc':0, 'data_path':data_for_repr}
            data_with_representation = repr_object.prob2vec(**param)
        else:
            param = {'activation':'linear', 'hidden_layer_size':int(self.args.rnn_params[0]), 'data':data_for_repr}
            data_with_representation = repr_object.dkt_representation(**param)

        return data_with_representation

    def generate_Xmatrix(self, input_data):

        """ Making Skill Model using problem vectors using clustering requires matlab """
        data_for_qmatrix = input_data
        if self.args.representation[0] is not None:
            qmatrix_obj = Qmatrix(data=data_for_qmatrix, path=self.args.workingDir[0], ctype="kmeans", csize=self.args.clustering_params[0], distance=self.args.clustering_params[1], uid='3')
            qmatrix_obj.problemvector()
            qmatrix = qmatrix_obj.q_matrix()
            X_new_skill = qmatrix_obj.main(self.original_data, qmatrix)
        else:
            X_new_skill = self.original_data
        return X_new_skill

    def generate_dkt(self, input_data):

        dkt_obj = DKT(input_data, "rnn", "rnn")
        input_dim = len(dkt_obj.p)
        input_dim_order = len(dkt_obj.up)
        training_data = input_data[input_data['user_id'].isin(self.user_train)]
        testing_data = input_data[input_data['user_id'].isin(self.user_test)]
        trainX, trainY, trainY_order = dkt_obj.dkt_data(training_data)
        testX, testY, testY_order = dkt_obj.dkt_data(testing_data)
        return [trainX, trainY, trainY_order, testX, testY, testY_order, input_dim, input_dim_order]

    def generate_afm(self, input_data):

        """ Generating data suitable for AFM model """
        data_for_afm = input_data
        x1, x2 = load_data(data_for_afm, self.user_train, self.user_test)
        trainX, trainY = np.concatenate((x1[0], x1[1]), axis=1), x1[2]
        testX, testY = np.concatenate((x2[1], x2[2]), axis=1), x2[3]
        d_t = x2[0][0]
        return [trainX, trainY, testX, testY, d_t]

    def generate_dafm(self, input_data):

        data_for_dafm = input_data
        dafmdata_obj = DAFM_data(args=self.args, complete_data=data_for_dafm, user_train=self.user_train, user_test=self.user_test, df_user_responses=self.df_user_responses, path=self.args.workingDir[0]+self.args.dataset[0], section=self.args.section[0], use_batches=self.use_batches)

        if self.args.skill_wise[0]=="True":
            if (not os.path.exists(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/"+"skill_index.csv")) or (not os.path.exists(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/"+"problem_index.csv")):
                d_skill = dafmdata_obj.d_skill
                skills = []
                index = []
                for i,j in d_skill.items():
                    skills.append(i)
                    index.append(j)
                if not os.path.exists(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/"):
                    os.makedirs(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/")
                pd.DataFrame({"skills":skills, "index":index}).to_csv(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/"+"skill_index.csv", sep=",", index=False)
                d_problem = dafmdata_obj.d_problem
                problems = [i for i,j in d_problem.items()]
                index = [j for i,j in d_problem.items()]
                if not os.path.exists(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/"):
                    os.makedirs(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/")
                pd.DataFrame({"problem":problems, "index":index}).to_csv(self.args.workingDir[0]+self.args.dataset[0]+"/SkillWise/"+"problem_index.csv", sep=",", index=False)

        if self.use_batches:
            return [dafmdata_obj, {'Q_jk_initialize':dafmdata_obj.Q_jk_initialize, 'section_count':dafmdata_obj.section_count, 'student_count': len(dafmdata_obj.d_student)}]
        else:
            trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent = dafmdata_obj.data_generator()
            return  [trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, {'Q_jk_initialize':dafmdata_obj.Q_jk_initialize, 'student_count': len(dafmdata_obj.d_student), 'section_count':dafmdata_obj.section_count}]

    def main(self):


        # original_data, df_user_responses = f(args=self.args, problem_hierarchy=self.args.unit[0], make_unit_users=self.args.unit_users[0])
        original_data, df_user_responses = f(args=self.args, make_unit_users=self.args.unit_users[0])
        self.df_user_responses = df_user_responses

        temp = "" if self.args.puser[0] == "orig" else "sub"
        self.user_train = set(pd.read_csv(self.args.workingDir[0]+"datasets/" +self.args.dataset[0]+"/Users/"+temp+"train.csv", header=None)[0].map(str))
        self.user_test = set(pd.read_csv(self.args.workingDir[0]+"datasets/"+self.args.dataset[0]+"/Users/"+temp+"test.csv", header=None)[0].map(str))
        print ("users:",len(self.user_train), len(self.user_test))

        self.original_data = original_data
        users = set(original_data["user_id"].map(str))
        self.user_train = list(users.intersection(self.user_train))
        self.user_test = list(users.intersection(self.user_test))

        if (not (self.args.representation[0] == None)):
            if "skill_name" not in original_data.columns:
                if type(self.args.clustering_params[0]) == int:
                    self.args.clustering_params[0] = "integer_"+str(self.args.clustering_params[0])
                original_data["skill_name"] = ["^"]*len(original_data)

            print ("Skill Model Using Word2Vec or DKT....")
            self.create_dict()
            data_with_repr = self.generate_representation(input_data=original_data)

        elif ("skill_name" in original_data.columns):
            print ("Skill Model Using ", self.args.skill_name[0], "....")
            data_with_repr = original_data

        else:
            print ("No skill model found")
            sys.exit()

        X_matrix = self.generate_Xmatrix(input_data=data_with_repr)
        if not (self.args.dkt[0]==None):
            print ('DKT loading data ....')
            yield self.generate_dkt(input_data=X_matrix)

        if not (self.args.afm[0]==None):
            print ('AFM loading data ....')
            yield self.generate_afm(input_data=X_matrix)

        if not (self.args.dafm[0]==None):
            print ('DAFM loading data ....')
            dg = self.generate_dafm(input_data=X_matrix)
            yield dg
