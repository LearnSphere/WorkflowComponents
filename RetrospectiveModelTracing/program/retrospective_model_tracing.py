#!/usr/bin/env python
# coding: utf-8

# In[14]:


import pandas as pd
import os
import argparse


# In[ ]:


def transform_retrospective_model_tracing(input_file, output_file):
    # Read the txt file into a pandas DataFrame, assuming tab-separated values
    df = pd.read_csv(input_file, sep='\t')

    # Ensure the Outcome column is explicitly set to a string-compatible type
    if 'Outcome' not in df.columns:
        df['Outcome'] = ''  # Add the Outcome column if it does not exist
    else:
        df['Outcome'] = df['Outcome'].astype(str)  # Explicitly cast to string

    # Sort the DataFrame by relevant columns to ensure proper sequence
    df = df.sort_values(by=['Anon Student Id', 'Problem Name', 'Attempt At Step', 'Row']).reset_index(drop=True)

    # Create a set to track the state for each (Student, Problem, Attempt) combination
    state_tracker = {}

    # Iterate over the rows of the DataFrame
    for index, row in df.iterrows():
        # Extract key identifiers for each row
        student_id = row['Anon Student Id']
        problem_name = row['Problem Name']
        attempt_step = row['Attempt At Step']

        # Create a unique key for each (Student, Problem, Attempt) combination
        key = (student_id, problem_name, attempt_step)

        # If the key is already marked as "N/A", continue setting "N/A" for the current row
        if key in state_tracker and state_tracker[key] == 'N/A':
            df.at[index, 'Outcome'] = 'N/A'
            continue

        # Check if Input matches Selection
        if row['Input'] == row['Selection']:
            # If they match, set Outcome to "correct"
            df.at[index, 'Outcome'] = 'correct'
            state_tracker[key] = 'correct'
        else:
            # If they do not match, set Outcome to "incorrect" and mark key as "N/A" for subsequent rows
            df.at[index, 'Outcome'] = 'incorrect'
            state_tracker[key] = 'N/A'

    # Write the updated DataFrame to a new txt file, preserving the original format
    df.to_csv(output_file, sep='\t', index=False)


# In[16]:


def transform_typical_simple_to_model_tracing(input_file, output_file):
    # Read the txt file into a pandas DataFrame, assuming tab-separated values
    df = pd.read_csv(input_file, sep='\t')

    # Sort the DataFrame by relevant columns to ensure proper sequence
    df = df.sort_values(by=['Anon Student Id', 'Problem Name', 'Attempt At Step', 'Row']).reset_index(drop=True)

    # Create a set to track incorrect state for each (Student, Problem, Attempt) combination
    incorrect_tracker = set()

    # Iterate over the rows of the DataFrame
    for index, row in df.iterrows():
        # Extract key identifiers for each row
        student_id = row['Anon Student Id']
        problem_name = row['Problem Name']
        attempt_step = row['Attempt At Step']

        # Create a unique key for each (Student, Problem, Attempt) combination
        key = (student_id, problem_name, attempt_step)

        # Check if this key is already marked as incorrect
        if key in incorrect_tracker:
            # If already incorrect, set Outcome to "incorrect"
            df.at[index, 'Outcome'] = 'incorrect'
        else:
            # Check if Input matches Selection
            if row['Input'] == row['Selection']:
                # If they match, set Outcome to "correct"
                df.at[index, 'Outcome'] = 'correct'
            else:
                # If they do not match, set Outcome to "incorrect" and mark key as incorrect
                df.at[index, 'Outcome'] = 'incorrect'
                incorrect_tracker.add(key)

    # Write the updated DataFrame to a new txt file, preserving the original format
    df.to_csv(output_file, sep='\t', index=False)


# In[21]:


#test command
#"C:\Users\hchen\Anaconda3\envs\36_env\python.exe" retrospective_model_tracing.py -programDir . -workingDir . -userId 1 -tracingMethod "Retrospective Model Tracing" -node 0 -fileIndex 0 "Typical Simple Approach.txt"
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description="Retrospective Model Tracing")
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-node", action='append')
    parser.add_argument("-tracingMethod", help="method to use", type=str, required=True, choices=['Typical Simple Approach', 'Retrospective Model Tracing'])
    
    args, option_file_index_args = parser.parse_known_args()
    
    working_dir = args.workingDir
    program_dir = args.programDir
    input_file = None
    #config_file = None
    
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            input_file = args.fileIndex[x][1]
    tracingMethod = args.tracingMethod
else:
    working_dir = "."
    program_dir = "."
    input_file = "Typical Simple Approach.txt"
    tracingMethod = "Typical Simple Approach"
    #tracingMethod = "Retrospective Model Tracing"


# In[22]:


if tracingMethod == "Typical Simple Approach":
    output_file = os.path.join(working_dir, "typical_simple_approach_result.txt")
    transform_typical_simple_to_model_tracing(input_file, output_file)
else:
    output_file = os.path.join(working_dir, "retrospective_model_tracing_result.txt")
    transform_retrospective_model_tracing(input_file, output_file)


# In[ ]:




