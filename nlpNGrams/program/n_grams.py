#!/usr/bin/env python
# coding: utf-8

# In[4]:


import argparse
import numpy as np
import pandas as pd
import pathlib
import math
import matplotlib.pyplot as plt
plt.style.use(style='seaborn')
from collections import defaultdict
from sklearn.model_selection import train_test_split
import string
import seaborn as sns
import urllib
import re
import nltk
from nltk.corpus import stopwords
STOPWORDS = set(stopwords.words('english'))
import plotly.express as px
import plotly
import plotly.graph_objects as go
import circlify
from tika import parser # pip install tika
import requests
from fpdf import FPDF
from PIL import Image
import warnings


# In[5]:


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


# In[6]:


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


# In[7]:


#create function to get a color dictionary
def get_colordict(palette,number,start):
    pal = list(sns.color_palette(palette=palette, n_colors=number).as_hex())
    color_d = dict(enumerate(pal, start=start))
    return color_d


# In[8]:


def merge_count_dict(final_dict, to_merge_dict):
    final_dict_keys = final_dict.keys()
    for to_merge_key in to_merge_dict.keys():
        if to_merge_key in final_dict_keys:
            final_dict[to_merge_key] = final_dict[to_merge_key] + to_merge_dict[to_merge_key]
        else:
            final_dict[to_merge_key] = to_merge_dict[to_merge_key]
    return final_dict
#to_merge_dict = {'key1': 1, 'key2': 2, 'key3': 3}
#final_dict = {}
#print(merge_count_dict(final_dict, to_merge_dict))
#print(merge_count_dict(final_dict, to_merge_dict))


# In[12]:


#test on command line
#"C:/ProgramData/Anaconda3/Python" n_grams.py -programDir . -workingDir . -userId 1 -exclude_stopwords Yes -n_grams 1 -plot_top_term 20 -plot_type “Donut chart” -text_column_nodeIndex 0 -text_column_fileIndex 0 -text_column user_id -text_corpus File -node 0 -fileIndex 0 "ISLS_2023_Codesign_Short_Paper.pdf"
#"C:/ProgramData/Anaconda3/Python" n_grams.py -programDir . -workingDir . -userId 1 -exclude_stopwords Yes -n_grams 1 -plot_top_term 20 -plot_type “Bar chart” -text_column_nodeIndex 0 -text_column_fileIndex 0 -text_column answer -text_corpus "Column in a file" -node 0 -fileIndex 0 "user_comment.txt"

#all variables
command_line = True
working_dir = ""
program_dir = ""
file_name = ""
n_grams = 1
plot_top_term = 10
exclude_stopwords = "Yes"
plot_type = ""
text_corpus = ""
text_column = ""
url = ""
   
#command line     
if command_line:
    arg_parser = argparse.ArgumentParser(description='Python program for NLP NGrams.')
    arg_parser.add_argument('-programDir', type=str, help='the component program directory')
    arg_parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    arg_parser.add_argument("-node", nargs=1, action='append')
    arg_parser.add_argument("-fileIndex", nargs=2, action='append')
    arg_parser.add_argument("-n_grams", type=int, choices=[1,2,3,4], default=1)
    arg_parser.add_argument("-plot_top_term", type=int)
    arg_parser.add_argument("-exclude_stopwords", type=str, choices=["Yes", "No"], default="Yes")
    arg_parser.add_argument("-plot_type", type=str, choices=["Bar chart", "Donut chart", "Treemap", "Circle packing"], default="Bar chart")
    arg_parser.add_argument("-text_column", type=str)
    arg_parser.add_argument("-url", type=str)
    arg_parser.add_argument("-text_corpus", type=str, choices=["Column in a file", "File", "URL"], default="File")
    args, option_file_index_args = arg_parser.parse_known_args()
    #var for both text corpus
    working_dir = args.workingDir
    program_dir = args.programDir
    file_name = args.fileIndex[0][1]
    n_grams = args.n_grams
    plot_top_term = args.plot_top_term
    exclude_stopwords = args.exclude_stopwords
    if exclude_stopwords == "No":
        exclude_stopwords = False
    else:
        exclude_stopwords = True
    plot_type = args.plot_type
    text_corpus = args.text_corpus
    #for Column in a file
    if text_corpus == "Column in a file":
        text_column = args.text_column
    #for url
    if text_corpus == "URL":
        url = args.url
else: #for testing
    working_dir = "."
    program_dir = "."
    #file_name = "ISLS_2023_Codesign_Short_Paper.pdf"
    file_name = "user_comment.txt"
    n_grams = 2
    plot_top_term = 500
    exclude_stopwords = False
    plot_type = "Donut chart"
    #Bar chart, Donut chart, Treemap, Circle packing
    #text_corpus = "File"
    text_corpus = "Column in a file"
    #text_corpus = "URL"
    text_column = "answer"
    url = "https://pslcdatashop.web.cmu.edu"

df_words = None
if text_corpus == "File" or text_corpus == "URL":
    text = ""
    if text_corpus == "File":
        file_extension = pathlib.Path(file_name).suffix
        if file_extension == ".pdf":
            raw = parser.from_file(file_name)
            text = raw['content']
        elif file_extension == ".doc" or file_extension == ".docx":
            raw = parser.from_file(file_name)
            text = raw['content']
        else: #any other file
            file = open(file_name, "r", encoding="ISO-8859-1") 
            text = file.read()
    elif text_corpus == "URL":
        url_response = requests.get(url)
        text = parser.from_buffer(url_response.content)['content']
    # Clean text
    text_c = remove_punctuation(text)
    if type(text_c) == str:
        # clean more
        text_c = re.sub('[^A-Za-z0-9°]+', ' ', text_c)
        text_c = text_c.replace('\n', '').lower()
    else:
        text_c = str(text_c)
    all_combination_count = generate_N_grams(text_c, n_grams, exclude_stopwords)
    #create DataFrame
    df_words = pd.DataFrame(list(all_combination_count.items()), columns = ['term', 'count'])
    df_words.sort_values('count', ascending=False, inplace=True)
    df_words.reset_index(drop=True, inplace=True)
elif text_corpus == "Column in a file":
    text_corpus_df = pd.read_csv(file_name,sep="\t",encoding='ISO-8859-1', quotechar='"',skipinitialspace=True, error_bad_lines=False)
    final_combination_count = defaultdict(int)
    for index, row in text_corpus_df.iterrows():
        # Clean text
        text_c = remove_punctuation(row[text_column])
        if type(text_c) == str:
            # clean more
            text_c = re.sub('[^A-Za-z0-9°]+', ' ', text_c)
            text_c = text_c.replace('\n', '').lower()
        else:
            text_c = str(text_c)   
        all_combination_count = generate_N_grams(text_c, n_grams, exclude_stopwords)
        #combine with current result
        final_combination_count = merge_count_dict(final_combination_count, all_combination_count)
        #create DataFrame
        df_words = pd.DataFrame(list(final_combination_count.items()), columns = ['term', 'count'])
        df_words.sort_values('count', ascending=False, inplace=True)
        df_words.reset_index(drop=True, inplace=True)
#output result to word_frequency.txt
df_words.to_csv('word_frequency.txt', sep="\t", index=False)


# In[13]:


num_term_plot = df_words.shape[0]
if num_term_plot > plot_top_term:
    num_term_plot = plot_top_term
#draw and output plot
if plot_type == "Bar chart":
    num_col_bar_chart = 5
    if plot_top_term < num_col_bar_chart:
        num_col_bar_chart = 1
    index_list = [[i[0],i[-1]+1] for i in np.array_split(range(num_term_plot), num_col_bar_chart)]
    n = df_words['count'].max()
    color_dict = get_colordict('viridis', n, 1)
    fig, axs = plt.subplots(1, num_col_bar_chart, figsize=(16,8), facecolor='white', squeeze=False)
    for col, idx in zip(range(0,num_col_bar_chart), index_list):
        df = df_words[idx[0]:idx[-1]]
        label = [w + ': ' + str(n) for w,n in zip(df['term'],df['count'])]
        color_l = [color_dict.get(i) for i in df['count']]
        x = list(df['count'])
        y = list(range(0, len(x)))
        sns.barplot(x = x, y = y, data=df, alpha=0.9, orient = 'h',
                    ax = axs[0][col], palette = color_l)
        axs[0][col].set_xlim(0,n+1)                     #set X axis range max
        axs[0][col].set_yticklabels(label, fontsize=12)
        axs[0][col].spines['bottom'].set_color('white')
        axs[0][col].spines['right'].set_color('white')
        axs[0][col].spines['top'].set_color('white')
        axs[0][col].spines['left'].set_color('white')
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        plt.tight_layout()
    plt.savefig('word_frequency_plot.png')  
elif plot_type == "Donut chart":
    max_donut_chart = 25
    if num_term_plot > max_donut_chart:
        num_term_plot = max_donut_chart
    pal = list(sns.color_palette(palette='Reds_r', n_colors=num_term_plot).as_hex())
    fig = px.pie(df_words[0:num_term_plot], values='count', names='term',
             color_discrete_sequence=pal)
    fig.update_traces(textposition='outside', textinfo='percent+label', 
                  hole=.6, hoverinfo="label+percent+name")
    fig.update_layout(width = 800, height = 600,
                  margin = dict(t=0, l=0, r=0, b=0))
    fig.update_layout(title_text='', title_x=0.5,
                 title={"yref": "paper","y" : 1,"yanchor" : "bottom"})
    fig.write_image("word_frequency_plot.png")
elif plot_type == "Treemap":
    fig = px.treemap(df_words[0:num_term_plot], path=[px.Constant("Treemap"), 'term'],
                 values='count',
                 color='count',
                 color_continuous_scale='viridis',
                 color_continuous_midpoint=np.average(df_words['count'])
                )
    fig.update_layout(margin = dict(t=50, l=25, r=25, b=25))
    fig.write_image("word_frequency_plot.png") 
elif plot_type == "Circle packing":
    circles = circlify.circlify(df_words['count'][0:num_term_plot].tolist(), 
                            show_enclosure=False, 
                            target_enclosure=circlify.Circle(x=0, y=0)
                           )
    n = df_words['count'][0:num_term_plot].max()
    color_dict = get_colordict('RdYlBu_r',n ,1)

    #Circle packing showing the top 30 words
    fig, ax = plt.subplots(figsize=(9,9), facecolor='white')
    ax.axis('off')
    lim = max(max(abs(circle.x)+circle.r, abs(circle.y)+circle.r,) for circle in circles)
    plt.xlim(-lim, lim)
    plt.ylim(-lim, lim)

    # list of labels
    labels = list(df_words['term'][0:num_term_plot])
    counts = list(df_words['count'][0:num_term_plot])
    labels.reverse()
    counts.reverse()

    # print circles
    for circle, label, count in zip(circles, labels, counts):
        x, y, r = circle
        ax.add_patch(plt.Circle((x, y), r, alpha=0.9, color = color_dict.get(count)))
        plt.annotate(label +'\n'+ str(count), (x,y), size=8, va='center', ha='center')
    plt.xticks([])
    plt.yticks([])
    plt.savefig('word_frequency_plot.png')

#save png to a pdf for learnsphere displays pdf better
pdf = FPDF('P','mm','A4') # create an A4-size pdf document
img = Image.open('word_frequency_plot.png') 
# get width and height
ration = img.width/img.height
x,y,w,h = 10,10,180,180/ration
pdf.add_page()
pdf.image('word_frequency_plot.png', x,y,w,h)
pdf.output("word_frequency_plot.pdf","F")


# In[ ]:




