#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import csv
import os
import glob
import numpy as np
import os
from os import listdir
from os.path import isfile, join
import re
import argparse
import sys


# In[76]:


command_line = True #false for jupyter notebook
problem_file = ""
transaction_file = ""
workingDir = ""
if command_line:
     #command line
    parser = argparse.ArgumentParser(description='Process datashop file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    
    parser.add_argument("-get_demographic_name", type=str, help='the problem name for demographic questions')
    parser.add_argument("-get_confidence_name", type=str, help='the problem name for confidence questions')
    
    args, option_file_index_args = parser.parse_known_args()

    transaction_file = args.fileIndex[0][1]
    problem_file = args.fileIndex[1][1]
    workingDir = args.workingDir
    
    tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')
    demoName = args.get_demographic_name
    if(tx['Problem Name'].str.contains(str(demoName)).any() == False):
    	demoName = "Invalid"
    confName = args.get_confidence_name
    if(tx['Problem Name'].str.contains(str(confName)).any() == False):
    	confName = "Invalid"
    print(tx['Problem Name'].str.contains(str(demoName)).any())
    print(confName)
    
#     tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')
#     if(tx['Problem Name'].str.contains(str(demoName)).any() == False):
#         demoName = "Invalid"
#     if(tx['Problem Name'].str.contains(str(confName)).any() == False):
#         confName = "Invalid"
    
else:
    #problems file
    problem_file = "problem.txt"

    #transactions file
    transaction_file = "trans.txt"
    workingDir = "."
    
    demoName = "ls_demographicd1f2ba108dea44c89042afb763889f17"
    confName = "xxxnewfcebdb97ba0548ffa72d13b2c5d83285"
    tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')
    if(tx['Problem Name'].str.contains(str(demoName)).any() == False):
        demoName = "Invalid"
    if(tx['Problem Name'].str.contains(str(confName)).any() == False):
        confName = "Invalid"


# ## Demographic Survey Results

# In[77]:


#check if demoName is a valid problem name
tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')

# for col in tx.columns:
#     print(col)

tx_subset = tx.loc[(tx['Problem Name'] == demoName)]

stp_names = list(tx_subset['Step Name'])

student_ids = set()

    #{stpname: {}, }
student_demos = {}
if(demoName != "Invalid" and demoName != ""):
    for name in stp_names:
        student_demos[name] = {}

    for problem_file in glob.glob(transaction_file):
        df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')


        for i, j in df.iterrows():
            student_id = j['Anon Student Id']
            student_ids.add(student_id)

            for name in stp_names:
                if name in str(j['Step Name']) and str(j['Is Last Attempt']) == '1.0':
                    if("<material>" in j['Input']):
                        student_demos[name][student_id]= re.findall(r'>(.*?)<', j['Input'])[0]
                    else:
                        student_demos[name][student_id]=j['Input']
else:
    for problem_file in glob.glob(transaction_file):
        df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')
        for i, j in df.iterrows():
                student_id = j['Anon Student Id']
                student_ids.add(student_id)
print(student_ids)
keyset = set()
for k in student_demos:
    keyset.update(student_demos[k])

for k in student_demos:
    for kk in keyset:
        student_demos[k].setdefault(kk, 0)
    


# ## Confidence Survey Result

# In[78]:


tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')

tx_subset = tx.loc[(tx['Problem Name'] == confName)]

conf_names = list(tx_subset['Step Name'])

dataset_name = tx['Level (Sequence)'].unique()[0]
dataset_num = tx['Level (Module)'].unique()[0]

student_answers = {}
student_times = {}
titles = []

if (confName != "Invalid"):
    for problem_file in glob.glob(transaction_file):
        df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')

        for i, j in df.iterrows():
            if str(j['Step Name']) in conf_names and str(j['Is Last Attempt']) == '1.0'and isinstance(j['Input'],str) and "<material>" in j['Input']:
                answer = re.findall(r'>(.*?)<', j['Input'])[0]
                student_id = j['Anon Student Id']
                time = j['Time']
                problem = j['Problem Name']
                if(str(j['Step Name']) not in titles):
                    titles.append(str(j['Step Name']))
                if student_id in student_answers.keys():
                    student_answers[student_id].append(answer)
                    student_times[student_id].append(time)

                else:
                    student_answers[student_id] = [answer]
                    student_times[student_id] = [time]
    titles.append('Student Times')

confidences = {}
for sid in student_ids:
    k = sid
    if k in student_answers.keys() or k in keyset:
        confidences[k] = []
        if k in student_answers.keys():
            confidences[k].extend(student_answers[k])
            confidences[k].append(student_times[k][0])
#         else:
#             confidences[k].extend(['NA', 'NA', 'NA', 'NA', 'NA'])
#             confidences[k].append('NA')
        confidences[k].append(dataset_name) #dataset name
        confidences[k].append(dataset_num) #dataset number

        for x in student_demos:
            confidences[k].append(student_demos[x].get(k))
    
if(demoName != "Invalid" or confName != "Invalid"):
    titles.append('Dataset Name')
    titles.append('Dataset Topic')
    for x in student_demos:
        titles.append(x)
print(titles)
print(confidences)
print(demoName)
if (demoName != "Invalid") or (confName != "Invalid"):
	final_df = pd.DataFrame.from_dict(confidences, orient="index")
	final_df.columns = titles
	final_df.to_csv("Demographic_and_Confidence_Survey.csv")


# In[ ]:





# In[ ]:




