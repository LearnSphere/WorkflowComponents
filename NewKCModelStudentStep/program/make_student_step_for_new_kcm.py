#!/usr/bin/env python
# coding: utf-8

# In[8]:


import pandas as pd
import numpy as np
import argparse
import re
import os
import sys
import datetime as dt


# In[9]:


def logToWfl(msg):
    logFile = open(log_file_name, "a")
    now = dt.datetime.now()
    logFile.write(str(now) + ": " + msg + "\n");
    logFile.close();


# In[10]:


#looking this pattern in KCM file: 'KC (Lasso Model)', 'KC (Lasso Model).1', 'KC (Lasso Model).2'....
def is_multi_skills_model(df_kcm, kcm_name):
    count = sum(col.startswith(kcm_name) for col in df_kcm.columns)
    if count > 1:
        return True
    else:
        return False


# In[11]:


def strip_model_name(model_name):
    match = re.search(r'\((.*?)\)', model_name)
    if match is None:
        return model_name
    else:
        return match.group(1)


# In[12]:


#command line
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description='Python program to generate student-step file with new KCM.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument('-user', type=str, help='user')
    
    parser.add_argument("-keep_steps_without_skill",  type=str, required=True)
    parser.add_argument("-new_kcm_name", type=str, required=True, help="The new model")
    
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    
    args, option_file_index_args = parser.parse_known_args()
    #process files
    student_step_file = ""
    kcm_file = ""
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            student_step_file = args.fileIndex[x][1]
        if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
            kcm_file = args.fileIndex[x][1]
            
    working_dir = args.workingDir
    program_dir = args.programDir
    user = args.user
    keep_steps_without_skill = args.keep_steps_without_skill
    if keep_steps_without_skill.lower() == 'true':
        keep_steps_without_skill = True
    else:
        keep_steps_without_skill = False
    new_kcm_name = args.new_kcm_name
    
else:
    working_dir = "."
    #current student_step
    student_step_file = "ds76_student_step_All_Data_74_2025_0528_101035.txt"
    #new KCM, exported from datashop and modified with new KCM
    kcm_file = "ds76_kcm_2025_0722_175015_lasso.txt"
    #keep the steps that are not labelled for skills
    keep_steps_without_skill = False
    #new KC model name
    #new_kcm_name = 'KC (Lasso Model)'
    new_kcm_name = 'KC (DecompArithDiam_1)'


log_file_name = os.path.join(working_dir, "makeSudent_step_for_new_kcm.wfl")

df_student_step = pd.read_csv(student_step_file, sep='\t')
#delete all parenthesis in column Problem Hierarchy
df_student_step.rename(columns={'Problem Hierarchy': 'Problem Hierarchy_copy'}, inplace=True)
df_student_step['Problem Hierarchy'] = df_student_step['Problem Hierarchy_copy']
df_student_step['Problem Hierarchy'] = df_student_step['Problem Hierarchy'].str.replace(r'[()]', '', regex=True)
student_step_required_columns = ['Row', 'Anon Student Id', 'Problem Hierarchy', 'Problem Name', 'Step Name']
if not set(student_step_required_columns).issubset(df_student_step.columns):
    error_msg = f"Required column(s) is missing from the student-step file: {student_step_file}"
    logToWfl(error_msg)
    print(error_msg)
    sys.exit(error_msg)
#make sure the new_kcm_name isn't already in the df_student_step
if new_kcm_name in df_student_step.columns:
    error_msg = f'The new KCM "{new_kcm_name}" already exists in the student-step file: {student_step_file}'
    logToWfl(error_msg)
    print(error_msg)
    sys.exit(error_msg)
    
df_kcm = pd.read_csv(kcm_file, sep='\t')
#delete all parenthesis in column Problem Hierarchy
df_kcm['Problem Hierarchy'] = df_kcm['Problem Hierarchy'].str.replace(r'[()]', '', regex=True)
#multi-skill is not allowed
if is_multi_skills_model(df_kcm, new_kcm_name):
    error_msg = "The new KC model has multiple skills for some steps and need to be turned into a model with single skill per step."
    logToWfl(error_msg)
    print(error_msg)
    sys.exit()
KCM_required_columns = ['Problem Hierarchy', 'Problem Name', 'Step Name']
if not set(KCM_required_columns).issubset(df_kcm.columns):
    error_msg = "Required column(s) is missing from the KCM file: {kcm_file}"
    logToWfl(error_msg)
    print(error_msg)
    sys.exit()
#only keep useful columns
df_kcm = df_kcm[[new_kcm_name,'Problem Hierarchy', 'Problem Name', 'Step Name']]

#join the two dataframes
joined_df = None
if keep_steps_without_skill:
    #left joine
    joined_df = df_student_step.merge(df_kcm, how='left', on=['Problem Hierarchy', 'Problem Name', 'Step Name'])
else:
    #inner join
    joined_df = df_student_step.merge(df_kcm, how='inner', on=['Problem Hierarchy', 'Problem Name', 'Step Name'])

if joined_df is None or joined_df.empty:
    error_msg = f'No common "Problem Hierarchy", "Problem Name" and "Step Name" are found in {student_step_file} and {kcm_file} files.'
    logToWfl(error_msg)
    print(error_msg)
    sys.exit()
    
#order by student, skill and row
sorted_joined_df = joined_df.sort_values(by=['Anon Student Id', new_kcm_name, 'Row'])
#delete problem and rename
sorted_joined_df.drop(columns=['Problem Hierarchy'], inplace=True)
sorted_joined_df.rename(columns={'Problem Hierarchy_copy': 'Problem Hierarchy'}, inplace=True)

model_name = strip_model_name(new_kcm_name)
opportunity_col_name = 'Opportunity (' + model_name +')'

sorted_joined_df[opportunity_col_name] = sorted_joined_df.groupby(['Anon Student Id', new_kcm_name]).cumcount() + 1
sorted_joined_df = sorted_joined_df.sort_values(by=['Row'])
#output
output_file = os.path.join(working_dir, f'student_step_{model_name}.txt')
sorted_joined_df.to_csv(output_file, sep='\t', index=False, na_rep='')


# In[ ]:




