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
transaction_file = ""
workingDir = ""
if command_line:
    #command line
    parser = argparse.ArgumentParser(description='Process datashop file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    
    args, option_file_index_args = parser.parse_known_args()
   
    activity_file = args.fileIndex[0][1] 
    transaction_file = args.fileIndex[1][1]

    workingDir = args.workingDir

else:
    activity_file = 'oli_content.csv'
    transaction_file = 'trans.txt'
    workingDir = "."


# In[3]:


df_oli = pd.read_csv(activity_file)
#iterate through activities
    #create variable for total_inputs
    #create dict_count of input: num
    #create dict_feedback of input : feedback
    #go through questions (stepname)
        #save question content and id
        #find matches of question id to step name from trans.txt
            #go through these matches
                #loc to inputs column
                #if input is not in dict_count, add input : 1
                #else input : num++
                
                #loc to feedback text
                #if input is not in dict_feedback, add input: eval
                #else leave be
                
                #increment total_inputs ++
                


# In[4]:


tx = pd.read_csv(transaction_file,delimiter="\t", index_col='Row')
acts2 = df_oli["Activity"].unique()
acts = []
for x in acts2:
    for n in x.split("'"):
        if n != '[]' and n != '[' and n != ']':
            acts.append(n)


# In[5]:


def get_list_of_questions_and_content(activity):
#     activity = "['" + activity + "']"
    lst_ids = []
    lst_content = []
    question_id = df_oli.loc[df_oli['Activity'] == activity, 'Question Texts'].values[0]
    for n in question_id.split("'"):
        #id and content
        if n[0].isalpha():
            #ids
            find = n.find("Question")
            if find != -1:
                lst_ids.append(n[:find-1])
            #content
            else:
                lst_content.append(n)
    return lst_ids, lst_content


# In[15]:


activities = []
question_list = []
question_content_list = []
total_count_nums = []
total_input_counts = []
total_input_feedbacks = []
# step_names = tx['Step Name'].tolist() #from trans.txt
for x in acts2:
    ques_list, ques_content = get_list_of_questions_and_content(x) #from the csv
#     question_list.append(ques_list)
#     question_content_list.append(ques_content)
    i = 0
    #iterate through ques_list and find matches
    for q in ques_list:
        total_inputs = 0
        dict_count = {}
        dict_feedback = {}
        df_matching = tx[tx['Step Name'].str.contains(q)==True]
        step_matches = list(df_matching['Step Name'].index)
        #iterate through step_matches
        for match in step_matches:
            if isinstance(tx['Input'][match], str):
                #input_content = (tx['Input'][match].replace('<material>', " ")).replace('</material>', " ")
                input_content = re.sub('<[^<]+?>', '', tx['Input'][match])

            #go through counts
            if(input_content not in dict_count):
                dict_count[input_content] = 1
            else:
                dict_count[input_content] = dict_count[input_content] + 1

            #go through feedback/evaluation
            if isinstance(tx['Feedback Text'][match], str):
                input_feedback = re.sub('<[^<]+?>', '', tx['Feedback Text'][match])
            if(input_content not in dict_feedback):
                dict_feedback[input_content] = input_feedback

            total_inputs = total_inputs + 1
        
        activities.append(x)
        total_input_counts.append(dict_count)
        total_input_feedbacks.append(dict_feedback)
        total_count_nums.append(total_inputs)
        question_list.append(q)
        question_content_list.append(ques_content[i])
        i = i + 1


# In[10]:


#add percentage to totalinputcounts
total_input_counts_revised = []
index = 0
for t in total_input_counts:
    temp = {}
    for k, v in t.items():
        if(k not in temp):
            percentage = round((v/total_count_nums[index])*100,2)
            temp[k] = str(v) + " (" + str(percentage) + "%)"
    total_input_counts_revised.append(temp)
    index = index + 1
    


# In[11]:


df_final = pd.DataFrame()
df_final['Activity'] = activities
df_final['Question Id'] = question_list
df_final['Total Observations'] = total_count_nums
df_final['Question Content'] = question_content_list
df_final['Input Count and Percentage'] = total_input_counts_revised
df_final['Input Evaluation'] = total_input_feedbacks

df_final = df_final[['Activity','Question Id', 'Total Observations', 'Question Content', 'Input Count and Percentage', 'Input Evaluation']]
                       
df_final.to_csv("error_report.csv",  mode='w', index = False)
# pd.set_option('display.max_rows', None)
#display(df_final)


# In[ ]:





# In[317]:





# In[ ]:




