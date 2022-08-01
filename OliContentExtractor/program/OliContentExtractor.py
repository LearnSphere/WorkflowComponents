#!/usr/bin/env python
# coding: utf-8

# # OLI Content Extraction

# In[1]:


import pandas as pd
import numpy as np
import os
from bs4 import BeautifulSoup
import zipfile
import csv
import re
import argparse
import sys
import shutil


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
   
    input_files = args.fileIndex[0][1] #f19.zip
    print(input_files)
    workingDir = args.workingDir

else:
    input_files = "f19.zip"
    workingDir = "."


# In[3]:


inputPath = "input"
if os.path.exists(inputPath):
    shutil.rmtree(inputPath)
os.makedirs(inputPath)


# In[4]:


with zipfile.ZipFile(input_files, 'r') as zip_ref:
    zip_ref.extractall("input")
basefile = os.path.basename(inputPath)
basename = os.path.basename(input_files).replace('.zip', '')
print("base file: " + basefile)
print("base name: " + basename)


# In[5]:


# SEMESTER = "f19"
#oli_org = open(f'{SEMESTER}/organizations/default/organization.xml', "r").read()
oli_org = open(basefile + '/' + basename + '/organizations/default/organization.xml', "r").read()
oli_org_soup = BeautifulSoup(oli_org, "xml")


# In[6]:


def get_module_unit_from_org(page_id):
    try:
        resource_ref = oli_org_soup.find('resourceref', {'idref': page_id})
        curr_module = resource_ref.find_parent('module').find('title').get_text()
        curr_unit = resource_ref.find_parent('unit').find('title').get_text()
        return curr_module, curr_unit
    except:
        return None, None

def is_header(p):
    # a header paragraph should have the form <p><em>...</em></p>, with no other inner tag
    n_contents = len([c for c in p.contents if not str(c.string).isspace()])
    return p.find("em") is not None and n_contents == 1


#make a list of activities from the workbook page
#get those activities as problem names from inline assessments
#get problem content

def get_file_content(filename):
    with open(basefile + '/' + basename + '/content/x-oli-workbook_page/' + filename ) as file:
        soup = BeautifulSoup(file.read(), 'xml')
    page_id = soup.find('workbook_page')['id']
    curr_module, curr_unit = get_module_unit_from_org(page_id)
    title = soup.find("title").get_text().strip()   
    
    #get activity <activity idref="newb8e4f2938ef7460ab6684f76ad70e9e1" purpose="checkpoint
    inline = []
    for x in soup.find_all("wb:inline"):
        inline.append(x)
    
    inline_id = []
    for z in inline:
        attributes_dictionary = soup.find('wb:inline').attrs
#         print(attributes_dictionary)
        inline_id.append(attributes_dictionary['idref'])
    
#     question_content = []
#     choices_content = []
#     #go through inline-assessment
#     for i in inline_id:
#         question_content.append(get_problems(i)[0])
#         choices_content.append(get_problems(i)[1])
        
    question_content = {} #dictionary of dictionaries, index corresponding to the question number
    choices_content = {}
    feedback_content = {}
    #go through inline-assessment
    for i in inline_id:
        ques = get_problems(i)[0]
        choi = get_problems(i)[1]
        feed = get_problems(i)[2]
        ques_id = get_problems(i)[3]
        #iterate through problems
        n = 0
        for k, v in ques.items():
            #get index number
            ind = k[-1]
            if(ind not in question_content):
                question_content[ques_id[n] + " Question " + str(ind)] = [v]
            else:
                question_content[ques_id[n] + " Question " + str(ind)].append(v)
            n = n + 1
        n = 0
        for k, v in choi.items():
            #get index number
            ind = k[-1]
#             print(ind)
            if(ind not in choices_content):
                choices_content[ques_id[n] + " Question " + str(ind)] = [v]
            else:
                choices_content[ques_id[n] + " Question " + str(ind)].append(v)
            n = n + 1
        n = 0
        for k, v in feed.items():
            #get index number
            ind = k[-1]
            if(ind not in feedback_content):
                feedback_content[ques_id[n] + " Question " + str(ind)] = [v]
            else:
                feedback_content[ques_id[n] + " Question " + str(ind)].append(v)
            n = n + 1
        
#     print(question_content)

    return {
        "Unit" : curr_unit, "Module" : curr_module,  "Title" : title, "Activity" : inline_id,
        "Question Texts" : question_content, "Choices" : choices_content, "Feedback" : feedback_content
    }

    
def get_problems(inline_id):
    for filename in os.listdir(basefile + '/' + basename + "/content/x-oli-inline-assessment"):
#         print(filename)
#         print(inline_id)
        if filename == (inline_id + '.xml'):
            file = filename
            break
    with open(basefile + '/' + basename + '/content/x-oli-inline-assessment/' + file ) as file:
        soup = BeautifulSoup(file.read(), 'xml')
    title = soup.find("title").get_text().strip()
    #feedback = soup.find("feedback").get_text().strip()
    question_all = soup.find("body").get_text().strip()
    
    question = {}
    choices = {}
    feedback = {}
    question_ids = []
    num = 1
    for p in soup.find_all("question"):
        question_id = p.attrs['id']
        question_ids.append(question_id)
        question[question_id + "Question" + str(num)] = (p.find("p").get_text().strip())
        num2 = 1
        temp = {}
        for n in p.find_all("choice"):
            choice_id = n.attrs['value']
            if(n != None):
                temp[choice_id + " Choice " + str(num2)] = n.get_text()
            num2 = num2 + 1
        choices[question_id + " Question " + str(num)] = temp
        temp2 = {}
        n = 1
        for f in p.find_all("feedback"):
            feedback_id = ""
            if(f.find('p') != None):
                feedback_id = (f.find('p').attrs)['id']
            temp2[feedback_id + " Feedback " + str(n)] = f.get_text()
            n = n + 1
        feedback[question_id + " Question " + str(num)] = temp2
        num = num + 1
    return question, choices, feedback, question_ids


# In[7]:


# curr_unit, curr_module, title, inline_id, question_content, choices_content = [],[],[],[],[],[]

# for filename in os.listdir(basefile + '/' + basename + "/content/x-oli-workbook_page"):
#     if filename.endswith(".xml"):
#         dict = get_file_content(filename)
#         num = 1
#         for x, y in dict.items():
#             if(num == 1):
#                 curr_unit.append(y)
#             if(num == 2):
#                 curr_module.append(y)
#             if(num == 3):
#                 title.append(y)
#             if(num == 4):
#                 inline_id.append(y)
#             if(num == 5):
#                 question_content.append(y)
#             if(num == 6):
#                 choices_content.append(y)
#             num = num + 1


# for filename in os.listdir(basefile + '/' + basename + "/content/x-oli-workbook_page"):
#     if filename.endswith(".xml"):
#         print(get_file_content())
df_oli = pd.DataFrame()
df_oli = pd.DataFrame([
    get_file_content(filename)
    for filename in os.listdir(basefile + '/' + basename + "/content/x-oli-workbook_page")
    if filename.endswith(".xml")
])

df_oli = df_oli[['Unit', 'Module', 'Title', 'Activity', 'Question Texts', 'Choices', 'Feedback']]
df_oli.to_csv("oli_content.csv",  mode='w', index = False)
#display(df_oli)
# print(df_oli.head())
#pd.set_option('display.max_rows', None)


# In[ ]:





# In[ ]:




