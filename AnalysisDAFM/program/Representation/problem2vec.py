"""
*prob2vec*

Function to create a dataframe with embedding vectors associated with every problem depending on the value of min_count.
Also, used to generate total number of problems associated with min_count and representation type.

                Input :- 
                    ws, vs, mt, mc - window_size, vector_size, model_type (always 1 for skip-gram), min_count.
                    repr_type - 'withCorrectness / withoutCorrectness'

                    data_path - (dataframe / datapath from which data can be readed of csv file)
                    data_name - (CognitiveTutor / ASSIStments)

                    save - if want to save the model. (True / False)
                    model_path - ("NA" / absolute path where to save the model)

                    saved - if model is already saved or not (True / False)
                    model_path - where the model is saved

                    model_path is till the path of directory only. file is decided on the basis of w2v parameters.

                    tp_save - if want to save the total number of problems associated with mc, repr_type(True / False)
                    tp_path - ("NA" / absolute path where to save the total problems)

                Output :-
                    X : Dataframe with four columns:-
                        problem_id
                        base_sequence_id
                        skill_name
                        vector - list of list and need to covert string to list to use the embedding vectors.

"""

import pandas as pd
import gensim
import os
from collections import defaultdict
class P2V:
    
    def __init__(self, repr_type):
        
        self.repr_type = repr_type

    def prob2vec(self, ws, vs, mt, mc, data_path, model_path="NA", saved=False, save=False, tp_save=False, tp_path="NA", data_name="CognitiveTutor"):
        
        repr_type = self.repr_type
        if not os.path.exists(tp_path) and tp_path != "NA":
            os.makedirs(tp_path)
        if not os.path.exists(model_path) and model_path != "NA":
            os.makedirs(model_path)
        if type(data_path) == str:
            try:
                if data_name == "CognitiveTutor":
                    data = pd.read_csv(data_path, sep="\t")
                else:
                    data = pd.read_csv(data_path, sep=",")
            except:
                print ("Data Path is not right", data_path)
                return
        else:
            data = data_path
        d = dict(zip(data["problem_id"].map(str), data["skill_name"].map(str)))
        d3 = dict(zip(data["problem_id"].map(str), data["base_sequence_id"].map(str)))
        if repr_type == "withCorrectness":
            def f(x):
                return str(int(x))
            if saved == True:
                fname = model_path+"/"+str(mc)+"$"+str(ws)+"$"+str(vs)
                model = gensim.models.Word2Vec.load(fname)
            else:
                data["problem_id"] = data["problem_id"].map(str) + data["correct"].map(f)
                data1 = data.iloc[:][["user_id", "problem_id"]]
                g = list(data1.groupby(["user_id"]))
                userToprob = {}
                userToprob = {list(g[i])[0]:list(map(str, list(list(g[i])[1]["problem_id"]))) for i in range(len(g))}
                model = gensim.models.Word2Vec(userToprob.values(), window=ws, size=vs, sg=mt, seed=1234, min_count=mc, workers=20, iter=30)
                if save == True:
                    fname = model_path+"/"+str(mc)+"$"+str(ws)+"$"+str(vs)
                    model.save(fname) 
    
            prob2vec = {}
            prob2vec = {i:model[i] for i in model.wv.vocab.keys()}
            l = list(set(list(map(lambda s:str(s[:-1]), list(prob2vec.keys())))))
            new_data = [[i, list(prob2vec[i+'0']) + list(prob2vec[i+'1']), d[i], d3[i]] for i in list(l) if ((i+'0' in prob2vec) and (i+'1' in prob2vec))]
    
        else:
            if saved == True:
                fname = model_path+"/"+str(mc)+"$"+str(ws)+"$"+str(vs)
                model = gensim.models.Word2Vec.load(fname)
            else:
                data1 = data.iloc[:][["user_id", "problem_id"]]
                g = list(data1.groupby(["user_id"]))
                userToprob = {}
                userToprob = {list(g[i])[0]:list(map(str, list(list(g[i])[1]["problem_id"]))) for i in range(len(g))}
                model = gensim.models.Word2Vec(userToprob.values(), window=ws, size=vs, sg=mt, seed=1234, min_count=mc, workers=20, iter=30)
                if save == True:
                    fname = model_path
                    model.save(fname)
            new_data = [[i, list(model[i]), d[i], d3[i]] for i in model.wv.vocab.keys()]
        new_dataa = [[], [], [], []]
        for i in range(len(new_data)):
            new_dataa[0].append(new_data[i][0])
            new_dataa[1].append(new_data[i][1])
            new_dataa[2].append(new_data[i][2])
            new_dataa[3].append(new_data[i][3])
        new_data = new_dataa
        X = pd.DataFrame({"problem_id": new_data[0], "vector": new_data[1],\
                          "skill_name": new_data[2], "base_sequence_id" : new_data[3]}) 
    
        if tp_save and repr_type=="withCorrectness":
            X["problem_id"].to_csv(tp_path+"C"+str(mc)+".csv", sep=",", index=False)
        elif tp_save: 
            X["problem_id"].to_csv(tp_path+"W"+str(mc)+".csv", sep=",", index=False)
        return X
