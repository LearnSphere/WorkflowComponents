#!/usr/bin/env python
# coding: utf-8

# In[27]:


import pandas as pd
import numpy as np
import sys
import datetime as dt
import argparse
import re
import copy
import os
from itertools import chain


# In[28]:


#function to break skills, order them and concetenate. order is important so that skill_a+skill_b is the same as skill_B+skill_a
def concetanete_skills(skills):
    if skills is None or skills is np.nan:
        return skills
    skills = skills.split('~~')
    skills = sorted(skills)
    return '+'.join(skills)


# In[29]:


def logProgressToWfl(progressMsg):
    logFile = open("multiskillConverterLog.wfl", "a")
    now = dt.datetime.now()
    progressPrepend = "%Progress::"
    logFile.write(progressPrepend + "@" + str(now) + "@" + progressMsg + "\n");
    logFile.close();


# In[ ]:


#C:/ProgramData/Anaconda3/Python multiskill_converter.py -programDir . -workingDir . -userId 1 -kcModelsToConvert_nodeIndex 0 -kcModelsToConvert_fileIndex 0 -kcModelsToConvert "KC (CCSS)" -kcModelsToConvert_nodeIndex 0 -kcModelsToConvert_fileIndex 0 -kcModelsToConvert "KC (MATHia New)" -multiskillConversionMethod "Concatenate" -node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\MultiskillConverter\test\test_data\test.txt -inputFile test.txt
#C:/ProgramData/Anaconda3/Python multiskill_converter.py -programDir . -workingDir . -userId 1 -kcModelToConvert_nodeIndex 0 -kcModelToConvert_fileIndex 0 -kcModelToConvert "KC (MATHia New)" -multiskillConversionMethod "Split to Multiple Rows" -valuesToBeSplit_nodeIndex 0 -valuesToBeSplit_fileIndex 0 -valuesToBeSplit "Correct Step Duration (sec)" -valuesToBeSplit_nodeIndex 0 -valuesToBeSplit_fileIndex 0 -valuesToBeSplit "Step Duration (sec)" -node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\MultiskillConverter\test\test_data\test.txt -inputFile test.txt
#C:/ProgramData/Anaconda3/Python multiskill_converter.py -programDir . -workingDir . -userId 1 -kcModelToConvert_nodeIndex 0 -kcModelToConvert_fileIndex 0 -kcModelToConvert "KC (MATHia New)" -multiskillConversionMethod "Split to Multiple Rows" -valuesToBeSplit_nodeIndex 0 -valuesToBeSplit_fileIndex 0 -valuesToBeSplit "Step End Time" -valuesToBeSplit_nodeIndex 0 -valuesToBeSplit_fileIndex 0 -valuesToBeSplit "Step Duration (sec)" -node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\MultiskillConverter\test\test_data\test.txt -inputFile test.txt

#command line
parser = argparse.ArgumentParser(description='Process datashop file.')
parser.add_argument('-programDir', type=str, help='the component program directory')
parser.add_argument('-workingDir', type=str, help='the component instance working directory')
parser.add_argument("-node", nargs=1, action='append')
parser.add_argument("-fileIndex", nargs=2, action='append')
parser.add_argument('-multiskillConversionMethod', choices=["Concatenate", "Split to Multiple Rows"], help='Method to handle multiskill steps(default="Concatenate")', default="Concatenate")
parser.add_argument('-kcModelsToConvert', nargs=1, action='append', type=str, help='KC models to convert when concatenating; e.g., "Item"')
parser.add_argument('-kcModelToConvert', nargs=1, type=str, help='KC model to convert when Split to Multiple Rows; e.g., "Item"')
parser.add_argument('-valuesToBeSplit', nargs=1, action='append', type=str, help='KC model to convert when Split to Multiple Rows;')
parser.add_argument('-averageColumnValues', choices=["Yes", "No"], help='If any column value should be averaged(default="No")', default="Concatenate")
parser.add_argument('-inputFile', type=str, help='data file containing multi-skill steps')
parser.add_argument('-userId', type=str,  help='placeholder for WF', default='')
args, option_file_index_args = parser.parse_known_args()
filename = args.inputFile
modification_method = args.multiskillConversionMethod
kcms_to_change = args.kcModelsToConvert
if kcms_to_change is not None:
    kcms_to_change = list(chain.from_iterable(kcms_to_change))
kcm_to_split = args.kcModelToConvert
if kcm_to_split is not None:
    kcm_to_split = kcm_to_split[0]
columns_value_to_be_split = args.valuesToBeSplit
if columns_value_to_be_split is not None:
    columns_value_to_be_split = list(chain.from_iterable(columns_value_to_be_split))
average_column_values = args.averageColumnValues
if average_column_values is not None and average_column_values == "Yes":
    average_column_values = True
else:
    average_column_values = False


# In[30]:


if False:
    filename = 'test.txt'

    #modification_method = 'Concatenate'
    kcms_to_change = ['KC (CCSS)', 'KC (MATHia New)']
    modification_method = 'Split to Multiple Rows'
    kcm_to_split = 'KC (MATHia New)'
    columns_value_to_be_split = ['Step Duration (sec)', 'Correct Step Duration (sec)']
    average_column_values = True


# In[31]:


df = pd.read_csv(filename, dtype=str, na_values = ['null', 'na', 'NA', 'n/a', 'nan'], sep="\t", encoding = "ISO-8859-1")
if modification_method == 'Concatenate':
    for kcm_to_change in kcms_to_change:
        print(kcm_to_change)
        if kcm_to_change in df.columns:
            #change ~~ to +
            df[kcm_to_change] = df[kcm_to_change].apply(concetanete_skills)
            #get KC model name without prefix "KC(""
            kcm_name = kcm_to_change
            if "KC (" in kcm_to_change and ")" in kcm_to_change:
                kc_name = kcm_to_change[len("KC ("):kcm_to_change.find(")")]
            kcm_opportunity = "Opportunity ({})".format(kc_name)
            if kcm_opportunity in df.columns:
                df.drop(kcm_opportunity, axis=1, inplace=True)
            df_omit_na = df[['Anon Student Id', kcm_to_change]]
            df_omit_na = df_omit_na.dropna()
            df_omit_na[kcm_opportunity] = df_omit_na.groupby(['Anon Student Id', kcm_to_change]).cumcount()+1
            df_omit_na = df_omit_na[[kcm_opportunity]]
            df = df.merge(df_omit_na, left_index=True, right_index=True, how='left')
    filename = os.path.basename(os.path.normpath(filename))
    df.to_csv('multiskill_converted_{}'.format(filename), sep='\t', index=False)

elif modification_method == 'Split to Multiple Rows':
    proc_pct = 0.1
    totalCnt = df.shape[0]
    if kcm_to_split in df.columns:
        #make a new dataframe
        split_df = pd.DataFrame(columns = df.columns)
        #loop through each rows
        cnt = 1
        for index, row in df.iterrows():
            #write to the workflow log for percentage processed
            if cnt/totalCnt > proc_pct:
                logProgressToWfl("{:.0%}".format(proc_pct))
                proc_pct = proc_pct + 0.1
            cnt = cnt + 1
            #process skills
            skills = row[kcm_to_split]
            if skills is None or pd.isna(skills):
                split_df = split_df.append(row, ignore_index = True)
                continue
            skills = row[kcm_to_split].split('~~')
            for skill in skills:
                row_as_dict = {}
                for column in df.columns:
                    if column == kcm_to_split:
                        row_as_dict[kcm_to_split] = skill
                    elif average_column_values == True and column in columns_value_to_be_split:
                        val_to_split = row[column]
                        if val_to_split is not None and not pd.isna(val_to_split):
                            try:
                                val = pd.to_numeric(val_to_split)
                                row_as_dict[column] = val/len(skills)
                            except:
                                row_as_dict[column] = row[column]
                        else:
                            row_as_dict[column] = row[column]
                    else:
                        row_as_dict[column] = row[column]
                split_df = split_df.append(row_as_dict, ignore_index = True)
        #redo opportunity
        kcm_name = kcm_to_split
        if "KC (" in kcm_to_split and ")" in kcm_to_split:
            kc_name = kcm_to_split[len("KC ("):kcm_to_split.find(")")]
        kcm_opportunity = "Opportunity ({})".format(kc_name)
        if kcm_opportunity in split_df.columns:
            split_df.drop(kcm_opportunity, axis=1, inplace=True)
        df_omit_na = split_df[['Anon Student Id', kcm_to_split]]
        df_omit_na = df_omit_na.dropna()
        df_omit_na[kcm_opportunity] = df_omit_na.groupby(['Anon Student Id', kcm_to_split]).cumcount()+1
        df_omit_na = df_omit_na[[kcm_opportunity]]
        split_df = split_df.merge(df_omit_na, left_index=True, right_index=True, how='left')
        filename = os.path.basename(os.path.normpath(filename))
        split_df.to_csv('multiskill_converted_{}'.format(filename), sep='\t', index=False)


# In[ ]:




