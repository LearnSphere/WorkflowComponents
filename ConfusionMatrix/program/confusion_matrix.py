#!/usr/bin/env python
# coding: utf-8

# In[54]:


import pandas as pd
import os
import numpy as np
import argparse


# In[55]:


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


# In[56]:


def calculate_metrics(labels, cm):
    total = sum(sum(row) for row in cm)
    correct = sum(cm[i][i] for i in range(len(labels)))
    accuracy = correct / total if total > 0 else 0.0

    precision = {}
    recall = {}
    f1 = {}

    for i, label in enumerate(labels):
        tp = cm[i][i]
        fp = sum(cm[r][i] for r in range(len(labels))) - tp
        fn = sum(cm[i][c] for c in range(len(labels))) - tp

        prec = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        rec = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        f1_score = (2 * prec * rec / (prec + rec)) if (prec + rec) > 0 else 0.0

        precision[label] = prec
        recall[label] = rec
        f1[label] = f1_score

    # If binary classification, return only the "positive" class metrics
    if len(labels) == 2:
        pos_label = labels[1]  # treat the 2nd label as positive
        return {
            "type": "binary",
            "accuracy": accuracy,
            "positive_label": pos_label,
            "precision": precision[pos_label],
            "recall": recall[pos_label],
            "f1": f1[pos_label],
            "confusion_matrix": cm
        }
    else:
        return {
            "type": "multiclass",
            "accuracy": accuracy,
            "precision": precision,
            "recall": recall,
            "f1": f1,
            "confusion_matrix": cm
        }


# In[57]:


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


# In[58]:


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


# In[59]:


def convert_numeric(value):
    if isinstance(value, float) and value.is_integer():
        return int(value)
    return value


# In[60]:


table_style = '''border="1" cellspacing="0" cellpadding="5" 
       style="border-collapse: collapse; text-align: center; font-family: Arial, sans-serif; font-size: 12px; width: auto; min-width: 50%; height: auto; border: 2px solid solid #D0E0F0;"'''
title_row_style = '''style=\"text-align:center; font-size: 14px; background-color: #F0F8FF;\"'''
title_col_style = '''style=\"text-align:center; font-size: 14px; background-color: #F0F8FF; writing-mode: vertical-rl; text-orientation: mixed; text-align: center; vertical-align: middle;\"'''


# In[61]:


def make_inner_html(df_cm, row_source, col_source):
    nrow = df_cm.shape[0] + 1
    ncol = df_cm.shape[1] + 1

    # Start HTML table
    html = f"<table {table_style}>\n"
    #first spanning rows
    #html += f"<tr><td></td><td colspan=\"{ncol}\" style=\"text-align:center; font-weight:bold; background-color: #f2f2f2;\">{col_source}</td></tr>"
    html += f"<tr {title_row_style}><td></td><td colspan=\"{ncol}\">{col_source}</td></tr>"
    # index row
    #html += f" <tr><td rowspan=\"{nrow}\" style='writing-mode: vertical-rl; text-orientation: mixed; text-align: center;vertical-align: middle; background-color: #f2f2f2; '>{row_source}</td>"
    html += f" <tr {title_row_style}><td rowspan=\"{nrow}\" {title_col_style}>{row_source}</td>"
    html += "<td></td>"
    for col in df_cm.columns:
        html += f"<td>{col}</td>"
    html += "</tr>\n"
    # Data rows
    printPct = False
    for idx, row in df_cm.iterrows():
        html += f"  <tr><td {title_row_style}>{idx}</td>"
        for col_name, value in row.items():
            if "percent" in col_name.lower():
                html += f"<td>{row[col_name]:.1%}</td>"
            else:
                html += f"<td>{row[col_name]:.0f}</td>"
        html += "</tr>\n"
        
    
    # End table
    html += "</table>"
    return html


# In[62]:


def metrics_to_html(metrics: dict) -> str:
    """
    Convert binary or multiclass metrics dictionary into an HTML table (manual HTML generation).
    Allows styling control over each row/col.
    """
    #html = '<table class="metrics-table" border="1" cellspacing="0" cellpadding="5">\n'
    html = f'<table {table_style}>\n'
    
    

    if metrics["type"] == "binary":
        # Single row without label
        html += "  <thead>\n"
        html += f"    <tr {title_row_style}><th>Precision</th><th>Recall</th><th>F1</th></tr>\n"
        html += "  </thead>\n"
        html += "  <tbody>\n"
        html += f'    <tr>'
        html += f'<td>{metrics["precision"]:.3f}</td>'
        html += f'<td>{metrics["recall"]:.3f}</td>'
        html += f'<td>{metrics["f1"]:.3f}</td>'
        html += f'</tr>\n'

    elif metrics["type"] == "multiclass":
        html += "  <thead>\n"
        html += f"    <tr {title_row_style}><th></th><th>Precision</th><th>Recall</th><th>F1</th></tr>\n"
        html += "  </thead>\n"
        html += "  <tbody>\n"
        for label in metrics["precision"].keys():
            html += f'    <tr>'
            html += f'<td {title_row_style}">{label}</td>'
            html += f'<td>{metrics["precision"][label]:.3f}</td>'
            html += f'<td>{metrics["recall"][label]:.3f}</td>'
            html += f'<td>{metrics["f1"][label]:.3f}</td>'
            html += f'</tr>\n'

    else:
        raise ValueError("Unknown metrics type")

    html += "  </tbody>\n</table>"

    return html

# #test
# binary_example = {
#     'type': 'binary',
#     'accuracy': 0.94,
#     'positive_label': '1',
#     'precision': 0.8636363636363636,
#     'recall': 1.0,
#     'f1': 0.9268292682926829
# }

# multiclass_example = {
#     'type': 'multiclass',
#     'accuracy': 0.76,
#     'precision': {'': 1.0, '0': 0.6363636363636364, '1': 0.2727272727272727},
#     'recall': {'': 0.9032258064516129, '0': 0.5384615384615384, '1': 0.5},
#     'f1': {'': 0.9491525423728813, '0': 0.5833333333333334, '1': 0.3529411764705882}
# }

# print(metrics_to_html(binary_example))
# #print(metrics_to_html(multiclass_example))


# In[63]:


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
    #in_file = "Human and AI output for reacting to student error & dictionary of moves - June 11th 2025 original.csv" 
    in_file = "ECTEL2025 Transcript Scores human-paper-gpt4o.csv"
    source_a_col = "praise_gpt4o-eval" #praise_gpt4o-present, praise_gpt4o-eval
    source_b_col = "praise_human-eval" #praise_human-present, praise_human-eval
    
df = None
#check if csv or txt
if in_file.lower().endswith('.csv'):
    df = pd.read_csv(in_file, encoding='ISO-8859-1')
elif in_file.lower().endswith('.txt'):
    df = pd.read_csv(in_file, sep='\t', encoding='ISO-8859-1')

#convert na to ""
df[source_a_col] = df[source_a_col].fillna('')
df[source_b_col] = df[source_b_col].fillna('')
#make sure string type
df[source_a_col] = df[source_a_col].apply(convert_numeric).astype(str)
df[source_b_col] = df[source_b_col].apply(convert_numeric).astype(str)
# Extract columns from DataFrame
source_a = df[source_a_col].tolist()
source_b = df[source_b_col].tolist()

# Compute confusion matrix
labels, cm = confusion_matrix_manual(source_a, source_b)
df_cm = pd.DataFrame(cm, index=labels, columns=labels)

#total for columns
df_cm['Total'] = df_cm.sum(axis=1)
#add the agreement count column
df_cm['Agreement'] = [df_cm.iloc[i, i] for i in range(len(df_cm))]
#total for rows
df_cm.loc['Total'] = df_cm.sum(axis=0)
#percent aggreement
df_cm['Percent of agreement'] = round(df_cm['Agreement']/df_cm['Total'], 2)

cm_html = make_inner_html(df_cm, source_a_col, source_b_col)


# In[64]:


metrics = calculate_metrics(labels, cm)
accuracy = metrics["accuracy"]
accuracy = round(accuracy, 2)
kappa = round(calculate_kappa(labels, cm), 2)
kappa_strength = get_kappa_strength(kappa)
sum_html = f'''<p style="font-family: Arial, sans-serif;"><span style="font-weight: bold;">Agreement:</span> {accuracy}; 
                <span style="font-weight: bold;">Kappa:</span> {kappa}; <span style="font-weight: bold;">Kappa Strength:</span> {kappa_strength}</p>'''
metric_html = metrics_to_html(metrics)
all_html = f'''<!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        
    </head>
    <body>
    {cm_html} 
    {sum_html}
    {metric_html}
    </body>
    </html>'''

print(sum_html)


# In[67]:


#output files
output_file = os.path.join(working_dir,'confusion_matrix.csv')
df_cm.to_csv(output_file)
with open("confusion_matrix.html", "w", encoding="utf-8") as f:
    f.write(all_html)


# In[ ]:




