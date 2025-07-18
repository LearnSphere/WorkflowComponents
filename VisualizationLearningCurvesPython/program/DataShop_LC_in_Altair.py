#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np
import altair as alt
import argparse
import re
import xml.etree.ElementTree as ET
import os
import math


# In[2]:


def getKCModelColumnName (modelName):
    return 'KC (' + modelName + ')'


# In[3]:


def getOpportunityColumnName (modelName):
    return 'Opportunity (' + modelName +')'


# In[4]:


def getPredictedErrorColumnName (modelName):
    return 'Predicted Error Rate (' + modelName + ')'


# In[5]:


def strip_model_name(model_name):
    match = re.search(r'\((.*?)\)', model_name)
    if match is None:
        return model_name
    else:
        return match.group(1)

# print(strip_model_name("KC (Orignial)"))
# print(strip_model_name("Predicted Error Rate (Orignial)"))
# print(strip_model_name("Orignial"))


# In[6]:


def int_or_inf(value):
    if value.lower() in ("inf", "infinity"):
        return float("inf")  # Represent infinity
    return int(value)  # Otherwise, convert to an integer


# In[7]:


def safe_str_to_float(s):
    try:
        return float(s)
    except ValueError:
        return None


# In[8]:


def sanitize_filename(filename):
    # Define invalid characters based on cross-platform rules
    invalid_chars = r'[\\/:*?"<>|]'
    # Replace invalid characters with an underscore
    sanitized = re.sub(invalid_chars, '_', filename)
    # Remove leading or trailing spaces and periods
    sanitized = sanitized.strip(' .')
    #remove space and comma and quotes just in case
    sanitized = re.sub(r"[ ,',\"]", "_", sanitized)
    return sanitized
# filename = "example:i,n valid|file*name?that\"this'.txt"
# safe_filename = sanitize_filename(filename)
# print(safe_filename)


# In[9]:


#detect if the model is multi-skilled
def multiskilled_detector (df, model):
    # Filter rows where the KC (model) column contains '~~'
    kc_model = getKCModelColumnName(model)
    filtered_df = df[df[kc_model].str.contains('~~', case=False, na=False)]
    if filtered_df is None or filtered_df.empty:
        return False
    else:
        return True

# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# print(multiskilled_detector(df, "Original"))
# print(multiskilled_detector(df, "Lasso Model"))


# In[10]:


#turn the multi-skilled row into multiple rows
#the columns to be repeated: Anon Student Id, First Attempt,  Predicted Error Rate (model)
#the columns to be changed: KC (model), Opportunity (model)
def multiskilled_converter (df, model):
    # Split  KC (model) and Opportunity (model) columns by ~~
    model_col_name = getKCModelColumnName(model)
    opportunity_col_name = getOpportunityColumnName(model)
    
    df[model_col_name] = df[model_col_name].str.split('~~')
    df[opportunity_col_name] = df[opportunity_col_name].str.split('~~')
    predicted_error_rate_col_name = 'Predicted Error Rate (' + model + ')'
    anon_student_id_col_name = 'Anon Student Id'
    first_attempt_col_name = 'First Attempt'
    # Expand rows for each item in the lists in KC (model)
    df_expanded = pd.DataFrame({
        anon_student_id_col_name: df[anon_student_id_col_name].repeat(df[model_col_name].str.len()),   # Repeat the anon_student_id based on the length of each list
        first_attempt_col_name: df[first_attempt_col_name].repeat(df[model_col_name].str.len()),  
        predicted_error_rate_col_name: df[predicted_error_rate_col_name].repeat(df[model_col_name].str.len()), 
        model_col_name: [item for sublist in df[model_col_name] for item in sublist],  # Flatten the lists of values
        opportunity_col_name: [item for sublist in df[opportunity_col_name] for item in sublist]  
    }).reset_index(drop=True)
    df_expanded[opportunity_col_name] = pd.to_numeric(df_expanded[opportunity_col_name], errors='coerce')
    return df_expanded

# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# print(multiskilled_converter (df, "Lasso Model"))


# In[11]:


#clean up a dataframe: multi-skill conversion and first attempt conversion
def clean_df (df, model):
    #multiskill conversion
    if multiskilled_detector(df, model):
        df = multiskilled_converter(df, model)
    df['First Attempt Num'] = [1 if x =='correct' else 0 for x in df['First Attempt']]
    opp_column_name = getOpportunityColumnName(model)
    #delete the rows that opportunity is none or not number
    # Step 1: Drop rows where opp is NaN or None
    df = df.dropna(subset=[opp_column_name])
    # Step 2: Drop rows where opp is not a number
    df = df[pd.to_numeric(df[opp_column_name], errors='coerce').notna()]
    return df


# In[12]:


def add_thumb_prints_to_html(file_path, thumb_print_html):
    with open(file_path, "r") as file:
        content = file.readlines()
    # Locate the `</body>` tag and insert the text before it
    for i in range(len(content)):
        if content[i].strip() == "</body>":
            content.insert(i, f"\n{thumb_print_html}\n") 
            break

    # Write the modified content back to the file
    with open(file_path, "w") as file:
        file.writelines(content)


# In[33]:


def extract_chart_parts(html_content, chart_id):
    """Extract and rename the div and script for unique chart embedding."""
    # Change the div id
    html_content = re.sub(r'id="vis"', f'id="{chart_id}"', html_content)

    # Change the embed call to use the new id
    html_content = re.sub(r'vegaEmbed\("#vis"', f'vegaEmbed("#{chart_id}"', html_content)

    # Extract <body> content only
    body_start = html_content.find('<body>') + len('<body>')
    body_end = html_content.find('</body>')
    body_content = html_content[body_start:body_end].strip()

    return body_content
# Example usage:
# with open("main_graph.html", 'r', encoding='utf-8') as f1:
#         html = f1.read()
# print(extract_chart_parts(html, "vis1"))


# In[34]:


def write_main_chart_html(main_html_file):
    #process the main chart
    with open(main_html_file, 'r', encoding='utf-8') as f:
        main_orig_html = f.read()
    main_html = extract_chart_parts(main_orig_html, "all")
    main_html = f'''
        <div style="margin-bottom: 40px;">
                    {main_html}
        </div>'''
    return main_html


# In[35]:


def category_html(element_file_dict, skill_categories):
    combined_html = ""
    if element_file_dict is not None and len(element_file_dict) > 0 and len(skill_categories) > 0:
        for category, elements in skill_categories.items():
            if elements is not None and len(elements) > 0:
                category_element_file_dict = {k: element_file_dict[k] for k in elements if k in element_file_dict}
                category_thrumbprint_html = no_category_html(category_element_file_dict)
                formatted_category = category.replace('_', ' ').title()
                combined_html = f'''{combined_html}\n
                    <div style="clear:left">
                        <hr>
                    </div>
                    <div id="perCategoryThumbs_{category}" style="clear:left">
                        <p class="classified_label">
                        <span style="font-weight: bold; font-size: 14px; font-family: Arial, sans-serif;">
                        {formatted_category}</span></p>
                    </div>
                    {category_thrumbprint_html}
                '''
    return combined_html
# # Example usage:
# my_dict = {'ALT:CIRCLE-AREA': 'ALT_CIRCLE-AREA', 'ALT:CIRCLE-CIRCUMFERENCE': 'ALT_CIRCLE-CIRCUMFERENCE', 'ALT:CIRCLE-DIAMETER': 'ALT_CIRCLE-DIAMETER', 'ALT:CIRCLE-RADIUS': 'ALT_CIRCLE-RADIUS', 'ALT:COMPOSE-BY-ADDITION': 'ALT_COMPOSE-BY-ADDITION', 'ALT:COMPOSE-BY-MULTIPLICATION': 'ALT_COMPOSE-BY-MULTIPLICATION', 'ALT:PARALLELOGRAM-AREA': 'ALT_PARALLELOGRAM-AREA', 'ALT:PARALLELOGRAM-SIDE': 'ALT_PARALLELOGRAM-SIDE', 'ALT:PENTAGON-AREA': 'ALT_PENTAGON-AREA', 'ALT:PENTAGON-SIDE': 'ALT_PENTAGON-SIDE', 'ALT:TRAPEZOID-AREA': 'ALT_TRAPEZOID-AREA', 'ALT:TRAPEZOID-BASE': 'ALT_TRAPEZOID-BASE', 'ALT:TRAPEZOID-HEIGHT': 'ALT_TRAPEZOID-HEIGHT', 'ALT:TRIANGLE-AREA': 'ALT_TRIANGLE-AREA', 'ALT:TRIANGLE-SIDE': 'ALT_TRIANGLE-SIDE'}
# skill_categories = {'too_little_data': [], 'low_flat': ['ALT:PARALLELOGRAM-SIDE'], 'still_high': ['ALT:TRAPEZOID-AREA', 'ALT:TRAPEZOID-HEIGHT'], 'no_learning': ['ALT:COMPOSE-BY-ADDITION'], 'good': ['ALT:CIRCLE-RADIUS', 'ALT:CIRCLE-AREA', 'ALT:COMPOSE-BY-MULTIPLICATION', 'ALT:CIRCLE-DIAMETER', 'ALT:TRIANGLE-SIDE', 'ALT:PARALLELOGRAM-AREA', 'ALT:CIRCLE-CIRCUMFERENCE', 'ALT:TRIANGLE-AREA', 'ALT:PENTAGON-AREA', 'ALT:PENTAGON-SIDE', 'ALT:TRAPEZOID-BASE']}
# html_table = category_html(my_dict, skill_categories)
# combined_html_head = '''
#     <!DOCTYPE html>
#     <html>
#     <head>
#         <meta charset="utf-8">
#         <script src="https://cdn.jsdelivr.net/npm/vega@5"></script>
#         <script src="https://cdn.jsdelivr.net/npm/vega-lite@5"></script>
#         <script src="https://cdn.jsdelivr.net/npm/vega-embed@6"></script>
#     </head>
#     <body>'''
# combined_html_tail = '''</body>
#     </html>'''
# print(f"{combined_html_head}\n{html_table}\n{combined_html_tail}")



# In[36]:


def no_category_html(element_file_dict):
    if element_file_dict is None or len(element_file_dict) == 0:
        return ""
    else:
        html = ''
        for i, (key, value) in enumerate(element_file_dict.items()):
            #the element html
            element_file = os.path.join(working_dir, f"{value}.html")
            with open(element_file, 'r', encoding='utf-8') as f:
                element_html = f.read()
            element_chart = extract_chart_parts(element_html, f"{value}")    
            html = f'''{html}\n
                    <div style="margin-bottom: 40px;">\n
                    {element_chart}\n
                    </div>'''
        return html
# # Example usage:
# working_dir = "."
# my_dict = {'ALT:CIRCLE-AREA': 'ALT_CIRCLE-AREA', 'ALT:CIRCLE-CIRCUMFERENCE': 'ALT_CIRCLE-CIRCUMFERENCE', 'ALT:CIRCLE-DIAMETER': 'ALT_CIRCLE-DIAMETER', 'ALT:CIRCLE-RADIUS': 'ALT_CIRCLE-RADIUS', 'ALT:COMPOSE-BY-ADDITION': 'ALT_COMPOSE-BY-ADDITION', 'ALT:COMPOSE-BY-MULTIPLICATION': 'ALT_COMPOSE-BY-MULTIPLICATION', 'ALT:PARALLELOGRAM-AREA': 'ALT_PARALLELOGRAM-AREA', 'ALT:PARALLELOGRAM-SIDE': 'ALT_PARALLELOGRAM-SIDE', 'ALT:PENTAGON-AREA': 'ALT_PENTAGON-AREA', 'ALT:PENTAGON-SIDE': 'ALT_PENTAGON-SIDE', 'ALT:TRAPEZOID-AREA': 'ALT_TRAPEZOID-AREA', 'ALT:TRAPEZOID-BASE': 'ALT_TRAPEZOID-BASE', 'ALT:TRAPEZOID-HEIGHT': 'ALT_TRAPEZOID-HEIGHT', 'ALT:TRIANGLE-AREA': 'ALT_TRIANGLE-AREA', 'ALT:TRIANGLE-SIDE': 'ALT_TRIANGLE-SIDE'}
# html_table = no_category_html(my_dict)
# combined_html_head = '''
#     <!DOCTYPE html>
#     <html>
#     <head>
#         <meta charset="utf-8">
#         <script src="https://cdn.jsdelivr.net/npm/vega@5"></script>
#         <script src="https://cdn.jsdelivr.net/npm/vega-lite@5"></script>
#         <script src="https://cdn.jsdelivr.net/npm/vega-embed@6"></script>
#     </head>
#     <body>'''
# combined_html_tail = '''</body>
#     </html>'''
# print(f"{combined_html_head}\n{html_table}\n{combined_html_tail}")


# In[37]:


#get the first level of aggregation: either by KC or student + opp
#allowed value for group_by is Knowledge Components or Students
#aggregate_measures, allowed values: "Error Rate", "Assistance Score", "Number of Incorrects", "Number of Hints", "Step Duration", "Correct Step Duration", "Error Step Duration"
#error_bar, allowed values: "No Error Bars", "Standard Deviation", "Standard Error"
def get_df_kc_opp_aggr(df, model, group_by = 'Knowledge Components', aggregate_meatures = "Error Rate", error_bar = "No Error Bars"):
    kc = getKCModelColumnName(model)
    kc_opportunity = getOpportunityColumnName(model)
    kc_predicted_error_rate = getPredictedErrorColumnName(model)
    group_by_column = ""
    if group_by.lower() == 'students':
        group_by_column = 'Anon Student Id'
    else:
        group_by_column = kc
    #get avg of predicted error rate and avg of first attempt for each "kc + opp"
    if aggregate_meatures == "Assistance Score":
        df["Assistance Score"] = df["Incorrects"] + df["Hints"]
    elif aggregate_meatures == "Step Duration":
        df['Step Duration (sec)'] = pd.to_numeric(df['Step Duration (sec)'], errors='coerce')
        df = df[df['Step Duration (sec)'].notna()]
    elif aggregate_meatures == "Correct Step Duration":
        df['Correct Step Duration (sec)'] = pd.to_numeric(df['Correct Step Duration (sec)'], errors='coerce')
        df = df[df['Correct Step Duration (sec)'].notna()]
    elif aggregate_meatures == "Error Step Duration":
        df['Error Step Duration (sec)'] = pd.to_numeric(df['Error Step Duration (sec)'], errors='coerce')
        df = df[df['Error Step Duration (sec)'].notna()]
    grouped = df.groupby([group_by_column, kc_opportunity])
    
    if aggregate_meatures == "Assistance Score":
        if error_bar == "No Error Bars":
            errrate = grouped[['Assistance Score']].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['Assistance Score']].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Assistance Score_mean': 'Assistance Score', 'Assistance Score_std': 'Error Bar'})
        elif error_bar == "Standard Error":
            errrate = grouped[['Assistance Score']].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Assistance Score_mean': 'Assistance Score', 'Assistance Score_<lambda_0>': 'Error Bar'})
        errrate = errrate.reset_index()
    elif aggregate_meatures == "Number of Incorrects":
        if error_bar == "No Error Bars":
            errrate = grouped[['Incorrects']].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['Incorrects']].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Incorrects_mean': 'Incorrects', 'Incorrects_std': 'Error Bar'})
        elif error_bar == "Standard Error":
            errrate = grouped[['Incorrects']].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Incorrects_mean': 'Incorrects', 'Incorrects_<lambda_0>': 'Error Bar'})
        errrate = errrate.reset_index()
    elif aggregate_meatures == "Number of Hints":
        if error_bar == "No Error Bars":
            errrate = grouped[['Hints']].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['Hints']].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Hints_mean': 'Hints', 'Hints_std': 'Error Bar'})
        elif error_bar == "Standard Error":
            errrate = grouped[['Hints']].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Hints_mean': 'Hints', 'Hints_<lambda_0>': 'Error Bar'})
        errrate = errrate.reset_index()
    elif aggregate_meatures == "Step Duration":
        if error_bar == "No Error Bars":
            errrate = grouped[['Step Duration (sec)']].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['Step Duration (sec)']].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Step Duration (sec)_mean': 'Step Duration (sec)', 'Step Duration (sec)_std': 'Error Bar'})
        elif error_bar == "Standard Error":
            errrate = grouped[['Step Duration (sec)']].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Step Duration (sec)_mean': 'Step Duration (sec)', 'Step Duration (sec)_<lambda_0>': 'Error Bar'})
        errrate = errrate.reset_index()
    elif aggregate_meatures == "Correct Step Duration":
        if error_bar == "No Error Bars":
            errrate = grouped[['Correct Step Duration (sec)']].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['Correct Step Duration (sec)']].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Correct Step Duration (sec)_mean': 'Correct Step Duration (sec)', 'Correct Step Duration (sec)_std': 'Error Bar'})
        elif error_bar == "Standard Error":
            errrate = grouped[['Correct Step Duration (sec)']].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Correct Step Duration (sec)_mean': 'Correct Step Duration (sec)', 'Correct Step Duration (sec)_<lambda_0>': 'Error Bar'})
        errrate = errrate.reset_index()
    elif aggregate_meatures == "Error Step Duration":
        if error_bar == "No Error Bars":
            errrate = grouped[['Error Step Duration (sec)']].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['Error Step Duration (sec)']].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Error Step Duration (sec)_mean': 'Error Step Duration (sec)', 'Error Step Duration (sec)_std': 'Error Bar'})
        elif error_bar == "Standard Error":
            errrate = grouped[['Error Step Duration (sec)']].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'Error Step Duration (sec)_mean': 'Error Step Duration (sec)', 'Error Step Duration (sec)_<lambda_0>': 'Error Bar'})
        errrate = errrate.reset_index()
    else:
        if error_bar == "No Error Bars":
            errrate = grouped[['First Attempt Num', kc_predicted_error_rate]].agg(np.mean)
        elif error_bar == "Standard Deviation":
            errrate = grouped[['First Attempt Num', kc_predicted_error_rate]].agg(['mean', 'std'])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'First Attempt Num_mean': 'First Attempt Num', 'First Attempt Num_std': 'Error Bar',
                                              kc_predicted_error_rate + "_mean":kc_predicted_error_rate,
                                              kc_predicted_error_rate + "_std":'Predicted Error Bar',
                                             })
        elif error_bar == "Standard Error":
            errrate = grouped[['First Attempt Num', kc_predicted_error_rate]].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            errrate.columns = ['_'.join(col).strip() for col in errrate.columns.values]
            errrate = errrate.rename(columns={'First Attempt Num_mean': 'First Attempt Num', 'First Attempt Num_<lambda_0>': 'Error Bar',
                                              kc_predicted_error_rate + "_mean":kc_predicted_error_rate,
                                              kc_predicted_error_rate + "_<lambda_0>":'Predicted Error Bar',
                                             })
        errrate = errrate.reset_index()
        errrate['Error Rate'] = 1 - errrate['First Attempt Num']
    #get count for each "kc + opp"
    counts = grouped.size()
    counts = counts.reset_index()
    counts = counts.rename(columns={0: 'Count'})
    #add count to errrate
    errrate['Count'] = counts['Count']
    return errrate

# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# df['First Attempt Num'] = [1 if x =='correct' else 0 for x in df['First Attempt']]
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Rate", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Rate", 'Standard Deviation'))
# print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Rate", 'Standard Error'))

#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'Standard Deviation'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'Standard Error'))

#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Incorrects", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Incorrects", 'Standard Deviation'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Incorrects", 'Standard Error'))

#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Hints", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Hints", 'Standard Deviation'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Hints", 'Standard Error'))

#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Step Duration", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Step Duration", 'Standard Deviation'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Step Duration", 'Standard Error'))

#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Correct Step Duration", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Correct Step Duration", 'Standard Deviation'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Correct Step Duration", 'Standard Error'))

#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Step Duration", 'No Error Bars'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Step Duration", 'Standard Deviation'))
#print(get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Step Duration", 'Standard Error'))


# In[38]:


#get the second level of aggregation: by KC
#input df should have these columns: same as the df returned from get_df_kc_opp_aggr
#column_to_average:"Error Rate", "Predicted Error Rate", "Assistance Score", "Number of Incorrects", "Number of Hints", "Step Duration", "Correct Step Duration", "Error Step Duration"
#error_bar, allowed values: "No Error Bars", "Standard Deviation", "Standard Error"
def get_df_opp_aggr(df, model, column_to_average = "Error Rate", error_bar = "No Error Bars"):
    kc = getKCModelColumnName(model)
    kc_opportunity = getOpportunityColumnName(model)
    kc_predicted_error_rate = getPredictedErrorColumnName(model)
    if column_to_average == "Predicted Error Rate":
        column_to_average = column_to_average + " (" + model +")"
    elif column_to_average == "Number of Incorrects":
        column_to_average ="Incorrects"
    elif column_to_average == "Number of Hints":
        column_to_average ="Hints"
    elif column_to_average == "Step Duration":
        column_to_average ="Step Duration (sec)"
    elif column_to_average == "Correct Step Duration":
        column_to_average ="Correct Step Duration (sec)"
    elif column_to_average == "Error Step Duration":
        column_to_average ="Error Step Duration (sec)"
    
    #aggregate for opp    
    grouped = df.groupby([kc_opportunity])
    if error_bar == "No Error Bars":
        aggr_df_by_opportunity = grouped[[column_to_average]].agg(np.mean)
    elif error_bar == "Standard Deviation":
            aggr_df_by_opportunity = grouped[[column_to_average]].agg(['mean', 'std'])
            aggr_df_by_opportunity.columns = ['_'.join(col).strip() for col in aggr_df_by_opportunity.columns.values]
            if "Predicted Error Rate" in column_to_average:
                aggr_df_by_opportunity = aggr_df_by_opportunity.rename(columns={column_to_average + '_mean': column_to_average, 
                                                             column_to_average +'_std': 'Predicted Error Bar' })
            else:
                aggr_df_by_opportunity = aggr_df_by_opportunity.rename(columns={column_to_average + '_mean': column_to_average, 
                                                             column_to_average +'_std': 'Error Bar' })
    elif error_bar == "Standard Error":
            aggr_df_by_opportunity = grouped[[column_to_average]].agg(['mean', lambda x: x.std(ddof=1) / np.sqrt(len(x))])
            aggr_df_by_opportunity.columns = ['_'.join(col).strip() for col in aggr_df_by_opportunity.columns.values]
            if "Predicted Error Rate"  in column_to_average:
                aggr_df_by_opportunity = aggr_df_by_opportunity.rename(columns={column_to_average + '_mean': column_to_average, 
                                                                            column_to_average + '_<lambda_0>': 'Predicted Error Bar'})
            else:
                aggr_df_by_opportunity = aggr_df_by_opportunity.rename(columns={column_to_average + '_mean': column_to_average, 
                                                                            column_to_average + '_<lambda_0>': 'Error Bar'})
    aggr_df_by_opportunity = aggr_df_by_opportunity.reset_index()
    #get total count for each opp
    totcounts = grouped['Count'].sum().reset_index()
    #combine the total count
    aggr_df_by_opportunity['Count'] = totcounts['Count']
    return aggr_df_by_opportunity

# #for test
# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# group_by = "Knowledge Components"
# #group_by = "Students"
# model = 'Original'
# df = clean_df(df, model)

# #test error rate without error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
# print(df_aggr)
# print(get_df_opp_aggr(df_aggr, "Original", column_to_average = "Error Rate", error_bar = "No Error Bars"))

#test error rate with standard error
#df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
#print(df_aggr)
#print(get_df_opp_aggr(df_aggr, "Original", column_to_average = "Error Rate", error_bar = "Standard Deviation"))

#test predicted error rate with standard error
#df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
#print(df_aggr)
#print(get_df_opp_aggr(df_aggr, "Original", column_to_average = "Predicted Error Rate", error_bar = "Standard Deviation"))

#test assistance score with standard error
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'Standard Error')
#print(df_aggr)
#print(get_df_opp_aggr(df_aggr, "Original", column_to_average = "Assistance Score", error_bar = "Standard Deviation"))

#test assistance score with standard error
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Step Duration", 'Standard Error')
#print(df_aggr)
#print(get_df_opp_aggr(df_aggr, "Original", column_to_average = "Error Step Duration", error_bar = "Standard Deviation"))

#test assistance score with standard error
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Incorrects", 'Standard Error')
#print(df_aggr)
#print(get_df_opp_aggr(df_aggr, "Original", column_to_average = "Number of Incorrects", error_bar = "Standard Deviation"))


# In[39]:


#draw learning curve
#df should have these columns: Opportunity (model), "Error Rate" or "Predicted Error Rate (Model)" or Assistance Score
#or "Incorrects" or "Hints" or "Step Duration (sec)" or "Correct Step Duration (sec)" or "Error Step Duration (sec)" and Count
def draw_error_rate_line (df, 
                          model, 
                          y_axis, #specfy which column to use for y-axis: "Error Rate", "Predicted Error Rate", "Assistance Score", "Number of Incorrects", "Number of Hints", "Step Duration", "Correct Step Duration", "Error Step Duration"
                          graph_title, #graph's title, leave blank if no title  
                          x_axis_title, #x_axis title, leave blank if no title
                          y_axis_title, #y-axis title, leave blank if no title
                          legend_title, #legend title, leave blank if no title
                          width, 
                          height, 
                          add_legend = True, #add legend to graph
                          point = True, #add point to graph
                          error_bar = False): #to add error bar
    kc_opportunity = getOpportunityColumnName(model)
    x_axis_modified_for_legend = ""
    y_axis_modified = ""
    if y_axis == "Error Rate":
        #y-axis's column name is "Error Rate"
        y_axis_modified = y_axis
        x_axis_modified_for_legend = model + " (error rate)"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Predicted Error Rate":
        #y-axis's column name is "Predicted Error Rate (model)"
        y_axis_modified = y_axis + ' (' + model + ')'
        x_axis_modified_for_legend = model + " (predicted error rate)"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Assistance Score":
        #change assistance score column name 
        df = df.rename(columns={'Assistance Score': 'Assistance Score Value'})
        #use "Assistance Score Value" as y-axis column
        y_axis_modified = "Assistance Score Value"
        x_axis_modified_for_legend = "Assistance Score"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Number of Incorrects":
        #change column name 
        df = df.rename(columns={'Incorrects': 'Incorrects Value'})
        #use "Incorrects Value" as y-axis column
        y_axis_modified = "Incorrects Value"
        x_axis_modified_for_legend = "Incorrects"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Number of Hints":
        #change column name 
        df = df.rename(columns={'Hints': 'Hints Value'})
        #use "Hints Value" as y-axis column
        y_axis_modified = "Hints Value"
        x_axis_modified_for_legend = "Hints"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Step Duration":
        #change column name 
        df = df.rename(columns={'Step Duration (sec)': 'Step Duration Value'})
        #use "Step Duration Value" as y-axis column
        y_axis_modified = "Step Duration Value"
        x_axis_modified_for_legend = "Step Duration"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Correct Step Duration":
        #change column name 
        df = df.rename(columns={'Correct Step Duration (sec)': 'Correct Step Duration Value'})
        #use "Step Duration Value" as y-axis column
        y_axis_modified = "Correct Step Duration Value"
        x_axis_modified_for_legend = "Correct Step Duration"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    elif y_axis == "Error Step Duration":
        #change column name 
        df = df.rename(columns={'Error Step Duration (sec)': 'Error Step Duration Value'})
        #use "Step Duration Value" as y-axis column
        y_axis_modified = "Error Step Duration Value"
        x_axis_modified_for_legend = "Error Step Duration"
        #change opportunity col name for legend prupose
        df[x_axis_modified_for_legend] = df[kc_opportunity]
    
    #get the domain_min and domain_max
    domain_min = 0
    domain_max = df[y_axis_modified].max()
    if domain_max <= 1:
        domain_max = 1
    elif domain_max <= 10 and domain_max > 1:
        domain_max = 10
    
    if add_legend is not None and not add_legend:
        error_rate_chart = alt.Chart(df).transform_calculate(
            color='"' + x_axis_modified_for_legend + '"').mark_line(point=point).encode(
            alt.X(x_axis_modified_for_legend, title=x_axis_title), 
            alt.Y(y_axis_modified, title=y_axis_title, scale=alt.Scale(domain=[domain_min, domain_max])),
              color = alt.Color('color:N', legend=None),
              tooltip=[alt.Tooltip(kc_opportunity, title="Opportunity"), alt.Tooltip(y_axis_modified, title=y_axis), alt.Tooltip('Count', title="Number of Observations")]
            ).properties(
                title=graph_title,
                width=width,
                height=height
            )
    else:
        error_rate_chart = alt.Chart(df).transform_calculate(
            color='"' + x_axis_modified_for_legend + '"').mark_line(point=point).encode(
            alt.X(x_axis_modified_for_legend, title=x_axis_title), 
            alt.Y(y_axis_modified, title=y_axis_title, scale=alt.Scale(domain=[domain_min, domain_max])),
              color = alt.Color('color:N', legend=alt.Legend(title=legend_title, orient='bottom', direction='vertical')),
              tooltip=[alt.Tooltip(kc_opportunity, title="Opportunity"), alt.Tooltip(y_axis_modified, title=y_axis), alt.Tooltip('Count', title="Number of Observations")]
            ).properties(
                title=graph_title,
                width=width,
                height=height
            )
    # Define error bars
    if error_bar is not None and error_bar == True:
        if y_axis == "Predicted Error Rate":
            error_bars = alt.Chart(df).mark_errorbar(color='black').encode(
                x=x_axis_modified_for_legend,
                y=y_axis_modified,
                yError='Predicted Error Bar')
        else:
            error_bars = alt.Chart(df).mark_errorbar(color='black').encode(
                x=x_axis_modified_for_legend,
                y=y_axis_modified,
                yError='Error Bar')
        error_rate_chart = alt.layer(error_rate_chart, error_bars)
    return error_rate_chart

# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# df = clean_df(df, 'Original')

# #test error rate, no error bar
# df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Rate", "No Error Bars")
# df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
# print(df_aggr)
# #draw error rate with point and legend
# error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Rate', 'All', 'Opportunity', 'Error Rate', 'Legend title', 600, 400, add_legend=True, point=True)
#draw error rate without point and legend
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Rate', 'All', 'Opportunity', 'Error Rate', 'Legend title', 600, 400, add_legend=False, point=False)
#draw predicted error rate
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Predicted Error Rate', 'All', 'Opportunity', 'Predicted Error Rate', 'Legend title', 600, 400, add_legend=True, point=True)

#test error rate and standard deviation or standard error for error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", aggregate_meatures = "Error Rate", error_bar ="Standard Deviation")
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", aggregate_meatures = "Error Rate", error_bar ="Standard Error")
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw error rate with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Rate', 'All', 'Opportunity', 'Error Rate', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)
#draw predicted error rate with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Predicted Error Rate', 'All', 'Opportunity', 'Predicted Error Rate', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#test error and predicted on the same graphic
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Rate", "No Error Bars")
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Rate", "Standard Error")
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Rate', 'All', 'Opportunity', 'Error Rate', 'Legend title', 600, 400, add_legend=True, point=True)
#pred_error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Predicted Error Rate', 'All', 'Opportunity', 'Error Rate', 'Legend title', 600, 400, add_legend=True, point=True)
#final_error_rate_chart = alt.layer(error_rate_graph, pred_error_rate_graph)
#final_error_rate_chart
#with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Rate', 'All', 'Opportunity', '', '', 600, 400, add_legend=True, point=True, error_bar=True)
#pred_error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Predicted Error Rate', 'All', 'Opportunity', '', '', 600, 400, add_legend=True, point=True, error_bar=True)
#final_error_rate_chart = alt.layer(error_rate_graph, pred_error_rate_graph)
#final_error_rate_chart

#test assistance score with and without error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'No Error Bars')
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'Standard Deviation')
# df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Assistance Score", 'Standard Error')
# df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw assistance score
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Assistance Score', 'All', 'Opportunity', 'Assistance Score', 'Legend title', 600, 400, add_legend=True, point=True)
#draw assistance score with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Assistance Score', 'All', 'Opportunity', 'Assistance Score', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#test incorrects with and without error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Incorrects", 'No Error Bars')
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Incorrects", 'Standard Deviation')
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw incorrects
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Number of Incorrects', 'All', 'Opportunity', 'whatever', 'Legend title', 600, 400, add_legend=True, point=True)
#draw incorrects with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Number of Incorrects', 'All', 'Opportunity', 'Assistance Score', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#test Number of Hints with and without error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Hints", 'No Error Bars')
# df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Number of Hints", 'Standard Deviation')
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw Hints
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Number of Hints', 'All', 'Opportunity', 'whatever', 'Legend title', 600, 400, add_legend=True, point=True)
#draw hints with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Number of Hints', 'All', 'Opportunity', 'hints', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#test Step duration with and without error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Step Duration", 'No Error Bars')
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Step Duration", 'Standard Error')
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw step duration
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Step Duration', 'All', 'Opportunity', 'whatever', 'Legend title', 600, 400, add_legend=True, point=True)
#draw step duration with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Step Duration', 'All', 'Opportunity', 'hints', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#test Correct Step Duration with and without error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Correct Step Duration", 'No Error Bars')
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Correct Step Duration", 'Standard Error')
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw correct step duration
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Correct Step Duration', 'All', 'Opportunity', 'whatever', 'Legend title', 600, 400, add_legend=True, point=True)
#draw correct step duration with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Correct Step Duration', 'All', 'Opportunity', 'hints', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#test Error Step Duration with and without error bar
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Step Duration", 'No Error Bars')
#df_aggr = get_df_kc_opp_aggr(df, "Original", "Knowledge Components", "Error Step Duration", 'Standard Error')
#df_aggr = df_aggr[df_aggr['KC (Original)'] == 'ALT:CIRCLE-AREA']
#print(df_aggr)
#draw correct step duration
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Step Duration', 'All', 'Opportunity', 'whatever', 'Legend title', 600, 400, add_legend=True, point=True)
#draw correct step duration with error bar
#error_rate_graph = draw_error_rate_line(df_aggr, 'Original', 'Error Step Duration', 'All', 'Opportunity', 'hints', 'Legend title', 600, 400, add_legend=True, point=True, error_bar = True)

#error_rate_graph


# In[40]:


#create graph for a model
#opp_aggr_df has these columns: KC (Original), Opportunity (Original), First Attempt Num, Predicted Error Rate (Original), Error Rate (or other values depending on line_to_display), Count
def create_model_lc_chart(opp_aggr_df, model, 
                          group_by_type, #Knowledge Components or Students
                          line_to_display, #specfy which column to use for y-axis: "Error Rate", "Predicted Error Rate", "Error Rate/Predicted Error Rate", "Assistance Score", "Number of Incorrects", "Number of Hints", "Step Duration", "Correct Step Duration", "Error Step Duration"
                          legend_title, #legend title
                          width, height,
                          add_legend=True, #false to add no legend
                          graph_title=None, #graph title
                          x_axis_title=None, #x-axis title
                          y_axis_title=None, #y-axis title
                          point=True,
                          error_bar="No Error Bars"): #"No Error Bars", "Standard Deviation", "Standard Error"
    #aggregate for opp    
    kc_opportunity = getOpportunityColumnName(model)
    kc_predicted_error_rate = getPredictedErrorColumnName(model)
    return_graphs = []
    
    add_error_bar = False
    if error_bar != "No Error Bars":
        add_error_bar = True
    
    if line_to_display == "Error Rate/Predicted Error Rate":
        df_aggr_by_opportunity = get_df_opp_aggr(opp_aggr_df, model, column_to_average = "Error Rate", error_bar = error_bar)
        error_rate_chart = draw_error_rate_line (df_aggr_by_opportunity, model, "Error Rate", 
                                             graph_title, x_axis_title, y_axis_title, legend_title, 
                                             width, height, 
                                             add_legend=add_legend, point=point, error_bar=add_error_bar)
        if error_rate_chart is not None:
            return_graphs.append(error_rate_chart)
        pred_df_aggr_by_opportunity = get_df_opp_aggr(opp_aggr_df, model, column_to_average = "Predicted Error Rate", error_bar = error_bar)
        predicted_error_rate_chart = draw_error_rate_line (pred_df_aggr_by_opportunity, model, 'Predicted Error Rate', 
                                                           graph_title, x_axis_title, y_axis_title, legend_title, 
                                                           width, height, 
                                                           add_legend=add_legend, point=point, error_bar=add_error_bar)
        if predicted_error_rate_chart is not None:
            return_graphs.append(predicted_error_rate_chart)
    else:
        df_aggr_by_opportunity = get_df_opp_aggr(opp_aggr_df, model, column_to_average = line_to_display, error_bar = error_bar)
        error_rate_chart = draw_error_rate_line (df_aggr_by_opportunity, model, line_to_display, 
                                             graph_title, x_axis_title, y_axis_title, legend_title, 
                                             width, height, 
                                             add_legend=add_legend, point=point, error_bar=add_error_bar)
        if error_rate_chart is not None:
            return_graphs.append(error_rate_chart)
    return return_graphs
    
# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# group_by = "Knowledge Components"
# #group_by = "Students"
# model = 'Original'
# df = clean_df(df, model)

# #test error rate, no error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="Error Rate", #y-axis title
#                                point=True,
#                                error_bar="No Error Bars")

# #test error rate, with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="Error Rate", #y-axis title
#                                point=True,
#                                error_bar="Standard Deviation")

#test predicted error rate, with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Predicted Error Rate", "Standard Deviation")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Predicted Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                                error_bar="Standard Error")

#test no legend, no titles, no points
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Predicted Error Rate", "Standard Deviation")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Error Rate', 
#                                legend_title = "Legend", 
#                                width = 175, height = 90, 
#                                add_legend=False,
#                                graph_title="", #graph title
#                                x_axis_title="", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=False)

#test error rate and predicted error rate, with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "Standard Deviation")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Error Rate/Predicted Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                                error_bar="Standard Deviation")

# #test error rate and predicted error rate, without error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Error Rate/Predicted Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True)

#test assistance score with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Assistance Score")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Assistance Score', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                               error_bar="Standard Error")

# #test number of incorrects with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Number of Incorrects")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Number of Incorrects', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                               error_bar="Standard Error")

# #test correct step duration with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Correct Step Duration")
# print(df_aggr)
# graphs = create_model_lc_chart(df_aggr, model, group_by, 
#                                line_to_display = 'Correct Step Duration', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="All Knowledge Component", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                               error_bar="Standard Error")

# final_graph = None
# for graph in graphs:
#     if final_graph is None:
#         final_graph = graph
#     else:
#         final_graph = alt.layer(final_graph, graph)
# final_graph


# In[41]:


#create graph for a student or skill
#opp_aggr_df has these columns: KC (Original), Opportunity (Original), First Attempt Num, Predicted Error Rate (Original), Error Rate (or other values depending on line_to_display), Count
def create_element_lc_chart(opp_aggr_df, model, 
                            group_by_type, #Knowledge Components or Students
                            element, 
                            line_to_display, #specfy which column to use for y-axis: "Error Rate", "Predicted Error Rate", "Error Rate/Predicted Error Rate", "Assistance Score", "Number of Incorrects", "Number of Hints", "Step Duration", "Correct Step Duration", "Error Step Duration" 
                            legend_title, #legend title
                            width, height, 
                            add_legend=True, #false to add no legend
                            graph_title=None, #graph title
                            x_axis_title=None, #x-axis title
                            y_axis_title=None, #y-axis title
                            point=True,
                           error_bar="No Error Bars"): #"No Error Bars", "Standard Deviation", "Standard Error"
    student_name = 'Anon Student Id'
    kc_name = getKCModelColumnName(model)
    kc_opportunity = getOpportunityColumnName(model)
    kc_predicted_error_rate = getPredictedErrorColumnName(model)
    return_graphs = []
    
    opp_aggr_df_copied = None
    if group_by_type == 'Knowledge Components':
        opp_aggr_df_copied = opp_aggr_df[opp_aggr_df[kc_name] == element].copy()
    else:
        opp_aggr_df_copied = opp_aggr_df[opp_aggr_df[student_name] == element].copy()
        
    if opp_aggr_df_copied is None or opp_aggr_df_copied.empty:
        return None
    
    add_error_bar = False
    if error_bar != "No Error Bars":
        add_error_bar = True
    
    if line_to_display == "Error Rate/Predicted Error Rate":
        error_rate_chart = draw_error_rate_line (opp_aggr_df_copied, model, "Error Rate", 
                                             graph_title, x_axis_title, y_axis_title, legend_title, 
                                             width, height, 
                                             add_legend=add_legend, point=point, error_bar=add_error_bar)
        if error_rate_chart is not None:
            return_graphs.append(error_rate_chart)
        pred_df_aggr_by_opportunity = get_df_opp_aggr(opp_aggr_df_copied, model, column_to_average = "Predicted Error Rate", error_bar = error_bar)
        predicted_error_rate_chart = draw_error_rate_line (pred_df_aggr_by_opportunity, model, 'Predicted Error Rate', 
                                                           graph_title, x_axis_title, y_axis_title, legend_title, 
                                                           width, height, 
                                                           add_legend=add_legend, point=point, error_bar=add_error_bar)
        if predicted_error_rate_chart is not None:
            return_graphs.append(predicted_error_rate_chart)
    else:
        error_rate_chart = draw_error_rate_line (opp_aggr_df_copied, model, line_to_display, 
                                             graph_title, x_axis_title, y_axis_title, legend_title, 
                                             width, height, 
                                             add_legend=add_legend, point=point, error_bar=add_error_bar)
        if error_rate_chart is not None:
            return_graphs.append(error_rate_chart)
    return return_graphs

# df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# group_by = "Knowledge Components"
# #group_by = "Students"
# model = 'Original'
# df = clean_df(df, model)

# #test error rate, no error bar for 'ALT:PARALLELOGRAM-AREA' or 'Stu_02ee1b3f31a6f6a7f4b8012298b2395e'
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
# print(df_aggr)
# element = 'ALT:PARALLELOGRAM-AREA'
# #element = 'Stu_02ee1b3f31a6f6a7f4b8012298b2395e'
# graphs = create_element_lc_chart(df_aggr, model, group_by, element=element,
#                                line_to_display = 'Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title=element, #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="Error Rate", #y-axis title
#                                point=True,
#                                error_bar="No Error Bars")

# #test error rate, no error bar for 'ALT:PARALLELOGRAM-AREA' or 'Stu_02ee1b3f31a6f6a7f4b8012298b2395e'
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "Standard Error")
# print(df_aggr)
# element = 'ALT:PARALLELOGRAM-AREA'
# graphs = create_element_lc_chart(df_aggr, model, group_by, element=element,
#                                line_to_display = 'Error Rate/Predicted Error Rate', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title=element, #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                                error_bar="Standard Error")

#test assistance score with error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Step Duration", "Standard Error")
# print(df_aggr)
# element = 'ALT:PARALLELOGRAM-AREA'
# graphs = create_element_lc_chart(df_aggr, model, group_by,element=element, 
#                                line_to_display = 'Step Duration', 
#                                legend_title = "Legend", 
#                                width = 600, height = 400, 
#                                add_legend=True,
#                                graph_title="Step Duration", #graph title
#                                x_axis_title="Opportunity", #x-axis title
#                                y_axis_title="", #y-axis title
#                                point=True,
#                               error_bar="Standard Error")

# final_graph = None
# for graph in graphs:
#     if final_graph is None:
#         final_graph = graph
#     else:
#         final_graph = alt.layer(final_graph, graph)
# final_graph


# In[42]:


#df_aggr should have these columns: Count, KC (model), Opportunity (model)
#skill_param_dict: {skill_name: gamma column of the skill table}
def categorize_lc(df_aggr, model, skill_slope_dict, 
                  student_threshold = 10, 
                  opportunity_threshold = 3, 
                  low_error_threshold = 0.2, 
                  high_error_threshold = 0.4, 
                  slope_threshold = 0.001) :
    kc_col = getKCModelColumnName(model)
    opportunity_col = getOpportunityColumnName(model)
    predicted_error_rate_col = getPredictedErrorColumnName(model)
    left_over_kcs = df_aggr[kc_col].unique().tolist()
    #rid of points that have count < student_threshold: 
    df_aggr = df_aggr[(df_aggr['Count'] >= student_threshold)]
    #too little data
    group_kc_counts = df_aggr.groupby(kc_col).size()
    too_little_data = group_kc_counts[group_kc_counts < opportunity_threshold].index.tolist()
    left_over_kcs = list(set(left_over_kcs) - set(too_little_data))
    #low and flat
    df_aggr = df_aggr[df_aggr[kc_col].isin(left_over_kcs)]
    df_aggr_opp_threshold = df_aggr[(df_aggr[opportunity_col] >= opportunity_threshold)]
    group_kc_max = df_aggr_opp_threshold.groupby(kc_col)['Error Rate'].max()
    low_flat_data = group_kc_max[group_kc_max < low_error_threshold].index.tolist()
    low_flat_data = list(set(low_flat_data) - set(too_little_data))
    left_over_kcs = list(set(left_over_kcs) - set(low_flat_data))
    #still high
    df_left_over_kc = df_aggr[df_aggr[kc_col].isin(left_over_kcs)]
    #for each kc, get the predicted_error at the highest opp
    still_high_data = df_left_over_kc.loc[df_left_over_kc.groupby(kc_col)[opportunity_col].idxmax(), [kc_col, 'Error Rate']]
    still_high_data = still_high_data[(still_high_data['Error Rate'] >= high_error_threshold)]
    still_high_data = still_high_data[kc_col].tolist()
    left_over_kcs = list(set(left_over_kcs) - set(still_high_data))
    #no learning
    no_learning_data = []
    for kc in left_over_kcs:
        if kc in skill_slope_dict and skill_slope_dict[kc] <= slope_threshold:
            no_learning_data.append(kc)
    good_data = list(set(left_over_kcs) - set(no_learning_data))
    return {"too_little_data":too_little_data, 
           "low_flat":low_flat_data,
           "still_high":still_high_data,
           "no_learning":no_learning_data,
           "good":good_data}
    
    
# primary_df = pd.read_csv("ds76_student_step_All_Data_74_2020_0926_034727.txt", sep='\t')
# primary_model = "Original"
# primary_df = clean_df(primary_df, primary_model)
# #get aggregation of kc/student and opp: count, mean of prediction and error
# primary_df_aggr = get_df_kc_opp_aggr(primary_df, primary_model, "kc")
# #primary_df_aggr.to_csv("temp.csv", index=False)
# print(primary_df_aggr.columns)
# skill_slope_dict = {"ALT:CIRCLE-AREA":0.104196940694798,
#                    "ALT:CIRCLE-CIRCUMFERENCE":0.104196940694798,
#                    "ALT:CIRCLE-DIAMETER":0.073534474059823,
#                    "ALT:COMPOSE-BY-ADDITION":0.0}

# print(categorize_lc(primary_df_aggr, primary_model, skill_slope_dict,
#                   student_threshold = 10, 
#                   opportunity_threshold = 3, 
#                   low_error_threshold = 0.2, 
#                   high_error_threshold = 0.4, 
#                   slope_threshold = 0.001))


# In[43]:


#parse the parameter_xml file and output skill_slope_dict
#paramter_xml is generated by afm related workflow component
def get_skill_slope_dict_from_xml(parameter_xml_filename):
    skill_slope_dict = {}
    # Load and parse the XML file
    tree = ET.parse(parameter_xml_filename)  # Replace with your actual file path
    root = tree.getroot()
    for elem in root:
        is_skill_elem = False
        for subelem in elem:
            if subelem.tag == "type" and subelem.text == "Skill":
                is_skill_elem = True
                break
        if is_skill_elem:
            skill_name = None
            skill_slope = None
            for subelem in elem:
                if subelem.tag == "name":
                    skill_name = subelem.text
                if subelem.tag == "slope":
                    skill_slope = subelem.text
            skill_slope = safe_str_to_float(skill_slope)
            if skill_name is not None and skill_slope is not None:
                skill_slope_dict[skill_name] = skill_slope
    return skill_slope_dict
#print(get_skill_slope_dict_from_xml("Parameters.xml"))   


# In[44]:


#parse DS model value export and output skill_slope_dict
#model_values is generated by DS model value export
def get_skill_slope_dict_from_ds_export(model_values_filename):
    skill_slope_dict = {}
    with open(model_values_filename, 'r') as file:
        lines = file.readlines()
    extracting = False
    extracted_lines = []
    for line in lines:
        if line.startswith("KC Values for"):
            extracting = True
            continue
        if line.startswith("Student Values for"):
            extracting = False
            break
        if extracting:
            extracted_lines.append(line.strip())
    for line in extracted_lines:
        if line.startswith("KC Name") and line.strip() != "":
            continue
        parts = line.split("\t")
        if len(parts) == 6:
            slope = safe_str_to_float(parts[5])
            if slope is not None:
                skill_slope_dict[parts[0]] = slope
    return skill_slope_dict
    
#print(get_skill_slope_dict_from_ds_export("ds76_afm_kcm472_2025_0225_183654.txt"))


# In[48]:


#test on command line
#C:\Users\hchen\Anaconda3\envs\36_env\python.exe DataShop_LC_in_Altair.py -programDir . -workingDir . -userId hcheng -afmSlopeThreshold 0.001 -categorizeLearningCurve false -errorBar "No Error Bars" -highErrorThreshold 40.0 -learningCurveGroupBy "Knowledge Components" -learningCurveMetric "Error Rate" -lowErrorThreshold 20.0 -opportunityCutOffMax INF -opportunityCutOffMin 0 -opportunityThreshold 3 -primaryModel_nodeIndex 0 -primaryModel_fileIndex 0 -primaryModel "Predicted Error Rate (Original)" -secondaryModel_nodeIndex 0 -secondaryModel_fileIndex 0 -secondaryModel "Predicted Error Rate (Original)" -showPredictedLearningCurve true -studentThreshold 10 -viewSecondary true -node 0 -fileIndex 0 ds76_student_step_All_Data_74_2020_0926_034727.txt
#C:\Users\hchen\Anaconda3\envs\36_env\python.exe DataShop_LC_in_Altair.py -programDir . -workingDir . -userId hcheng -afmSlopeThreshold 0.001 -categorizeLearningCurve false -errorBar "Standard Error" -highErrorThreshold 40.0 -learningCurveGroupBy "Knowledge Components" -learningCurveMetric "Assistance Score" -lowErrorThreshold 20.0 -opportunityCutOffMax INF -opportunityCutOffMin 0 -opportunityThreshold 3 -primaryModel_nodeIndex 0 -primaryModel_fileIndex, 0, -primaryModel "Predicted Error Rate (Area)" -secondaryModel_nodeIndex 0 -secondaryModel_fileIndex 0 -secondaryModel "Predicted Error Rate (Area)" -showPredictedLearningCurve true -studentThreshold 10 -viewSecondary false -node 0 -fileIndex 0 ds76_student_step_All_Data_74_2020_0926_034727.txt

#to enable handling of big datasets
alt.data_transformers.disable_max_rows()

#command line
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description='Python program to generate learning curves.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument('-user', type=str, help='user')
    
    parser.add_argument('-errorBar', type=str, choices=["No Error Bars", "Standard Deviation", "Standard Error"], default="No Error Bars", help='error bar type')
    parser.add_argument('-learningCurveMetric', type=str, choices=["Error Rate", "Predicted Error Rate", "Assistance Score", "Number of Incorrects", "Number of Hints", "Step Duration", "Correct Step Duration", "Error Step Duration"], 
                        default="Error Rate", help='LC metrics')
    parser.add_argument('-learningCurveGroupBy', type=str, choices=["Knowledge Components", "Students"], default="Knowledge Components", help='learning curve type')
    
    parser.add_argument('-categorizeLearningCurve', type=str, choices=["true", "false"], default="true", help='categorize leanring curve')
    parser.add_argument("-showPredictedLearningCurve", type=str, choices=["true", "false"], default="true", help="show predicted Learning Curve")
    parser.add_argument("-viewSecondary", type=str, choices=["true", "false"], default="true", help="show secondary model")
    
    parser.add_argument("-primaryModel", type=str, required=True, help="Primary model")
    parser.add_argument("-secondaryModel", type=str,  help="Secondary model")
    
    parser.add_argument("-opportunityCutOffMax", type=int_or_inf, default="INF", help="Opprotunity cut off max")
    parser.add_argument("-opportunityCutOffMin", type=int, default=0, help="Opprotunity cut off min")
    
    parser.add_argument("-studentThreshold", type=int, default=10, help="A number for student threshold, used in LC categorization")
    parser.add_argument("-afmSlopeThreshold", type=float, default=0.001, help="A floating-point number for model slope threshold, used in LC categorization")
    parser.add_argument("-highErrorThreshold", type=float, default=40.0, help="A floating-point number for high error threshhold, used in LC categorization")
    parser.add_argument("-lowErrorThreshold", type=float, default=20.0, help="A floating-point number for low error threshold, used in LC categorization")
    parser.add_argument("-opportunityThreshold", type=int, default=3, help="A number for opportunity threshold, used in LC categorization")
    
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    
    args, option_file_index_args = parser.parse_known_args()
    #process files
    primary_file = ""
    parameter_file = ""
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            primary_file = args.fileIndex[x][1]
        if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
            parameter_file = args.fileIndex[x][1]
            
    working_dir = args.workingDir
    program_dir = args.programDir
    user = args.user
    errorBar = args.errorBar
    learningCurveMetric = args.learningCurveMetric
    learningCurveType = args.learningCurveGroupBy
    
    showPredictedLearningCurve = args.showPredictedLearningCurve
    if showPredictedLearningCurve.lower() == 'true':
        showPredictedLearningCurve = True
    else:
        showPredictedLearningCurve = False
    categorizeLearningCurve = args.categorizeLearningCurve
    if categorizeLearningCurve.lower() == 'true':
        categorizeLearningCurve = True
    else:
        categorizeLearningCurve = False
    viewSecondary = args.viewSecondary
    if viewSecondary.lower() == 'true':
        viewSecondary = True
    else:
        viewSecondary = False
        
    primaryModel = args.primaryModel
    secondaryModel = args.secondaryModel
    
    opportunityCutOffMax = args.opportunityCutOffMax
    opportunityCutOffMin = args.opportunityCutOffMin
    
    studentThreshold = args.studentThreshold
    afmSlopeThreshold = args.afmSlopeThreshold
    highErrorThreshold = args.highErrorThreshold
    lowErrorThreshold = args.lowErrorThreshold
    opportunityThreshold = args.opportunityThreshold
    
else:
    primary_file = "ds76_student_step_All_Data_74_2020_0926_034727.txt"
    parameter_file = "parameters.xml"
    #parameter_file = "ds76_afm_kcm472_2025_0225_183654.txt"
    working_dir = "."
    program_dir = "."
    user = "hcheng"
    
    errorBar = "No Error Bars"
    #errorBar = "Standard Error"
    #errorBar = "Standard Deviation"
    
    learningCurveMetric = "Error Rate"
    #learningCurveMetric = "Assistance Score"
    #learningCurveMetric = "Step Duration"
    learningCurveType = "Knowledge Components"
    #learningCurveType = "Students"
    
    categorizeLearningCurve = True
    #categorizeLearningCurve = False
    showPredictedLearningCurve = True
    #showPredictedLearningCurve = Fasle
    #viewSecondary = True
    viewSecondary = False
    
    primaryModel = "Predicted Error Rate (Original)"
    secondaryModel = "Predicted Error Rate (Original)"
    
    opportunityCutOffMax = float("inf")
    #opportunityCutOffMax = 20
    opportunityCutOffMin = 0
    #opportunityCutOffMin = 10
    
    studentThreshold = 10
    afmSlopeThreshold = 0.001
    highErrorThreshold = 40.0
    lowErrorThreshold = 20.0
    opportunityThreshold = 3

if showPredictedLearningCurve and learningCurveMetric == "Error Rate":
    learningCurveMetric = "Error Rate/Predicted Error Rate"
if learningCurveType == "Knowledge Components":
    graph_title="All Knowledge Components"
elif learningCurveType == "Students":
    graph_title="All Students"
if learningCurveMetric not in ["Error Rate", "Predicted Error Rate", "Error Rate/Predicted Error Rate"]:
    categorizeLearningCurve = False
    

y_axis_title = ""
#somehow if not error rate, predited error rate, not need to do the y axis
if learningCurveMetric in ["Error Rate", "Predicted Error Rate", "Error Rate/Predicted Error Rate"]:
    y_axis_title = "Error Rate"
else:
    y_axis_title = f"{learningCurveMetric} Value"
#fresh new log file
logFile_name = os.path.join(working_dir, "Datashop_LC_in_Altair.wfl")
logFile = open(logFile_name, "w")
logFile.close();    

    
#clean primary data
primary_model = strip_model_name(primaryModel)
primary_opportunity = getOpportunityColumnName(primary_model)
df = pd.read_csv(primary_file, sep='\t')
#check if model is multi-skilled and convert first attempt to 0/1 and delete rows that are non-numeric in opp column
primary_df = clean_df(df, primary_model)

#opportunity cutoff
if not math.isinf(opportunityCutOffMax):
    primary_df = primary_df[primary_df[primary_opportunity] <= opportunityCutOffMax]
if opportunityCutOffMin > 0:
    primary_df = primary_df[primary_df[primary_opportunity] >= opportunityCutOffMin]
    
#unique list of students or skills
unique_elements = None
if learningCurveType == 'Knowledge Components':
    unique_elements = primary_df[getKCModelColumnName(primary_model)].unique().tolist()
else:
    unique_elements = primary_df['Anon Student Id'].unique().tolist()
#get aggregation of kc/student and opp: count, mean of prediction and error or others
primary_df_aggr = get_df_kc_opp_aggr(primary_df, primary_model, group_by=learningCurveType, aggregate_meatures=learningCurveMetric, error_bar=errorBar)

# #test error rate, no error bar
# df_aggr = get_df_kc_opp_aggr(df, model, group_by, "Error Rate", "No Error Bars")
#print(df_aggr)

all_graphs = create_model_lc_chart(primary_df_aggr, primary_model, learningCurveType, 
                               line_to_display = learningCurveMetric, 
                               legend_title = "Legend",
                               width = 600, height = 400, 
                               add_legend=True,
                               graph_title=graph_title,
                               x_axis_title="Opportunities",
                               y_axis_title=y_axis_title,
                               error_bar=errorBar
                               )

secondary_model = None
secondary_df_aggr = None
#viewSecondary
if viewSecondary:
    secondary_model = strip_model_name(secondaryModel)
    secondary_opportunity = getOpportunityColumnName(secondary_model)
    #clean secondary data
    #check if model is multi-skilled and convert first attempt to 0/1 and delete opp that is non-numeric
    secondary_df = clean_df(df, secondary_model)
    #opportunity cutoff
    if not math.isinf(opportunityCutOffMax):
        secondary_df = secondary_df[secondary_df[secondary_opportunity] <= opportunityCutOffMax]
    if opportunityCutOffMin > 0:
        secondary_df = secondary_df[secondary_df[secondary_opportunity] >= opportunityCutOffMin]
    secondary_df_aggr = get_df_kc_opp_aggr(secondary_df, secondary_model, group_by=learningCurveType, aggregate_meatures=learningCurveMetric, error_bar=errorBar)
    all_graphs_secondary_model = create_model_lc_chart(secondary_df_aggr, secondary_model, learningCurveType, 
                               line_to_display = learningCurveMetric, 
                               legend_title = "Legend",
                               width = 600, height = 400, 
                               add_legend=True,
                               graph_title=graph_title,
                               x_axis_title="",                        
                               y_axis_title="",
                               error_bar=errorBar
                               )

    if all_graphs is not None and all_graphs_secondary_model is not None:
            all_graphs.extend(all_graphs_secondary_model)
    elif all_graphs is None and all_graphs_secondary_model is not None:
            all_graphs = all_graphs_secondary_model
            
main_graph = None
for graph in all_graphs:
    if graph is not None:
        if main_graph is None:
            main_graph = graph
        else:
            main_graph = alt.layer(main_graph, graph)
if main_graph is not None:
    main_graph.save(os.path.join(working_dir, 'all.html'))


#get unique list of skills or students
# Combine and remove duplicates
unique_elements = list(set(unique_elements))
element_safe_filename = {}
for element in unique_elements:
    all_graphs_for_this_element = None
    #primary model
    graphs_for_this_element = create_element_lc_chart(primary_df_aggr, primary_model, learningCurveType, 
                                                        element = element,
                                                        line_to_display = learningCurveMetric, 
                                                       legend_title = f'Legend for {element}',
                                                       width = 600, height = 400, 
                                                       add_legend=True,
                                                       graph_title=element,
                                                      x_axis_title="Opportunities",
                                                       y_axis_title=y_axis_title,
                                                       error_bar=errorBar)
    
    #viewSecondary
    if viewSecondary:
        graphs_for_this_element_secondary_model = create_element_lc_chart(secondary_df_aggr, secondary_model, learningCurveType, 
                                                                           element = element,
                                                                            line_to_display = learningCurveMetric, 
                                                                           legend_title = f'Legend for {element}',
                                                                           width = 600, height = 400, 
                                                                           add_legend=True,
                                                                           graph_title=element,
                                                                              x_axis_title="",
                                                                           y_axis_title="",
                                                                           error_bar=errorBar)
        
        if graphs_for_this_element is not None and graphs_for_this_element_secondary_model is not None:
            graphs_for_this_element.extend(graphs_for_this_element_secondary_model)
        elif graphs_for_this_element is None and graphs_for_this_element_secondary_model is not None:
            graphs_for_this_element = graphs_for_this_element_secondary_model
        elif graphs_for_this_element is None and graphs_for_this_element_secondary_model is None:
            continue
    #save this
    graph_for_this_element = None
    if graphs_for_this_element is not None:
        for graph in graphs_for_this_element:
            if graph is not None:
                if graph_for_this_element is None:
                    graph_for_this_element = graph
                else:
                    graph_for_this_element = alt.layer(graph_for_this_element, graph)
    safe_filename = sanitize_filename(str(element))
    element_safe_filename[str(element)] = safe_filename
    if graph_for_this_element is not None:
        graph_for_this_element.save(os.path.join(working_dir, f"{safe_filename}.html")) 
    #add to the dict
    if graph_for_this_element is not None:
        element_safe_filename[str(element)] = safe_filename
        
#order
element_safe_filename = dict(sorted(element_safe_filename.items()))
#print(element_safe_filename)

#do categorization
skill_categories = None
if learningCurveType == 'Knowledge Components' and categorizeLearningCurve:
    _, ext = os.path.splitext(parameter_file)
    if "xml" in ext:
        skill_slope_dict = get_skill_slope_dict_from_xml(parameter_file)
    else:
        skill_slope_dict = get_skill_slope_dict_from_ds_export(parameter_file)
    #print(skill_slope_dict)
    skill_categories = categorize_lc(primary_df_aggr, primary_model, skill_slope_dict,
                      student_threshold = studentThreshold, 
                      opportunity_threshold = opportunityThreshold, 
                      low_error_threshold = lowErrorThreshold/100, 
                      high_error_threshold = highErrorThreshold/100, 
                      slope_threshold = afmSlopeThreshold)
    #print(skill_categories)

combined_html_head = '''
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <script src="https://cdn.jsdelivr.net/npm/vega@5"></script>
        <script src="https://cdn.jsdelivr.net/npm/vega-lite@5"></script>
        <script src="https://cdn.jsdelivr.net/npm/vega-embed@6"></script>
    </head>
    <body>'''
combined_html_tail = '''</body>
    </html>'''

#write the all_final.html
all_html = write_main_chart_html(os.path.join(working_dir, "all.html"))
if learningCurveType == 'Knowledge Components' and categorizeLearningCurve and skill_categories is not None:
    elements_html = category_html(element_safe_filename, skill_categories)
else:
    elements_html = no_category_html(element_safe_filename)

all_combined_html = f'''{combined_html_head}\n
                        {all_html}\n
                        {elements_html}\n
                        {combined_html_tail}'''
with open(os.path.join(working_dir, "all_final.html"), 'w', encoding='utf-8') as f:
    f.write(all_combined_html)
    


# In[ ]:




