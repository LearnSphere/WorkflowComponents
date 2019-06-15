import pandas as pd
import numpy as np
import sys
from sklearn.decomposition import PCA
from sklearn.preprocessing import LabelEncoder
from scipy import stats

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
    pd.options.mode.chained_assignment = None
  
    if sys.argv[1] == "wide":
      extension= sys.argv[2].rpartition('.')[-1]
      
      if extension == "xlsx" :
     
        # Read data from the excel file
        df = pd.read_excel(sys.argv[2])
   
        # extract the students ids as a list
        ids = pd.read_excel(sys.argv[2], usecols = "A")
    
        student_ids = ids.values.tolist()    

        data = df.iloc[:,1:36].values

        #df_score =df.Posttest  
        #data_score = df_score.values
    
        students_ids = {}
        students_posttest = {}
        student_pretest = {}
        scores_index = {}

        
        #print(data)
        pca = PCA(n_components=24).fit(data)
        X = pca.transform(data)
   
        lambada = float(sys.argv[3])   
        iterations = int(sys.argv[4])    
    
        DF = DPMEANS(lambada, iterations)
        DF.fit(X)     

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
        df_result.to_csv(sys.argv[5], header=True, index=False, sep='\t')
    
      if extension == "txt" :
        df_origin = pd.read_table(sys.argv[2])
        df = df_origin.drop(['Cluster '], axis=1)

        LE = LabelEncoder()
        df['Student Id'] = LE.fit_transform(df['student_means[, 1]'])
        #print(df)
        ids = {}
        for index, row in df.iterrows():
           ids[row['Student Id']] = row['student_means[, 1]']


        df_clustering=df.drop(['student_means[, 1]'],axis=1)
        pca = PCA(n_components=2).fit(df_clustering)
        X = pca.transform(df_clustering)
   
        lambada = float(sys.argv[3])   
        iterations = int(sys.argv[4])    
    
        DF = DPMEANS(lambada, iterations)       
        DF.fit(X)

        students_clusters = {}
        for key, value in DF.s_l.items():
           for item in value:
              students_clusters[ids[item]] = key

        clusters = []
        for row in df_origin['student_means[, 1]'].items():
            clusters.append(students_clusters[row[1]])


        students =df['student_means[, 1]']
        list_of_tuples = list(zip(students, clusters))
        df_result = pd.DataFrame(list_of_tuples, columns=['Student', 'Cluster'])
        df_result.to_csv(sys.argv[5], header=True, index=False, sep='\t', mode='a')

    if sys.argv[1] == "long":
        df_origin = pd.read_table(sys.argv[2])
        df= df_origin[['Anon Student Id', 'KC (Default)', 'Duration (sec)', 'Outcome']]

        df.loc[df.Outcome != 'CORRECT', 'CORRECT'] = 0
        df.loc[df.Outcome == 'CORRECT', 'CORRECT'] = 1

        df.rename(columns={'CORRECT': 'outcome'}, inplace=True)

       #How to pre-process based on the parameters : mean or median - isdu

        LE=LabelEncoder()
        df['Student Id'] = LE.fit_transform(df['Anon Student Id'])

        LE1=LabelEncoder()
        df['model'] = LE1.fit_transform(df['KC (Default)'])

        ids={}
        row_index ={}
        for index, row in df.iterrows():
            ids[row['Student Id']] = row['Anon Student Id']
            row_index[index]=row['Student Id']

        df_for_clustering = df[['Student Id','model','Duration (sec)','outcome']]
     
        isoutcome = sys.argv[6]
        isduration =sys.argv[7]
        mean_or_median = sys.argv[8]
        np_components =0        
        
        if  isoutcome=="yes" and isduration=="no":
            if mean_or_median =="mean" :
                df_for_clustering=df_for_clustering.groupby('Student Id', as_index=False).agg({"outcome": "mean"})
                np_components = 2

            if mean_or_median =="median" :
                df_for_clustering=df_for_clustering.groupby('Student Id', as_index=False).agg({"outcome": "median"})
                np_components = 2

            istudentid="yes"


        if isoutcome == "yes" and isduration == "yes":
               df1=df_for_clustering.groupby(['Student Id'], as_index=False).agg({"outcome": "mean"})
               df1['outcome']= stats.zscore(df1['outcome'])

               df2=df_for_clustering.groupby(['Student Id'], as_index=False).agg({"Duration (sec)": "mean"})
               df2['Duration (sec)'] = stats.zscore(df2['Duration (sec)'])
               df_for_clustering =pd.merge(df1, df2, on='Student Id')

               np_components=3

        pca = PCA(n_components=np_components).fit(df_for_clustering)
        X = pca.transform(df_for_clustering)

        lambada = float(sys.argv[3])
        DF = DPMEANS(lambada, 10)
        DF.fit(X)     
      
        students_clusters={}
        for key, value in DF.s_l.items():
            for item in value :
                  students_clusters[ids[item]]=key
       
        clusters=[]
        for row in df_origin['Anon Student Id'].items():
            clusters.append(students_clusters[row[1]])

        df_result = df_origin
        df_result.insert(loc=4, column='Cluster', value=clusters,allow_duplicates=False)
        df_result.to_csv(sys.argv[5], header=True, index=False, sep='\t', mode='a')

       
        