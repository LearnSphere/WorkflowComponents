#!/usr/bin/env python
# coding: utf-8

# In[12]:


import pandas as pd
import argparse
import os


# In[15]:


#test command from WF component:
#C:/Users/hchen/Anaconda3/python.exe convertTabCsv.py -programDir . -workingDir . -userId 1 -inputFormat CSV -inputFile "MainTable.csv"

#command line
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description='Python program to convert between CSV and tab-delimited file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-inputFile", type=str)
    parser.add_argument("-inputFormat", type=str, choices=["CSV", "Tab-delimited"], default="CSV")
    
    args, option_file_index_args = parser.parse_known_args()
    #no train_split_type
    working_dir = args.workingDir
    program_dir = args.programDir
    file_name = args.inputFile
    input_format = args.inputFormat
else:
    #test file
    #file_name = "MainTable_test_for_csv.csv"
    file_name = "MainTable_test_for_tab.txt"
    #input format
    #input_format="CSV"
    input_format="Tab-delimited"
    #working_dir
    working_dir = "."
    


# In[16]:


df = None
if input_format == "CSV":
    df = pd.read_csv(file_name)
    #output to tab-delimited
    df.to_csv(os.path.join(working_dir, 'convertedDelimited.txt'), sep ='\t', index=False)
elif input_format == "Tab-delimited":
    df = pd.read_csv(file_name, sep="\t")
    #output to csv
    df.to_csv(os.path.join(working_dir, 'convertedDelimited.csv'), index=False)


# In[ ]:




