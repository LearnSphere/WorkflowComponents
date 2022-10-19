#!/usr/bin/env python
# coding: utf-8

# In[2]:


import warnings
with warnings.catch_warnings():
    warnings.simplefilter("ignore")
    from fuzzywuzzy import fuzz

import numpy as np
import pandas as pd
import argparse
import sys
import csv


# In[31]:


"""
levenshtein_ratio_and_distance:
For more information, check out https://www.cuelogic.com/blog/the-levenshtein-algorithm#:~:text=The%20Levenshtein%20distance%20is%20a,one%20word%20into%20the%20other.

Introduction: the function takes in two strings, comparing their similarity by calculating their levenshtein distance. It suggests if there is a match based on the difference between Levenshtein Distance Ratio and the threshold that the user specified.

Arguments: 
The two strings that will be compared 
string 1
string 2
Returns: levenshtein_ratio
"""
def levenshteinRatioAndDistance(str1, str2):
    # Initialize matrix of zeros
    rows = len(str1)+1
    cols = len(str2)+1
    distance = np.zeros((rows,cols),dtype = int)

    # Populate matrix of zeros with the indices of each character of both strings
    for i in range(1, rows):
        for k in range(1,cols):
            distance[i][0] = i
            distance[0][k] = k

    # Iterate over the matrix to compute the cost of deletions,insertions and/or substitutions    
    for col in range(1, cols):
        for row in range(1, rows):
            if str1[row-1] == str2[col-1]:
                cost = 0 # If the characters are the same in the two strings in a given position [i,j] then the cost is 0
            else:
                # In order to align the results with those of the Python Levenshtein package, if we choose to calculate the ratio
                # the cost of a substitution is 2. If we calculate just distance, then the cost of a substitution is 1.
                cost = 2
 
            distance[row][col] = min(distance[row-1][col] + 1,      # Cost of deletions
                                 distance[row][col-1] + 1,          # Cost of insertions
                                 distance[row-1][col-1] + cost)     # Cost of substitutions

    # Computation of the Levenshtein Distance Ratio, in percentage (integer), eg. 70
    Ratio = int((((len(str1)+len(str2)) - distance[row][col]) / (len(str1)+len(str2))) * 100)
    return Ratio;
#print(levenshteinRatioAndDistance("Adrian S.", "Aidan Smith"))


# In[3]:


"""
Call function levenshteinRatioAndDistance()

Arguments: 
The two strings that will be compared 
string 1
string 2

threshold: 
    # should be passes in when this function is being called independently, 
    # if the functions in this file are called somewhere else, 
    # the user can just specify the threshold as the function argument for convenience. 
    # e.g. compareNamesByLeven(string1, string2, 70), where 70 is tht threshold
    # User should specify the threshold value to determine if the two strings match or not: if the two strings are not a exact match (scored as 100 within the function), 
    # then two strings will be considered as a match ONLY if the score is GREATER than the threshold. Otherwise, they will not be considered as a match.
    # The current default value is 62. The testing on matching students' first and last names suggests that (1) if the first name or last name is a exact match but the other is not, the
    # score will be approximately 80 - 90 or above. But if both the first and last name are not a exact match (but has reasonable amount of similarity), the score is above 60, and around 65.
    # If there are some levels of similarity but the first name or the last name is very different, the score is <= 60.
    # Users can increase the threshold value if they want a more exact match. They will get the unmatched strings printed out in the terminal for manual checking.

Returns: True for matched

"""

def isMatch(str1, str2, threshold=62):
    Ratio = levenshteinRatioAndDistance(str1, str2);
    if Ratio >= 95:
        return True
    elif Ratio >= threshold:
        logFile = open("fuzzy.log", 'w')
        logFile.write(" '{}' and '{}' are not the same, but maybe a match, with matching score {}\n".format(str1, str2, Ratio))
        return True
    else:
        # insertions and/or substitutions
        # This is the minimum number of edits needed to convert string a to string b
        #print("String {} has no match".format(str1))
        return False
#print(isMatch("Barbara", "Barb", 70))


# In[29]:


'''
mergeFileByMatching
Introduction: 
The function takes in two map Files and return a new dataframe by finding match of strings between the two map files
and merge all the columns in both map files if it's a matched column.


Arguments:
        mapfile1: File 1
        mapfile2: File 2
        threshold: 
            threshold that identify two strings as a match when their matching score produced by levenshtein algorithms is greater than this ratio.
            default to 63. See isMatch() function header of why this value.
        columnNamesFileOne: name of the columns in file 1 to be matched with the columns in file 2
        columnNamesFileTwo: name of the columns in file 2 to be matched with the columns in file 1
        Note that both "columnNamesFileOne" and "columnNamesFileTwo" should be passed in as a list of strings.
'''
def mergeFileByMatching(mapfile1, mapfile2, threshold, columnNamesFileOne, columnNamesFileTwo):
    #Read the dataframe
    df1 = pd.read_csv(mapfile1)
    if df1.shape[1] == 1:
        df1 = pd.read_csv(mapfile1, sep='\t')
    df2 = pd.read_csv(mapfile2)
    if df2.shape[1] == 1:
        df2 = pd.read_csv(mapfile12, sep='\t')
    #Get the column names of all columns in both map file
    col1 = list(df1.columns)
    col2 = list(df2.columns)
    
    #make a new data frame to output
    output_df = []
    matched1, matched2, col_final = set(), set(), list()

    # concatenate strings to find match
    df1_string = df1[columnNamesFileOne].apply(lambda row: ' '.join(row.values.astype(str)), axis=1)
    df2_string = df2[columnNamesFileTwo].apply(lambda row: ' '.join(row.values.astype(str)), axis=1)
    
    for index1, str1 in enumerate(df1_string):
        for index2, str2 in enumerate(df2_string):
            if isMatch(str1, str2, threshold):
                #add to matched1, matched2
                matched1.add(index1)
                matched2.add(index2)
                column = dict()
                for col in col1:
                    #if col is also in col2, add suffix to col name
                    if col not in col2:
                        column[col] = df1[col][index1]
                        if col not in col_final:
                            col_final.append(col)
                    else:
                        column[col + "_file1"] = df1[col][index1]
                        if col + "_file1" not in col_final:
                            col_final.append(col + "_file1")
                for col in col2:
                    #if col is also in col1, add suffix to col name
                    if col not in col1:
                        column[col] = df2[col][index2]
                        if col not in col_final:
                            col_final.append(col)
                    else:
                        column[col + "_file2"] = df2[col][index2]
                        if col + "_file2" not in col_final:
                            col_final.append(col + "_file2")
                output_df.append(column)
    
    unmatched1, unmatched2 = list(), list()
    for i in range(df1.shape[0]):
        if i not in matched1:
            unmatched1.append(i)
    for i in range(df2.shape[0]):
        if i not in matched2:
            unmatched2.append(i)
            
    for i in unmatched1:
        column = dict()
        for col in col1:
            #if col is also in col2, add suffix to col name
            if col not in col2:
                column[col] = df1[col][i]
                if col not in col_final:
                    col_final.append(col)
            else:
                column[col + "_file1"] = df1[col][i]
                if col + "_file1" not in col_final:
                    col_final.append(col + "_file1")
        output_df.append(column)
        
    for i in unmatched2:
        column = dict()
        for col in col2:
            #if col is also in col1, add suffix to col name
            if col not in col1:
                column[col] = df2[col][i]
                if col not in col_final:
                    col_final.append(col)
            else:
                column[col + "_file2"] = df2[col][i]
                if col + "_file2" not in col_final:
                    col_final.append(col + "_file2")
        output_df.append(column)
           
    output_df = pd.DataFrame(output_df, columns=col_final)
    return output_df
#result = mergeFileByMatching("school.csv", "student.csv", 62, ["firstname", "lastname"], ["firstname", "lastname"])
#result.to_csv('matching_merged_result.csv', index=False)


# In[26]:


'''
stringFileMatching
Introduction: 
The function takes in a string and a column of a map File; and return a new dataframe by marking the rows whose column is matched


Arguments:
        matching_str
        mapfile
        column_to_match
        threshold: 
            threshold that identify two strings as a match when their matching score produced by levenshtein algorithms is greater than this ratio.
            default to 63. See isMatch() function header of why this value.
'''
def stringFileMatching(matching_str, mapfile, column_to_match, threshold):
    #Read the dataframe
    df = pd.read_csv(mapfile)
    if df.shape[1] == 1:
        df = pd.read_csv(mapfile, sep='\t')
    
    #make a new data frame to output
    output_df = []
    matched = set()
    cols = list(df.columns)
    col_final = list(df.columns)
    col_final.append("matched")
    
    df_string = df[column_to_match]
    for index, str in enumerate(df_string):
        if isMatch(matching_str, str, threshold):
            #add to matched
            matched.add(index)
            column = dict()
            for col in cols:
                column[col] = df[col][index]
            column['matched'] = matching_str
            output_df.append(column)
    
    unmatched = list()
    for i in range(df.shape[0]):
        if i not in matched:
            unmatched.append(i)     
    for i in unmatched:
        column = dict()
        for col in cols:
            column[col] = df[col][i]
        column['matched'] = ""
        output_df.append(column)
    output_df = pd.DataFrame(output_df, columns=col_final)
    return output_df

#result = stringFileMatching("adrian", "school.csv", "firstname", 62)
#result.to_csv('matching_merged_result.csv', index=False)


# In[ ]:


#test on command line
#"C:/ProgramData/Anaconda3/Python" fuzzy_matching.py -programDir . -workingDir . -threshold 70 -userId 1 -column_to_match_nodeIndex 0 -column_to_match_fileIndex 0 -column_to_match firstname -file1_column1_to_match_nodeIndex 0 -file1_column1_to_match_fileIndex 0 -file1_column1_to_match firstname -file1_column2_to_match_nodeIndex 0 -file1_column2_to_match_fileIndex 0 -file1_column2_to_match lastname -file1_column3_to_match_nodeIndex 0 -file1_column3_to_match_fileIndex 0 -file1_column3_to_match firstname -file1_column4_to_match_nodeIndex 0 -file1_column4_to_match_fileIndex 0 -file1_column4_to_match firstname -file1_columns firstname_lastname -file2_column1_to_match_nodeIndex 1 -file2_column1_to_match_fileIndex 0 -file2_column1_to_match firstname -file2_column2_to_match_nodeIndex 1 -file2_column2_to_match_fileIndex 0 -file2_column2_to_match lastname -file2_column3_to_match_nodeIndex 1 -file2_column3_to_match_fileIndex 0 -file2_column3_to_match firstname -file2_column4_to_match_nodeIndex 1 -file2_column4_to_match_fileIndex 0 -file2_column4_to_match firstname -file2_columns firstname_lastname -matching_mode "File to file" -num_column_to_match 2 -node 0 -fileIndex 0 school.csv -node 1 -fileIndex 0 student.csv
#"C:/ProgramData/Anaconda3/Python" fuzzy_matching.py -programDir . -workingDir . -threshold 70 -userId, 1 -column_to_match_nodeIndex 0 -column_to_match_fileIndex 0 -column_to_match firstname -file1_column1_to_match_nodeIndex 0 -file1_column1_to_match_fileIndex 0 -file1_column1_to_match firstname -file1_column2_to_match_nodeIndex 0 -file1_column2_to_match_fileIndex 0 -file1_column2_to_match firstname -file1_column3_to_match_nodeIndex 0 -file1_column3_to_match_fileIndex 0 -file1_column3_to_match firstname -file1_column4_to_match_nodeIndex 0 -file1_column4_to_match_fileIndex 0 -file1_column4_to_match firstname -matching_mode "File to string" -num_column_to_match 1 -string_to_match adrian -node 0 -fileIndex 0 school.csv -node 1 -fileIndex 0 student.csv

#command line
command_line = True
matching_mode = "File to file" # or "File to string"
if command_line:
    parser = argparse.ArgumentParser(description='Python program to fuzzy-match.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-matching_mode", type=str, choices=["File to file", "File to string"], default="file to file")
    parser.add_argument("-threshold", type=int, default=70)
    parser.add_argument("-string_to_match", type=str)
    parser.add_argument("-column_to_match", type=str)
    parser.add_argument("-num_column_to_match", type=int, default=1)
    parser.add_argument("-file1_columns", type=str)
    parser.add_argument("-file2_columns", type=str)
    args, option_file_index_args = parser.parse_known_args()
    #var for both modes
    working_dir = args.workingDir
    file1_name = args.fileIndex[0][1]
    matching_mode = args.matching_mode
    threshold = args.threshold
    #for file to string
    string_to_match = ""
    column_to_match = ""
    #for file to file
    file2_name = ""
    num_column_to_match = -1
    file1_columns = ""
    file2_columns = ""
    if matching_mode == "File to string":
        string_to_match = args.string_to_match
        column_to_match = args.column_to_match
    if matching_mode == "File to file":
        file2_name = args.fileIndex[1][1]
        num_column_to_match = args.num_column_to_match
        file1_columns = args.file1_columns
        file2_columns = args.file2_columns

if matching_mode == "File to string":
    result = stringFileMatching(string_to_match, file1_name, column_to_match, threshold)
    result.to_csv('fuzzy_matching_result.txt', sep="\t", index=False) 
elif matching_mode == "File to file":
    file1_columns = file1_columns.split("_")
    file2_columns = file2_columns.split("_")
    result = mergeFileByMatching(file1_name, file2_name, threshold, file1_columns, file2_columns)
    result.to_csv('fuzzy_matching_result.txt', sep="\t", index=False)
    

