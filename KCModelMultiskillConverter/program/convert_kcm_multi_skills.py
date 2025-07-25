#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np
import sys
import datetime as dt
import argparse
import re
import copy
import os
#from itertools import chain


# In[2]:


#function to break skills, order them and concetenate. order is important so that skill_a+skill_b is the same as skill_B+skill_a
def concetanete_skills(skills):
    if skills is None or skills is np.nan:
        return skills
    skills = skills.split('~~')
    skills = sorted(skills)
    return '+'.join(skills)


# In[3]:


#command line
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description='Python program to generate student-step file with new KCM.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument('-user', type=str, help='user')
    
    parser.add_argument("-kcm_to_concatenate", type=str, required=True, help="The KCM to be concatenated")
    
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    
    args, option_file_index_args = parser.parse_known_args()
    #process files
    filename = ""
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            filename = args.fileIndex[x][1]
            
    working_dir = args.workingDir
    program_dir = args.programDir
    user = args.user
    kcm_to_concatenate = args.kcm_to_concatenate
    
else:
    working_dir = "."
    filename = 'ds6586_kcm90466_2025_0722_124309.txt'
    kcm_to_concatenate = 'KC (Default)'

kcm_concatenated = re.sub(r'\((.*?)\)', r'(\1 Concatenated)', kcm_to_concatenate)
    
df = pd.read_csv(filename, dtype=str, na_values = ['null', 'na', 'NA', 'n/a', 'nan'], sep="\t", encoding = "ISO-8859-1")
cols = df.columns
new_cols = []
for col in cols:
    if not kcm_to_concatenate in col:
        new_cols.append(col)
new_cols.append(kcm_concatenated)
new_df = pd.DataFrame(columns = new_cols)

for index, row in df.iterrows():
    combinedSkills = ""
    new_row = {}  
    for col in cols:
        val = row[col]

        if not kcm_to_concatenate in col:
            new_row[col] = "" if pd.isnull(val) else str(val).strip()
        else:
            if not pd.isnull(val) and str(val).strip() != "":
                val_str = str(val).strip()
                if combinedSkills == "":
                    combinedSkills = val_str
                else:
                    combinedSkills += "~~" + val_str
    combinedSkills = concetanete_skills(combinedSkills)
    new_row[kcm_concatenated] = combinedSkills
    #new_df = new_df.append(new_row, ignore_index=True)
    new_df = pd.concat([new_df, pd.DataFrame([new_row])], ignore_index=True, sort=False)
filename = os.path.basename(filename)
output_file = os.path.join(working_dir, f'multiskill_converted_{filename}')
new_df.to_csv(output_file, sep='\t', index=False, na_rep='')
                


# In[ ]:




