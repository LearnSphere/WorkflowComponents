#!/usr/bin/env python
# coding: utf-8

# In[2]:


import pandas as pd
import argparse
import os
import re
import csv


# In[3]:


def vtt_to_csv(vtt_file, csv_file):
    file_name, file_extension = os.path.splitext(os.path.basename(csv_file))
    csv_temp_file = file_name + "_temp" + file_extension
    with open(vtt_file, "r", encoding="utf-8") as vtt, open(csv_temp_file, "w", newline='', encoding="utf-8") as csv_out:
        writer = csv.writer(csv_out)
        writer.writerow(["Start Time", "End Time", "Text"])

        lines = vtt.readlines()
        time_pattern = re.compile(r"(\d{2}:\d{2}:\d{2}\.\d{3})\s-->\s(\d{2}:\d{2}:\d{2}\.\d{3})")

        text_buffer, start_time, end_time = "", None, None
        for line in lines:
            line = line.strip()
            match = time_pattern.match(line)
            if match:
                if start_time and end_time and text_buffer:
                    writer.writerow([start_time, end_time, text_buffer.strip()])
                start_time, end_time = match.groups()
                text_buffer = ""
            elif line and not line.startswith("WEBVTT"):
                text_buffer += " " + line

        if start_time and end_time and text_buffer:
            writer.writerow([start_time, end_time, text_buffer.strip()])
    df = pd.read_csv(csv_temp_file)
    df.to_csv(csv_file, index=False)
    #delete the temp file
    os.remove(csv_temp_file)


# In[4]:


#test command from WF component:
#C:/Users/hchen/Anaconda3/python.exe convertTabCsv.py -programDir . -workingDir . -userId 1 -inputFormat CSV -inputFile "MainTable.csv"

#command line
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description='Python program to convert between CSV and tab-delimited file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-inputFile", type=str)
    parser.add_argument("-inputFormat", type=str, choices=["CSV", "Tab-delimited", "VTT"], default="CSV")
    
    args, option_file_index_args = parser.parse_known_args()
    #no train_split_type
    working_dir = args.workingDir
    program_dir = args.programDir
    file_name = args.inputFile
    input_format = args.inputFormat
else:
    #test file
    #file_name = "MainTable_test_for_csv.csv"
    #file_name = "MainTable_test_for_tab.txt"
    file_name = "A2 Transcript of user 216886 tutor session on 2023-09-25 LS id 4760786.vtt"
    #input format
    #input_format="CSV"
    #input_format="Tab-delimited"
    input_format="VTT"
    #working_dir
    working_dir = "."

df = None
if input_format == "CSV":
    df = pd.read_csv(file_name)
    #output to tab-delimited
    df.to_csv(os.path.join(working_dir, 'convertedDelimited.txt'), sep ='\t', index=False)
elif input_format == "Tab-delimited":
    df = pd.read_csv(file_name, sep="\t")
    #output to csv
    df.to_csv(os.path.join(working_dir, 'convertedDelimited.csv'), index=False)
elif input_format == "VTT":
    vtt_to_csv(file_name, os.path.join(working_dir, 'convertedDelimited.csv'))

