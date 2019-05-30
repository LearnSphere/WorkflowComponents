import pandas as pd
import numpy as np
import sys
from sklearn.decomposition import PCA

class DPMEANS:
    def __init__(self, londa, max_iter=1):
        self.londa = londa
        self.max_iter = max_iter

    def fit(self, data):

        self.u = {}
        self.z = {}
        self.k = 1
        self.data_index = {}
        for i in range(len(data)):
                self.z[i] = 1

        self.u[0] = np.mean(data, axis=0)


        for i in range(1, self.max_iter+1):

                self.l = {}
                self.s_l = {}
                for index in range(len(data)):
                    self.data_index[index] = data[index]
                    #euclidian distance
                    distances = [np.linalg.norm(data[index]-self.u[c]) for c in range(self.k)]
                    distances_array = np.array(distances)

                    if min(distances) > self.londa:

                        self.k = self.k+1
                        self.z[index] = self.k
                        self.u[self.k-1] = data[index]

                    else:
                        self.z[index] = distances_array.argmin()+1

                    distances.clear()

                # the output  here is k and the clusters indexes
                # Generate the clusters
                for j in range(1, self.k+1):
                    list = []
                    list_s = []
                    for i in range(len(data)):
                        if self.z[i] == j:
                            list.append(data[i])
                            list_s.append(i)

                    self.l[j] = list
                    self.s_l[j] = list_s

                #Compute the centroids of the results clusters:

                self.u[j-1] = 1/len(self.l[j]) * sum(self.l[j], 0)


if __name__ == '__main__':

    # Read data from the excel file
    df = pd.read_excel(sys.argv[1])
    #df = sys.argv[1]
    #print("type of mydata passed as arg: ", type(df))   


    # extract the students ids as a list
    ids = pd.read_excel(sys.argv[1], usecols = "A")
    
    student_ids = ids.values.tolist()
    

    data = df.iloc[:,1:36].values

    #df_score =df.Posttest  
    #data_score = df_score.values
    
    students_ids = {}
    students_posttest = {}
    student_pretest = {}
    scores_index = {}

    #for i in range(len(data_score)):
        #students_ids[i] = student_ids[i]
        #students_posttest[i] = data_score[i]

    #df_pretest = df.Prestest 
    #pretest_score = df_pretest.values

    #for i in range(len(pretest_score)):
        #student_pretest[i]=pretest_score[i]

    #print(data)
    pca = PCA(n_components=24).fit(data)
    X = pca.transform(data)
   
    lambada = int(sys.argv[2])   
    iterations = int(sys.argv[3])
    
    
    DF = DPMEANS(lambada, iterations)
    DF.fit(X)

    #scores1 = []

    #for item in DF.s_l[1] :
        #print(students_posttest[item])
        #scores1.append(students_posttest[item])

    #scores_1_index={}
    #for j in range(1, len(DF.s_l)+1):
        #scores = []
        #scores_1=[]
        #for item in DF.s_l[j]:
           #scores.append(students_posttest[item])
           #scores_1.append(student_pretest[item])
        #scores_index[j] = np.mean(scores)
        #scores_1_index[j]=np.mean(scores_1)

    students_clusters = {}
    for item in DF.s_l:
       print(item , DF.s_l[item])
       for s in DF.s_l[item] :
           students_clusters[s]=item

    data_res=[]
    
    for i in range(len(student_ids)):
        l=[]
        l.append(student_ids[i][0].strip(":"))
        l.append(students_clusters[i])
        #l.append(students_posttest[i])
        #l.append(student_pretest[i])
        data_res.append(l)
    
    df_result = pd.DataFrame(data_res,columns=['ID', 'Cluster'])
    df_result.to_csv(sys.argv[4], header=True, index=False, sep='\t')