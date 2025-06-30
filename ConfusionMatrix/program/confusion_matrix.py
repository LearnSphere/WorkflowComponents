#!/usr/bin/env python
# coding: utf-8

# In[73]:


import pandas as pd
import os
import numpy as np
import argparse


# In[74]:


def confusion_matrix_manual(y_true, y_pred, labels=None):
    #get all possible labels
    if labels is None:
        labels = sorted(set(y_true) | set(y_pred))
    #paired index with label
    label_to_index = {label: i for i, label in enumerate(labels)}
    size = len(labels)
    #blank matrix
    matrix = [[0] * size for _ in range(size)]
    for actual, predicted in zip(y_true, y_pred):
        i = label_to_index[actual]
        j = label_to_index[predicted]
        matrix[i][j] += 1

    return labels, matrix


# In[75]:


def calculate_metrics(labels, cm):
    total = sum(sum(row) for row in cm)
    correct = sum(cm[i][i] for i in range(len(labels)))
    accuracy = correct / total

    precision = {}
    recall = {}
    for i, label in enumerate(labels):
        tp = cm[i][i]
        fp = sum(cm[r][i] for r in range(len(labels))) - tp
        fn = sum(cm[i][c] for c in range(len(labels))) - tp
        precision[label] = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall[label] = tp / (tp + fn) if (tp + fn) > 0 else 0.0

    return accuracy, precision, recall


# In[76]:


def calculate_kappa(labels, cm):
    total = sum(sum(row) for row in cm)
    observed_agreement = sum(cm[i][i] for i in range(len(labels))) / total

    row_sums = [sum(row) for row in cm]
    col_sums = [sum(cm[r][c] for r in range(len(labels))) for c in range(len(labels))]

    expected_agreement = sum(
        (row_sums[i] * col_sums[i]) for i in range(len(labels))
    ) / (total * total)

    kappa = (observed_agreement - expected_agreement) / (1 - expected_agreement) if expected_agreement != 1 else 0.0
    return kappa


# In[77]:


def get_kappa_strength(kappa):
    if kappa < 0:
        return "Less than chance"
    elif 0 <= kappa <= 0.2:
        return "Slight"
    elif 0.2 < kappa <= 0.4:
        return "Fair"
    elif 0.4 < kappa <= 0.6:
        return "Moderate"
    elif 0.6 < kappa <= 0.8:
        return "Substantial"
    elif 0.8 < kappa <= 1:
        return "Almost perfect"
    else:
        return "Invalid kappa value"


# In[84]:


command_line=True
if command_line:
    parser = argparse.ArgumentParser(description="Tutor Evaluation")
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-source_a_col", help="", type=str)
    parser.add_argument("-source_b_col", help="", type=str)    
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-node", action='append')
    args, option_file_index_args = parser.parse_known_args()
    working_dir = args.workingDir
    program_dir = args.programDir
    if working_dir is None:
        working_dir = ".//"
    if program_dir is None:
        program_dir = ".//"
    
    source_a_col = args.source_a_col
    source_b_col = args.source_b_col
    
    #process files for WF:
    if args.node is not None:
        for x in range(len(args.node)):
            if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
                in_file = args.fileIndex[x][1]
                 
else: #for test
    working_dir = ".//"
    program_dir = ".//"
    in_file = "Human and AI output for reacting to student error & dictionary of moves - June 11th 2025 original.csv" 
    source_a_col = "Human"
    source_b_col = "AI"
    
df = pd.read_csv(in_file, encoding='ISO-8859-1')
#convert na to ""
df[source_a_col] = df[source_a_col].fillna('')
df[source_b_col] = df[source_b_col].fillna('')
# Extract columns from DataFrame
source_a = df[source_a_col].tolist()
source_b = df[source_b_col].tolist()

# Compute confusion matrix
labels, cm = confusion_matrix_manual(source_a, source_b)
df_cm = pd.DataFrame(cm, index=labels, columns=labels)
df_cm.index = df_cm.index.map(lambda x: f'{source_a_col}_' + str(x))
df_cm.columns = df_cm.columns.map(lambda x: f'{source_b_col}_' + str(x))
#print(df_cm)
#total for columns
df_cm['Total'] = df_cm.sum(axis=1)
#add the agreement count column
df_cm['Agreement'] = [df_cm.iloc[i, i] for i in range(len(df_cm))]
#total for rows
df_cm.loc['Total'] = df_cm.sum(axis=0)
#percent aggreement
df_cm['Percent of agreement'] = round(df_cm['Agreement']/df_cm['Total'], 2)


# In[85]:


# Metrics
accuracy, precision, recall = calculate_metrics(labels, cm)
df_precision = pd.DataFrame.from_dict(precision, orient='index', columns=['Precision'])
df_precision['Precision'] = round(df_precision['Precision'], 2)
df_precision.index = df_precision.index.map(lambda x: f'{source_a_col}_' + str(x))
df_precision.loc['Total'] = np.nan
df_recall = pd.DataFrame.from_dict(recall, orient='index', columns=['Recall'])
df_recall['Recall'] = round(df_recall['Recall'], 2)
df_recall.index = df_recall.index.map(lambda x: f'{source_a_col}_' + str(x))
df_recall.loc['Total'] = np.nan
df_cm = df_cm.join(df_precision)
df_cm = df_cm.join(df_recall)
output_file = os.path.join(working_dir,'confusion_matrix.csv')
df_cm.to_csv(output_file) 


# In[86]:


#add accuracy and kappa
accuracy = round(accuracy, 2)
kappa = round(calculate_kappa(labels, cm), 2)
kappa_strength = get_kappa_strength(kappa)
with open(output_file, 'a') as f:
    f.write('\n')
    f.write(f'Accuracy: {accuracy}\n')
    f.write(f'Kappa: {kappa}; Strength of Agreement: {kappa_strength}\n')


# In[ ]:




