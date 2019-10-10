"""

order of calling

problemvector()
qmatrix()
X = X_matrix()


to only save missing skill problems:-       python read_load.py True True multiple False CognitiveTutor False False True False
to save users train and sub train sets:-    python read_load.py False True multiple False CognitiveTutor False split False subsplit
                                            python main.py

"""
import sys
import pandas as pd
import numpy as np
import os
import pdb
from scipy import io

from collections import Counter
from itertools import product
import ast
# import matlab.engine
from collections import defaultdict

class Qmatrix:

    def __init__(self, data, path, csize, ctype, distance, uid, qsave=False):

        self.qsave = qsave
        self.csize, self.ctype, self.distance = csize, ctype, distance
        self.uid = uid
        self.path = path
        self.data = data

    def q_matrix(self):

        csize, ctype, distance = self.csize, self.ctype, self.distance
        uid = self.uid
        d = {}
        d['half'], d['same'], d['double'], d['samem10'], d['samep10'], d['integer'] = lambda k:int(k/2), lambda k:k, lambda k: 2*k, lambda k:int (k/2-k/10), lambda k: int(k/2+k/10), lambda k: k
        if ctype == "kmeans":
            try:
                import matlab.engine
                print ("Clustering using matlab")
                if distance == "euclidean":
                    distance = 'sqeuclidean'
                else:
                    distance = 'cosine'
                data_path = self.path + 'log/'+str(uid)+'.mat'
                eng = matlab.engine.start_matlab()
                data = eng.load(data_path);
                eng.double(data['x'])
                z = eng.kmeans(eng.double(data['x']), d[csize.split('_')[0]](data['k']), 'distance', distance);
                labels = np.array(z, dtype=np.int32)
                labels = labels[:, 0]
            except:
                from sklearn.cluster import KMeans
                print ("Clustering using sklearn and using euclidean with cluster size as: ", d[csize.split('_')[0]](self.k), csize, self.k)
                kmeans = KMeans(n_clusters=d[csize.split("_")[0]](self.k))
                kmeans.fit(list(self.data['vector']))
                labels = kmeans.labels_

        problem_path = self.path +'log/'+uid+'.pro'
        problems = pd.read_csv(problem_path, sep=",", header=None)
        problems = list(problems[0].map(str))
        qmatrix = dict(zip(problems, list(map(str, list(labels)))))
        return qmatrix

    def total_skills(self, skill_train):

        total_skills, multi_skills = [], 0
        for skill in skill_train:
            if "~~" in skill:
                total_skills.extend(skill.split('~~'))
                multi_skills += 1
            else:
                total_skills.append(skill)
        return len(set(total_skills))

    def problemvector(self):

        data = self.data
        data_path = self.path + 'log/'+self.uid+'.mat'
        problem_path = self.path + 'log/'+self.uid+'.pro'
        if not os.path.exists(self.path+'log/'):
            os.makedirs(self.path+'log/')
        k = int(self.csize.split('_')[1]) if self.csize.split('_')[0]=="integer" else self.total_skills(list(data["skill_name"]))
        self.k = k
        io.savemat(data_path, dict(x=list(data['vector']), k=k))
        pd.Series(data["problem_id"]).to_csv(problem_path, sep=",", index=False, header=None)

    def opportunity_count(self, X):
        if 'First Transaction Time' in X.columns:
            X = X.sort_values(by=['user_id', 'First Transaction Time'], ascending = [True, True])
        skills = list(set(X['skill_name']))
        opportunity = []
        ppo = []
        user_ids = list(X["user_id"])
        for k, i in enumerate(list(X["skill_name"])):
            if k!=0:
                if user_ids[k] != user_ids[k-1]:
                    d = {j:0 for j in skills}
            else:
                d = {j:0 for j in skills}
            d[i] += 1
            opportunity.append(str(d[i]))
            ppo.append(d[i])
        X["Opportunity"] = opportunity
        return X

    def main(self, X, qmatrix):

        X["old_skill_name"] = X["skill_name"]
        if "Opportunity" in X.columns:
            X["old_Opportunity"] = X["Opportunity"]
        l = []
        k = []
        for i in qmatrix.keys():
            k.append(i)
        for i in list(X['problem_id']):
            if i in k:
                l.append(str(qmatrix[i]))
            else:
                l.append('')
        X["skill_name"] = l
        X = self.opportunity_count(X)
        return X
