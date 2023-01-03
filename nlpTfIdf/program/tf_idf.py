#!/usr/bin/env python
# coding: utf-8

# In[10]:


import argparse
import numpy as np
import pandas as pd
from nltk.tokenize import word_tokenize
import nltk
from nltk.corpus import stopwords
STOPWORDS = set(stopwords.words('english'))
from io import StringIO
import re
from collections import defaultdict
import string
import zipfile
import os
import shutil
import sys
from pathlib import Path
from tika import parser # pip install tika


# In[11]:


#defining the function to remove punctuation and extra space
def remove_punctuation(text):
    if(type(text)==float):
        return text
    #remove punctuation
    ans_no_punct = ""
    for i in text:
        if i not in string.punctuation:
            ans_no_punct += i
    #remove the extra space
    ans_no_extra_space = ""
    prev_char = ""
    for i in ans_no_punct:
        if i.strip() != "" or ( i.strip() == "" and prev_char.strip() != ""):
            ans_no_extra_space += i
        prev_char = i  
    return ans_no_extra_space.strip()
#remove_punctuation("Goodwill and other intangible assets account for some 2.0 mln euro ( $ 2.6 mln ) of the purchase price , 20 pct of which payable in Aspo shares .")


# In[12]:


#method to generate n-grams:
#params:
#text-the text for which we have to generate n-grams
#ngram-number of grams to be generated from the text(1,2,3,4 etc., default value=1)
def generate_N_grams(text, ngram=1, exclude_stopwords=True):
    words = []
    if exclude_stopwords:
        words=[word for word in text.split(" ") if word not in STOPWORDS] 
    else:
        words=[word for word in text.split(" ") if word not in []] 
    temp=zip(*[words[i:] for i in range(0,ngram)])
    all_combinations = [' '.join(ngram) for ngram in temp]
    all_combination_count = defaultdict(int)
    for combination in all_combinations:
        all_combination_count[combination] += 1
    return all_combination_count
#generate_N_grams(remove_punctuation("Goodwill and other intangible Goodwill and other intangible assets account for some 2.0 mln euro ( $ 2.6 mln ) of the purchase price , 20 pct of which payable in Aspo shares ."), 2, False)


# In[13]:


def cleanStopWords(text):
    words = []
    words=[word for word in text.split(" ") if word not in STOPWORDS] 
    return " ".join(words)
#print(cleanStopWords("and al and the alds"))


# In[14]:


def logToWfl(msg):
    logFile = open("tf_idf.wfl", "a")
    now = dt.datetime.now()
    logFile.write(str(now) + ": " + msg + "\n");
    logFile.close();


# In[20]:


#test on command line
#"C:/ProgramData/Anaconda3/Python" tf_idf.py -programDir . -workingDir . -userId 1 -exclude_stopwords Yes -term "good job" -term_type "Phrase" -text_column_nodeIndex 0 -text_column_fileIndex 0 -text_column answer -text_column_nodeIndex 0 -text_column_fileIndex 0 -text_column answer_2 -text_corpus "Columns in a file" -node 0 -fileIndex 0 user_comment.txt       
#all variables
command_line = True
working_dir = ""
program_dir = ""
file_name = ""
text_corpus = ""
text_column = ""
term_type = ""
exclude_stopwords = "Yes"
term = ""
   
#command line     
if command_line:
    arg_parser = argparse.ArgumentParser(description='Python program for NLP TFIDF.')
    arg_parser.add_argument('-programDir', type=str, help='the component program directory')
    arg_parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    arg_parser.add_argument("-node", nargs=1, action='append')
    arg_parser.add_argument("-fileIndex", nargs=2, action='append')
    arg_parser.add_argument("-text_corpus", type=str, choices=["Columns in a file", "Files"], default="File")
    arg_parser.add_argument("-text_column", type=str, action='append')
    arg_parser.add_argument("-exclude_stopwords", type=str, choices=["Yes", "No"], default="Yes")
    arg_parser.add_argument("-term_type", type=str, choices=["Word", "Phrase"], default="Word")
    arg_parser.add_argument("-term", type=str)
    args, option_file_index_args = arg_parser.parse_known_args()
    #var for both text corpus
    working_dir = args.workingDir
    program_dir = args.programDir
    file_name = args.fileIndex[0][1]
    text_corpus = args.text_corpus
    #for Columns in a file
    if text_corpus == "Columns in a file":
        text_column = args.text_column
    exclude_stopwords = args.exclude_stopwords
    if exclude_stopwords == "No":
        exclude_stopwords = False
    else:
        exclude_stopwords = True
    term_type = args.term_type
    term = args.term 
else: #for testing
    working_dir = "."
    program_dir = "."
    #file_name = "user_comment.txt"
    file_name = "praises.zip"
    #text_corpus = "Columns in a file"
    text_corpus = "Files"
    text_column = ['answer', 'answer_2']
    term_type = "Phrase"
    exclude_stopwords = True
    term = "good job"


# In[21]:


#modify search word
search_text = ""
n_grams = 1
if term_type == "Phrase" and exclude_stopwords:
    search_text = cleanStopWords(term).strip()
else:
    search_text = term
n_grams = len(search_text.split())
doc_cnt_with_term = defaultdict(int)
#process "Columns in a file"
if text_corpus == "Columns in a file":
    text_corpus_df = pd.read_csv(file_name,sep="\t",encoding='ISO-8859-1', quotechar='"',skipinitialspace=True, error_bad_lines=False)
    original_columns = text_corpus_df.columns.tolist()
    new_columns = original_columns.copy()
    for col_to_search in text_column:
        new_columns.append("tf_idf_" + str(col_to_search))
    output_df = pd.DataFrame(columns = new_columns)
    for index, row in text_corpus_df.iterrows():
        row_as_dict = {}
        for col_to_search in text_column:
            # Clean text
            text_c = remove_punctuation(row[col_to_search])
            if type(text_c) == str:
                # clean more
                text_c = re.sub('[^A-Za-z0-9°]+', ' ', text_c)
                text_c = text_c.replace('\n', '').lower()
            else:
                text_c = str(text_c)
            all_combination_count = generate_N_grams(text_c, n_grams, exclude_stopwords)
            total_term_cnt = 0
            search_term_cnt = 0
            for key in all_combination_count:
                total_term_cnt = total_term_cnt + all_combination_count[key]
                if key == search_text:
                    search_term_cnt = all_combination_count[search_text]
            if search_term_cnt > 0:
                doc_cnt_with_term[col_to_search] += 1
            
            for new_col in new_columns:
                if new_col != "tf_idf_" + str(col_to_search) and new_col in original_columns:
                    row_as_dict[new_col] = row[new_col]
                elif new_col != "tf_idf_" + str(col_to_search):
                    if total_term_cnt > 0:
                        row_as_dict["tf_idf_" + str(col_to_search)] = search_term_cnt/total_term_cnt
                    else:
                        row_as_dict["tf_idf_" + str(col_to_search)] = None
        output_df = output_df.append(row_as_dict, ignore_index = True)
    total_documents = text_corpus_df.shape[0]
    for col_to_search in text_column:
        idf = 0
        if doc_cnt_with_term[col_to_search] > 0:
            idf = np.log(total_documents/doc_cnt_with_term[col_to_search])
        output_df['tf_idf_' + str(col_to_search)] = output_df['tf_idf_' + str(col_to_search)]*idf
    output_df.to_csv('tf_idf_result.txt', sep="\t", index=False)
elif text_corpus == "Files":
    zip_temp_dir = os.path.join(working_dir, "zip_temp")
    if os.path.exists(zip_temp_dir):
        try:
            shutil.rmtree(zip_temp_dir)
        except OSError as e:
            logToWfl("Error: %s : %s" % (dir_path, e.strerror))
            sys.exit("Error: %s : %s" % (dir_path, e.strerror))
    #make new dir
    if not os.path.exists(zip_temp_dir):
        os.makedirs(zip_temp_dir)
    #if zip dir already exist
    with zipfile.ZipFile(file_name, 'r') as zip_ref:
        zip_ref.extractall(zip_temp_dir)
        zip_ref.close()
    output_df = pd.DataFrame(columns = ['file', 'tf_idf'])
    doc_cnt_with_term = 0
    doc_cnt = 0
    #go throught each file
    for subdir, dirs, files in os.walk(zip_temp_dir):
        for file in files:
            row_as_dict = {}
            file_full_path = os.path.join(subdir, file)
            file_name_for_id = file_full_path[(file_full_path.find(zip_temp_dir) + len(zip_temp_dir)+1):]
            #file name without zip_temp
            file_ext = os.path.splitext(file_full_path)[1]
            text = ""
            acceptable_file_type = True
            if file_ext == ".pdf":
                raw = parser.from_file(file_full_path)
                text = raw['content']
                doc_cnt+=1;
            elif file_ext == ".doc" or file_ext == ".docx":
                raw = parser.from_file(file_full_path)
                text = raw['content']
                doc_cnt+=1;
            elif file_ext == ".txt" or file_ext == ".csv":
                f = open(file_full_path, "r", encoding="ISO-8859-1")
                text = f.read()
                f.close()
                doc_cnt+=1;
            else:
                acceptable_file_type = False;
            if acceptable_file_type == False:
                row_as_dict["file"] = file_name_for_id
                row_as_dict["tf_idf"] = None
            else:
                # Clean text
                text_c = remove_punctuation(text)
                if type(text_c) == str:
                    # clean more
                    text_c = re.sub('[^A-Za-z0-9°]+', ' ', text_c)
                    text_c = text_c.replace('\n', '').lower()
                else:
                    text_c = str(text_c)
                all_combination_count = generate_N_grams(text_c, n_grams, exclude_stopwords)
                #print(all_combination_count)
                total_term_cnt = 0
                search_term_cnt = 0
                for key in all_combination_count:
                    total_term_cnt = total_term_cnt + all_combination_count[key]
                    if key == search_text:
                        search_term_cnt = all_combination_count[search_text]
                if search_term_cnt > 0:
                    doc_cnt_with_term += 1
                row_as_dict["file"] = file_name_for_id
                
                if total_term_cnt > 0:
                    row_as_dict["tf_idf"] = search_term_cnt/total_term_cnt
                else:
                    row_as_dict["tf_idf"] = None
            output_df = output_df.append(row_as_dict, ignore_index = True)
    idf = 0
    if doc_cnt_with_term > 0:
        idf = np.log(doc_cnt/doc_cnt_with_term)
    output_df['tf_idf'] = output_df['tf_idf']*idf
    output_df.to_csv('tf_idf_result.txt', sep="\t", index=False)


# In[ ]:




