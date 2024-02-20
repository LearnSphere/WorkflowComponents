#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np
import sys
import argparse
import os


# In[ ]:


#test command from WF component:
#C:/ProgramData/Anaconda3/Python transform_join.py -programDir . -workingDir . -file1Delimiter "\t" -file2Delimiter "\t" -file_1 "generic_table_1.txt" -file_1_match_columns "Anon Student Id,Row" -file_2 "student_mapping.txt" -file_2_match_columns "Anon Student Id,Actual Student Id" -howToConcatenate vertical -howToJoin merge -howToMerge left -numColumnsToMerge 2 
#C:/ProgramData/Anaconda3/Python transform_join.py -programDir . -workingDir . -file1Delimiter "\t" -file2Delimiter "\t" -file_1 "generic_table_1.txt" -file_2 "student_mapping.txt" -howToConcatenate vertical -howToJoin concatenate -howToMerge left -numColumnsToMerge 5 

#command line
parser = argparse.ArgumentParser(description='Join.')
parser.add_argument('-programDir', type=str, help='the component program directory')
parser.add_argument('-workingDir', type=str, help='the component instance working directory')
parser.add_argument('-file_1', help='file 1 to be processed', required=True)
parser.add_argument('-file_2', help='file 2 to be processed', required=True)
parser.add_argument('-file1Delimiter', help='file 1 field delimiter', default="\t")
parser.add_argument('-file2Delimiter', help='file 2 field delimiter', default="\t")
parser.add_argument('-file_1_match_columns', help='file 1 fields to be merged on')
parser.add_argument('-file_2_match_columns', help='file 2 fields to be merged on')
parser.add_argument('-howToJoin', choices=["concatenate", "merge"], help='join method', required=True)
parser.add_argument('-howToConcatenate', choices=["vertical", "horizontal"], help='concatenate method')
parser.add_argument('-howToMerge', choices=["inner", "left", "right", "outer"], help='merge method')
parser.add_argument('-numColumnsToMerge', help='how many columns to merge on')
args, option_file_index_args = parser.parse_known_args()

working_dir = args.workingDir
join_method = args.howToJoin
file_1 = args.file_1
file_2 = args.file_2
file_1_delimiter = args.file1Delimiter
file_2_delimiter = args.file2Delimiter
file_1_match_columns = args.file_1_match_columns
file_2_match_columns = args.file_2_match_columns
how_to_concatenate = args.howToConcatenate
how_to_merge = args.howToMerge


# In[2]:


def logToWfl(msg): 
    log_file_name = os.path.join(working_dir, 'joinLog.wfl')
    now = dt.datetime.now()
    logFile.write(str(now) + ": " + msg + "\n");
    logFile.close();


# In[3]:


#for testing program
# join_method = "merge"
# file_1 = "generic_table_1.txt"
# file_1_match_columns = "Anon Student Id,First Transaction Time" 
# file_2 = "student_mapping.txt" 
# file_2_match_columns = "Anon Student Id,Actual Student Id" 
# how_to_concatenate = "vertical"
# how_to_merge = "inner" 
# file_1_delimiter = "\t" 
# file_2_delimiter = "\t"
# working_dir = "."


# In[4]:


try:
    file_encoding = 'utf8'        # set file_encoding to the file encoding (utf8, latin1, etc.)
    # input_fd1 = open(file_1, encoding=file_encoding, errors = 'backslashreplace')
    # df1 = pd.read_csv(input_fd1, sep=file_1_delimiter, error_bad_lines=False, low_memory=False)
    if file_1_delimiter == "\\t":
        df1 = pd.read_csv(file_1,sep="\t",encoding='utf8',dtype=object, engine='python')
    else:
        df1 = pd.read_csv(file_1,encoding='utf8',dtype=object, engine='python')
       
    # input_fd2 = open(file_2, encoding=file_encoding, errors = 'backslashreplace')
    # df2 = pd.read_csv(input_fd2, sep=file_2_delimiter, error_bad_lines=False, low_memory=False)
    if file_2_delimiter == "\\t":
        df2 = pd.read_csv(file_2,sep="\t",encoding='utf8',dtype=object, engine='python') 
    else:
        df2 = pd.read_csv(file_2,encoding='utf8',dtype=object, engine='python')
    
except Warning as e:
    logToWfl(e)
    


# In[5]:


#concatenate
result = None
if join_method == 'concatenate':
    if how_to_concatenate == 'horizontal':
        result = pd.concat([df1, df2], ignore_index=True, sort=False, axis=1)
    else:
        result = pd.concat([df1, df2], ignore_index=True, sort=False, axis=0)
#merge
else:
    left_on_col_list = file_1_match_columns.split(",")
    right_on_col_list = file_2_match_columns.split(",")
    #make sure all pair columns have the same column type
    result = df1.merge(df2, how=how_to_merge, left_on=left_on_col_list, right_on=right_on_col_list)

output_file = os.path.join(working_dir, 'joinedResult.txt')
result.to_csv(output_file, sep="\t", index=False, )  


# In[ ]:




