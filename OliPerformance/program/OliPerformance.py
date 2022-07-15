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


# In[2]:


command_line = True #false for jupyter notebook
problem_file = ""
# transaction_file = ""
workingDir = ""
if command_line:
    #command line
    parser = argparse.ArgumentParser(description='Process datashop file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    args, option_file_index_args = parser.parse_known_args()
    print(args.fileIndex)
    transaction_file = args.fileIndex[0][1]
    problem_file = args.fileIndex[1][1]
    workingDir = args.workingDir
#     print(transaction_file)
    print(problem_file)
else:
    #problems file
    problem_file = "problem.txt"

#     #transactions file
    transaction_file = "trans.txt"
    workingDir = "."


#make sure the output folder exist
outputPath = workingDir + "/final"
if not os.path.exists(outputPath):
    os.makedirs(outputPath)


# ## Number of Learn By Doing (LBD) and Did I Get This (DIGT) Activities per Module
# #### Last Updated 05-25-2022 3:44 pm EST

# In[3]:



#The below call can be used to load multiple files, but it's fine to load a single problem file like this
# file_name = glob.glob("problem.txt")

#Create a dictionary to represent each module in the course
modules = {}

#Create a dataframe of the problem file we read in
df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')

#get transaction file
tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')

#get the first occurence of each problem name ---correspond to oli:purpose

#Grab every unique module in the course and initialize a dictionary based on them.
#The key is the module name
#The value is a list, where the first index into the list will be a dictionary of "Did I Get This" acitivites
#The second index into the list is a dictionary of "Learn by Doing" activities
mods = df['Problem Hierarchy'].unique()

for mod in mods:
    modules[mod] = [{},{},{}]

#We iterate over each row in the dataframe (akin to going down each row in a CSV file)
for i, j in df.iterrows():
    #problem name in each row
    problem_name = j['Problem Name']
    
    #first occurence of the problem name in transaction file
    prob_index = (tx['Problem Name'] == str(problem_name)).idxmax()
    
    #get type
    #prob_type = tx.loc[i]['CF (oli:purpose)']
    #prob_type = tx.iloc[prob_index].loc['CF (oli:purpose)']
    prob_type = tx['CF (oli:purpose)'].iloc[prob_index]
    #print(problem_name)

    #module name in each row
    module_name = j['Problem Hierarchy']

    #Grab the Did I Get This and Learn By Doing dictionaries
    digt = modules[module_name][0]
    lbd = modules[module_name][1]
    none = modules[module_name][2]

    #If the problem name does not mention 'quiz', meaning it's a formative (DIGT or LBD) activity
#     if 'quiz' not in str(prob_type):
        
    if 'didigetthis' in str(prob_type):
        #If we have not come across this DIGT problem in the module before, we add it to the dictionary
        if problem_name not in digt.keys():
            digt[problem_name] = 1
            modules[module_name][0] = digt

    elif 'learnbydoing' in str(prob_type):
        #If we have not come across this LBD problem in the module before, we add it to the dictionary
        if problem_name not in lbd.keys():
            lbd[problem_name] = 1
            modules[module_name][1] = lbd
    else:
        if problem_name not in none.keys():
            none[problem_name] = 1
            modules[module_name][2] = none
            

#This code just loops over our modules dictionary, creates a list of the module name, # of DIGT, # of LBDs and then
#converts it to a dataframe so we can save it as a .csv file
results = []
for key, value in modules.items():
    current_mod = key
    digts = len(modules[current_mod][0])
    lbds = len(modules[current_mod][1])
    none = len(modules[current_mod][2])
    results.append([current_mod.split('module ')[1], digts, lbds, none])

df = pd.DataFrame(results, columns =['Module', 'Did I Get This', 'Learn By Doing', 'Uncategorized'])
df.to_csv(outputPath + "/activities_per_module.csv")


# ## Student Quiz Data per Module

# In[4]:


#The below call can be used to load multiple files, but it's fine to load a single problem file like this
# file_name = glob.glob("problem.txt")

#Create a dataframe of the problem file we read in
df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')

modules = df['Problem Hierarchy'].unique()

length = len(modules)


#Quiz Scores Spreadsheet
# list_of_files = glob.glob("problem.txt")
# modules = ['sequence General Chemistry I, unit Foundations of Chemistry, module Atomic Theory',
#           'sequence General Chemistry I, unit Chemical Reactions, module Chemical Reactions and Equations',
#           'sequence General Chemistry I, unit Measurement, module Measurements',
#           'sequence General Chemistry I, unit Composition of Substances and Solutions, module Aqueous Solutions',
#           'sequence General Chemistry I, unit Reactions and Stoichiometry, module Reaction Stoichiometry']

quiz_data = {}

#These are gathered from the code block above
for module in modules:


        #Our dataframe only consists of Atomic Theory problems now
        at_df = df.loc[module == df['Problem Hierarchy']]

        #Get all student_ids in a list that did atomic theory work
        unique_student_ids = df['Anon Student Id'].unique()

        for student_id in unique_student_ids:
            permod = {}
            s_df = at_df.loc[(df['Anon Student Id'] == student_id)]
            if not s_df.empty:
                atq_df = s_df.loc[at_df['Problem Name'].str.contains('quiz')]
                if not atq_df.empty:
                    attempts = corrects = atq_df['Problem View'].tail(1).values[0]
                    final_score = atq_df['Corrects'].tail(1).values[0]
                    quiz_score = atq_df['Avg Corrects'].tail(1).values[0]
                    first_attempt_correct = atq_df['Corrects'].head(1).values[0]
                    quiz_steps = atq_df['Steps'].tail(1).values[0]
                    if student_id in quiz_data.keys():
                        quiz_data[student_id][module] = [attempts, final_score, quiz_score, first_attempt_correct, quiz_steps]
                    else:
                        permod[module] = [attempts, final_score, quiz_score, first_attempt_correct, quiz_steps]
                        quiz_data[student_id] = permod
                else:
                    if student_id in quiz_data.keys():
                        quiz_data[student_id][module] = [0, 0, 0, 0, 0]
                    else:
                        permod[module] = [0, 0, 0, 0, 0]
                        quiz_data[student_id] = permod
            else:
                if student_id in quiz_data.keys():
                        quiz_data[student_id][module] = [0, 0, 0, 0, 0]
                else:
                    permod[module] = [0, 0, 0, 0, 0]
                    quiz_data[student_id] = permod
    #columns: student ID, prob hierarchy, attempts, final_score, quiz_score, first_attempt_correct, quiz_steps
    #rows

dfquiz = {}
for stdnt, moddata in quiz_data.items():
    c = moddata.copy()
    for mod, data in c.items():
        if stdnt in dfquiz.keys():
            dfquiz[stdnt].extend(data)
        else:
            dfquiz[stdnt] = data
# print(dfquiz)

#make the final dataframe
df_quiz2 = pd.DataFrame(columns=['stuID', 'prob-hierarchy', 'attempts', 'final-score', 'quiz-score', 'first-attempt-correct', 'quiz-steps'])

for k,v in dfquiz.items():
    ph = 1
    for x in range(length):

        L = []
        L.append(k)
        L.append(ph)
#         print(len(v[(x*5):(x*5)+5]))
        L=L+v[(x*5):(x*5)+5]
        df_quiz2.loc[len(df_quiz2)] = L
        ph=ph+1

#df_quiz = pd.DataFrame.from_dict(dfquiz, orient="index")

# df_quiz.columns = ['stuID', 'hierarchy', 'attempts', 'final', 'quiz', 'first', 'steps']

# df_quiz.columns = ['attempts-at', 'final-at', 'grade-at', 'first-at', 'steps-at',
#                   'attempts-cre', 'final-cre', 'grade-cre', 'first-cre', 'steps-cre',
#                   'attempts-m', 'final-m', 'grade-m', 'first-m', 'steps-m',
#                   'attempts-as', 'final-as', 'grade-as', 'first-as', 'steps-as',
#                   'attempts-rs', 'final-rs', 'grade-rs', 'first-rs', 'steps-rs']
df_quiz2.to_csv(outputPath + "/s_student_quiz.csv")


# ## Student Performance on the Formative Assessments per Module

# In[5]:


#Formative Assessments for Chemistry
#The below call can be used to load multiple files, but it's fine to load a single problem file like this
# list_of_files = glob.glob(transaction_file)
# file_name = glob.glob(problem_file)

#Create a dataframe of the problem file we read in
df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')

modules = df['Problem Hierarchy'].unique()

# length = len(modules)

# list_of_files = glob.glob("problem.txt")
# modules = ['sequence General Chemistry I, unit Foundations of Chemistry, module Atomic Theory',
#           'sequence General Chemistry I, unit Chemical Reactions, module Chemical Reactions and Equations',
#           'sequence General Chemistry I, unit Measurement, module Measurements',
#           'sequence General Chemistry I, unit Composition of Substances and Solutions, module Aqueous Solutions',
#           'sequence General Chemistry I, unit Reactions and Stoichiometry, module Reaction Stoichiometry']

#Is there an easy way to identify the learnersourcing problems? Perhaps we use the feedback text

ls_problems = df['Problem Name'].unique()
# ls_problems = ['ls_mcqcb4beaaa2978458980cb3faf0553bab7',
#                'd76305630f0f41f08928febb2c9d5888',
#                'c7266a17a43145eab752c7d9b47ae92d',
#                'e79cceb3c3254239b97d561e8a1137a8',
#                'c4ab99b22c0e48b583b9d6ae423738de']

#These are gathered from the code block above
digts = list(digt.keys())
lbds = list(lbd.keys())
all_data = {}
for problem_file in problem_file:
    #df = pd.read_csv(problem_file,delimiter="\t", index_col='Row')

    for module in modules:
        student_all_act_c_i_s = {}
        quiz_correct_and_attempt = {}
        #Our dataframe only consists of Atomic Theory problems now
        at_df = df.loc[module == df['Problem Hierarchy']]

        #Get all student_ids in a list that did atomic theory work
        unique_student_ids = df['Anon Student Id'].unique()

        for student_id in unique_student_ids:
            permod = {}
            s_df = at_df.loc[(df['Anon Student Id'] == student_id)]
            if not s_df.empty:
                #Here we grab the incorrects, corrects, steps, and first attempt totals for all problems the student did
                at_q_df = s_df.loc[~at_df['Problem Name'].str.contains('quiz')]
                if not at_q_df.empty:
                    incorrects = at_q_df['Incorrects'].sum()
                    corrects = at_q_df['Corrects'].sum()
                    steps = at_q_df['Steps'].sum()
                    correct_first_attempts = at_q_df['Correct First Attempts'].sum()
                    problems_done = len(pd.unique(at_q_df['Problem Name'])) - 1 #Minus 1 to remove the LS activity
                    did_ls = at_q_df.isin(ls_problems).any().any()
                    if student_id in all_data.keys():
                        all_data[student_id][module] = [incorrects, corrects, steps, correct_first_attempts, problems_done, did_ls]
                    else:
                        permod[module] = [incorrects, corrects, steps, correct_first_attempts, problems_done, did_ls]
                        all_data[student_id] = permod
                else:
                    if student_id in all_data.keys():
                        all_data[student_id][module] = [0, 0, 0, 0, 0, False]
                    else:
                        permod[module] = [0, 0, 0, 0, 0, False]
                        all_data[student_id] = permod
            else:
                if student_id in all_data.keys():
                    all_data[student_id][module] = [0, 0, 0, 0, 0, False]
                else:
                    permod[module] = [0, 0, 0, 0, 0, False]
                    all_data[student_id] = permod

#Walk this dictionary to make the desired spreadsheet that contains the total AND broken down by module
dfdata = {}
for stdnt, moddata in all_data.items():
    c = moddata.copy()
    totals = [0,0,0,0,0,0]
    for mod, data in c.items():
        totals[0] += data[0]
        totals[1] += data[1]
        totals[2] += data[2]
        totals[3] += data[3]
        totals[4] += data[4]
        totals[5] += data[5]

        if stdnt in dfdata.keys():
            dfdata[stdnt].extend(data)
        else:
            dfdata[stdnt] = data
    dfdata[stdnt].extend(totals)

# print(dfdata)

df_stu_acts = pd.DataFrame(columns=['stuID', 'prob-hierarchy', 'incorrect', 'correct', 'steps', 'first', 'problems', 'ls'])

for k,v in dfdata.items():
    ph = 1
    for x in range(length):
        L = []
        L.append(k)
        L.append(ph)
        L=L+v[6*x:(6*x)+6]
        df_stu_acts.loc[len(df_stu_acts)] = L
        ph=ph+1


# df_student_acts = pd.DataFrame.from_dict(dfdata, orient="index")
# df_student_acts.columns = ['incorrect-at', 'correct-at', 'steps-at', 'first-at', 'problems-at', 'ls-at',
#                           'incorrect-cre', 'correct-cre', 'steps-cre', 'first-cre', 'problems-cre', 'ls-cre',
#                           'incorrect-m', 'correct-m', 'steps-m', 'first-m', 'problems-m', 'ls-m',
#                           'incorrect-as', 'correct-as', 'steps-as', 'first-as', 'problems-as', 'ls-as',
#                           'incorrect-rs', 'correct-rs', 'steps-rs', 'first-rs', 'problems-rs', 'ls-rs',
#                           'incorrect-total', 'correct-total', 'steps-total', 'first-total', 'problems-total', 'ls-total']
df_stu_acts.to_csv(outputPath + "/student_act.csv")


# In[ ]:




