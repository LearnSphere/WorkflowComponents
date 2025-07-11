#!/usr/bin/env python
# coding: utf-8

# In[81]:


import openai
import json
import argparse
import pandas as pd
import os
import shutil
import sys
import re
import datetime as dt
import operator
import zipfile


# In[82]:


def logProgressToWfl(progressMsg):
    logFile = open(log_file_name, "a")
    now = dt.datetime.now()
    progressPrepend = "%Progress::"
    logFile.write(progressPrepend + "@" + str(now) + "@" + progressMsg + "\n");
    logFile.close();


# In[83]:


def logToWfl(msg):
    logFile = open(log_file_name, "a")
    now = dt.datetime.now()
    logFile.write(str(now) + ": " + msg + "\n");
    logFile.close();


# In[84]:


def fix_malformed_json(json_str):
    #first delete anything before [ or {
    json_str = json_str.lstrip()
    #match = re.search(r'\[\s*\{', json_str)
    if not json_str.startswith('{') and not json_str.startswith('['):
        match = re.search(r'[\{\[]', json_str)
        if match is None:
            return json_str
        json_str = json_str[match.start():]
    #if json_str ends with }  ]
    if bool(re.search(r'}\s*\]\s*$', json_str)):
        return json_str
    last_comma_pos = json_str.rfind('},')
    if last_comma_pos != -1:
        # Truncate the string to remove the last malformed object
        json_str = json_str[:last_comma_pos+1] + ']'
    return json_str

# #test
# test_str = ''' JSON
# [
#   {
#     "Line": 51,
#     "Error": "not specified URL",
#     "Tutor Response": "Is the URL configured to go to the IXL website?",
#     "Score": 1,
#     "Rationale": "The tutor effectively responds by asking the student to clarify what she is asking and guiding her to self-correct without directly stating that a mistake has been made."
#   },
#   {
#     "Line": 58,
#     "Error": "not assigning students to breakout rooms",
#     "Tutor Response": "If you click on apps and then breakout rooms, you can assign students to rooms by dragging them in. Have you tried that?",
#     "Score": 1,
#     "Rationale": "The tutor effectively addresses the student's confusion by providing clear instructions and guiding her through the process without directly stating that a mistake has been made."
#   }
# ]
# '''

# test_str = '''
# [
#   {
#     "Line": 51,
#     "Error": "not specified URL",
#     "Tutor Response": "Is the URL configured to go to the IXL website?",
#     "Score": 1,
#     "Rationale": "The tutor effectively responds by asking the student to clarify what she is asking and guiding her to self-correct without directly stating that a mistake has been made."
#   },
#   {
#     "Line": 58,
#     "Error": "not assigning students to breakout rooms",
#     "Tutor Response": "If you click on apps and then breakout rooms, you can assign students to rooms by dragging them in. Have you tried that?",
#     "Score": 1,
#     "Rationale": "The tut
# '''

# test_str = '''
# junk here
# {"Rationale": "The tutor effectively reacted to the student's error by guiding and motivating them to find their own mistake. When the student said "72 equal to four", the tutor asked clarifying questions such as "What do you mean?" and "What is 72 times four?" to help the student think critically about the problem. This approach allows the student to reach the correct answer on their own, promoting their problem-solving skills and building their confidence. Therefore, the tutor receives a score of 1.", "Score": 1}

# '''

# test_str = '''
# Math not found
# '''

# print(fix_malformed_json(test_str))


# In[85]:


def escape_rationale(json_str):
    def escape_match(m):
        content = m.group(1)
        escaped = content.replace('"', r'\"')
        return f'"Rationale": "{escaped}"'

    # Match all Rationale values
    pattern = r'"Rationale"\s*:\s*"((?:[^"\\]|\\.)*?)"'
    return re.sub(pattern, escape_match, json_str, flags=re.DOTALL)


# In[86]:


def extract_score(response_str):
    match = re.search(r'(?i)score[^0-9]*([01])', response_str)
    return int(match.group(1)) if match else None
#test
#test_str = '''The tutor effectively responds to the student's error by guiding and motivating them to find their own mistake, rather than directly stating that they are wrong. This is shown through the tutor's use of prompts such as "Please walk me through that problem again?" and "Can you explain to me what you did here?" Additionally, the tutor encourages the student to think critically and offers alternative methods for solving the problem. This approach allows the student to understand their mistake and learn from it, rather than being told they are incorrect. Therefore, this transcript would score a 1.'''
# test_str = '''The tutor effectively guided the student to find their own mistake without directly mentioning the error by asking open-ended questions, such as 'What is your question?' and 'Does that make sense or are you confused?'. Also, the tutor provided positive reinforcement by saying 'You got it' and 'Good luck on the rest of your classwork'. Therefore, the tutor's response was effective in correcting the student's error without causing discouragement. Score: 1.'''
# print(extract_score(test_str))


# In[97]:


def extract_response(response_str, json_obj=False):
    #response = response_obj['choices'][0]['text'].strip()
    #response = response_obj.choices[0].message.content
    #response is extracted already in query_open_ai when stream is set to true
    if not json_obj:
        return response_str
    #clean the misformed JSON, happened when max_token is too small, last object can be truncated
    response_str = fix_malformed_json(response_str)
    response_str = escape_rationale(response_str)
    #delete the last period
    response_str = response_str[:-1] if response_str.endswith('.') else response_str
    df = pd.DataFrame()
    try:
        # Attempt to load the JSON
        parsed = json.loads(response_str)
        if isinstance(parsed, list):
            for obj in parsed:
                new_row = {}
                for key, value in obj.items():
                    if value is None or value == "":
                        value = 0
                    new_row[key] = value
                df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
        elif isinstance(parsed, dict):
            new_row = {}
            for key, value in parsed.items():
                if value is None or value == "":
                    value = 0
                new_row[key] = value
            df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
        else:
            new_row = {}
            new_row['Response'] = response_str
            try:
                new_row['Score'] = int(response_str)
            except (ValueError, TypeError):
                new_row['Score'] = 0
            df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
        if 'Score' in df.columns:
            # Replace empty strings or NaN values with 0
            df['Score'] = df['Score'].replace('', None)  # Treat empty string as missing
            df['Score'] = df['Score'].fillna(0) 
        elif 'Rationale' in df.columns:
            last_try = extract_score(df['Rationale'])
            if last_try is not None:
                df['Score'] = last_try 
        return df
    except Exception as e:
        last_try = extract_score(response_str)
        new_row = {}
        if last_try is None or last_try == "":
            new_row["Score"] = 0
            new_row['Response'] = response_str
        else:
            #only specific for score and rationale situation
            new_row["Score"] = last_try
            new_row["Rationale"] = response_str
        df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
        if 'Score' in df.columns:
            # Replace empty strings or NaN values with 0
            df['Score'] = df['Score'].replace('', None)  # Treat empty string as missing
            df['Score'] = df['Score'].fillna(0)    
        return df

#test
#print(extract_response("error not found", json_obj=True))
#test_str = '''[\n    {\n        "Line": 46,\n        "Error": "Incorrect statement that 2 can go into 7",\n        "Tutor Response": "You can\'t go into seven evenly. 2, 4, 6, 8.",\n        "Score": 1,\n        "Rationale": "The tutor effectively corrects the student\'s misunderstanding by listing multiples of 2, guiding the student to see the error."\n    },\n    {\n        "Line": 57,\n        "Error": "Incorrect statement that 3 can go into 5",\n        "Tutor Response": "Three can go into, uh, five. Cannot go into",\n        "Score": 1,\n        "Rationale": "The tutor corrects the student by clarifying that 3 cannot go into 5, effectively addressing the mistake."\n    },\n    {\n        "Line": 65,\n        "Error": "Incorrect statement about divisibility of 9 into 53",\n        "Tutor Response": "So does nine go 53? No, no. Goes 54.",\n        "Score": 1,\n        "Rationale": "The tutor effectively points out the correct understanding of divisibility by 9, correcting the student\'s error."\n    },\n    {\n        "Line": 85,\n        "Error": "Incorrect addition of 18 and 5, resulting in 23 instead of 23",\n        "Tutor Response": "Five is 23, I think.",\n        "Score": 0,\n        "Rationale": "The tutor confirms an incorrect calculation without correction, which does not help the student understand the correct operation."\n    },\n    {\n        "Line": 90,\n        "Error": "Incorrect statement that 3 goes into 28 evenly",\n        "Tutor Response": "Now three going to 28. I can play right now. 28 even.",\n        "Score": 0,\n        "Rationale": "The tutor incorrectly confirms that 3 goes into 28 evenly, reinforcing the student\'s error instead of correcting it."\n    }\n]'''
#test_str = '''{"Rationale": "The tutor effectively reacted to the student's error by guiding and motivating them to find their own mistake instead of directly mentioning the error. This can be seen in the tutor's effective responses such as "So I suppose the question is asking you to determine the value of x from this equation?" and "Can you think of the next step we can do?". This approach allows the student to actively participate in the learning process and develop problem-solving skills. Therefore, a score of 1 is given.", "Score":1}'''
#test_str = '''{"Rationale": "The tutor effectively helps the student realize their mistake by prompting them with questions like \"Is there a number after the 5?\" and explaining the rounding process to them. This helps the student understand their error and correct it on their own, instead of just being told they are wrong, which can be discouraging. The tutor also apologizes and explains that they are at school and cannot continue the session, showing consideration and responsibility. There is no mention of the student making a mistake, but rather a focus on guiding them to the correct solution.", "Score": 1}'''
#test_str = '''The tutor effectively reacts to the student's error by guiding them to think through the problem and find their own mistake. They ask the student to explain their thought process and provide helpful hints to solve the problem. This fosters independent thinking and problem-solving skills in the student. In contrast, direct criticism can discourage students and hinder their learning progress. Hence, the tutor's responses are effective and show understanding of best practices in tutoring. Score: 1'''
#test_str = '1'
#print(extract_response(test_str, json_obj=True))


# In[98]:


def vtt_to_df(vtt_file):
    df = pd.DataFrame(columns=["start_time", "end_time", "text"])
    pattern = re.compile(r'(\d{2}:\d{2}:\d{2}\.\d{3}) --> (\d{2}:\d{2}:\d{2}\.\d{3})\n(.+)', re.MULTILINE)
    with open(vtt_file, 'r', encoding='utf-8') as file:
        content = file.read()
        matches = pattern.findall(content)
        for match in matches:
            new_row = {"start_time": match[0], "end_time": match[1], "text": match[2].strip()}
            df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
    return df

# #test
# transcript_filename = "drive-download-20250325T133806Z-001/878010494_captions.vtt"
# #transcript_filename = "A2 Transcript of user 216886 tutor session on 2023-09-25 LS id 4760786.vtt"
# print(vtt_to_df(transcript_filename))


# In[99]:


def convert_df_column_prompt_text(df, col):
    return "\n".join(f"{i+1} {row}" for i, row in enumerate(df[col]))

# #test
# #transcript_filename = "drive-download-20250325T133806Z-001/878010494_captions.vtt"
# transcript_filename = "A2 Transcript of user 216886 tutor session on 2023-09-25 LS id 4760786.vtt"
# df = vtt_to_df(transcript_filename)
# print(convert_df_column_prompt_text(df, "text"))


# In[100]:


#prompt file should has this: Transcript Start --- --- Transcript End
# text before Transcript Start --- is the prompt; text after --- Transcript End is the format prompt
class PromptFormatError(Exception):
    pass

def parse_prompt(filename):
    with open(filename, 'r', encoding='utf-8') as file:
        content = file.read()
        found_start_prompt = "Transcript Start ---" in content
        found_format_prompt = "--- Transcript End" in content
        if not found_start_prompt or not found_format_prompt:
            raise PromptFormatError('Prompt file missing "Transcript Start ---" or "--- Transcript End"')
        start_prompt = content.split("Transcript Start ---")[0] + "Transcript Start ---\n"
        format_prompt = "--- Transcript End" + content.split("--- Transcript End", 1)[1]
        return (start_prompt, format_prompt)
    
# # test
# prompt_filename = "math_error_filter_prompt.txt"
# prompt_start, format_prompt = parse_prompt(prompt_filename)
# print(prompt_start)
# print(format_prompt)


# In[101]:


#extract csv and get all files with extension vtt
def get_files_in_zip(zip_filename, extract_to):
    with zipfile.ZipFile(zip_filename, 'r') as zip_ref:
        zip_ref.extractall(extract_to)
    allfiles = []
    for root, _, files in os.walk(extract_to):
        for file in files:
            if file.lower().endswith(".vtt") or file.lower().endswith(".csv") or file.lower().endswith(".txt"):
                allfiles.append(os.path.join(root, file))
    return allfiles
#test
#print(get_files_in_zip("danielle_vtts.zip", "./unzipped_temp"))


# In[102]:


#clean a the double // or \\ from the file path
def clean_filename(filename):
    normalized_name = os.path.normpath(filename)
    # Remove leading dot and backslash if present
    if normalized_name.startswith((".\\", "./")):
        normalized_path = normalized_path[2:]
    return normalized_name
#print(clean_filename(".//unzipped_files_temp\\blah//blah2\\878010491_captions.vtt"))


# In[103]:


def query_open_ai(prompt, temperature=1, max_tokens=200):
    response_obj = openai.Completion.create(engine = "gpt-3.5-turbo-instruct", 
                                            prompt = prompt, 
                                            temperature = temperature,
                                            max_tokens = max_tokens)
    response = response_obj['choices'][0]['text'].strip()
    #print(f"in query_open_ai: {response}")
    return response

# #test
# openai_api_key = "your_key"
# openai.api_key = openai_api_key
# #prompt_filename = "math_error_filter_prompt.txt"
# #prompt_filename = "Plus_math_error_evaluation_prompt_gpt_4o.txt"
# prompt_filename = "math_error_by_line_prompt_gpt35.txt"
# prompt_start, format_prompt = parse_prompt(prompt_filename)
# transcript_filename = "drive-download-20250325T133806Z-001/878010491_captions.vtt"
# df = vtt_to_df(transcript_filename)
# all = convert_df_column_prompt_text(df, "text")
# prompt = f"""{prompt_start} {all} {format_prompt}"""
# response = None
# try:
#     response = query_open_ai(prompt)
# except Exception as e:
#     print(f"An error occurred: {e}")
# print(response)
# print(extract_response(response, True))


# In[104]:


#filename: vtt or csv
class FileTypeError(Exception):
    pass

def evaluation_file(transcript_filename, prompt_filename, cur_file_cnt, all_file_cnt, col=None, 
                        num_tries=3, temperature=1, max_tokens=200): 
    #prompt file
    prompt_start, format_prompt = parse_prompt(prompt_filename)
    #transcript file
    file_ext = os.path.splitext(transcript_filename)[1]
    all_text = None
    if file_ext.lower() == ".csv":
        df = pd.read_csv(transcript_filename)
        if col not in df.columns:
            raise FileTypeError("CSV doesn't have the specified utterence column")
        all_text = convert_df_column_prompt_text(df, col)
    elif file_ext.lower() == ".vtt":
        df = vtt_to_df(transcript_filename)
        all_text = convert_df_column_prompt_text(df, "text")
    elif file_ext.lower() == ".txt":
        with open(transcript_filename, 'r', encoding='utf-8') as file:
            all_text = file.read()
    else:
        raise FileTypeError('Transcript file can only be CSV, VTT or TXT')
        
    prompt = f"""{prompt_start} {all_text} {format_prompt}"""
    #logToWfl(f"prompt: {prompt}")
    
    # Iterate over the num_tries times
    all_trial_responses = {}
    for trial_index in range(num_tries):
        #print(f"trial: {trial_index}")
        trial_name = f"Trial_{trial_index+1}"
        try:
            #response if parsed in query_open_ai
            response = query_open_ai(prompt, temperature = temperature, max_tokens = max_tokens)
            
            response_parsed = extract_response(response, json_obj=True)
        except Exception as e:
            error_msg = f"An error occurred: {e}"
            logToWfl(error_msg)
            print(error_msg)
            response_parsed = pd.DataFrame([{"Server Error": error_msg,"Score":0}])
        all_trial_responses[trial_name] = response_parsed
        
        prog = ((cur_file_cnt - 1)/all_file_cnt) + (1/all_file_cnt) * ((trial_index+1)/num_tries)
        logProgressToWfl( "{:.0%}".format(prog))
        print(f"Overall progress: {prog:.0%}")
    return all_trial_responses

# #test
# #878010494_captions.vtt or 878010491_captions.vtt or 875691055_captions.vtt
# openai_api_key = "your key"
# openai.api_key = openai_api_key
# log_file_name = "situation_finder_evalution_wf.log"
# #prompt_filename = "math_error_filter_prompt.txt"
# #prompt_filename = "Plus_math_error_evaluation_prompt_gpt_4o.txt"
# prompt_filename = "math_error_by_line_prompt_gpt35.txt"
# #response = evaluation_file("drive-download-20250325T133806Z-001/878010491_captions.vtt", prompt_filename, 1, 1, num_tries=3) 
# #response = evaluation_file("convertedDelimited.csv", prompt_filename, 1, 1, col = "Text", num_tries=3) 
# response = evaluation_file("018b3e14-8717-7772-f9c9-f259993de6b3.txt", prompt_filename, 1, 1, num_tries=3)
# print("Trial_1")
# print(response["Trial_1"])
# print("Trial_2")
# print(response["Trial_2"])
# print("Trial_3")
# print(response["Trial_3"])


# In[114]:


#test
#C:\Users\hchen\Anaconda3\envs\36_env\python.exe tutor_transcript_evaluation_py36.py -programDir . -workingDir . -userId 1 -max_token 200 -number_of_trials 3 -openai_api_key  -prompt_file C:\WPIDevelopment\dev06_dev\WorkflowComponents\TutorTranscriptEvaluation\test\Tutoringanalytics-1-x345861\output\prompt.txt -temperature 1.0 -transcript_file_type VTT -write_prompt true -node 0 -fileIndex 0, C:\WPIDevelopment\dev06_dev\WorkflowComponents\TutorTranscriptEvaluation\test\test_data\878011973_captions.vtt -node 1 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\TutorTranscriptEvaluation\test\test_data\math_error_evaluation_prompt.txt
#test situation filter
command_line=True
if command_line:
    parser = argparse.ArgumentParser(description="Tutor Evaluation")
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-transcript_file_type", help="transcript file type", type=str, required=True, choices=['Zip of VTT Files', 'Zip of CSV Files', 'Zip of TXT Files', 'VTT', 'CSV', 'TXT'])
    parser.add_argument("-openai_api_key", help="OpenAI API Key", type=str, required=True)
    parser.add_argument("-max_token", help="maximum token returned from gpt", type=int, default=200)
    parser.add_argument("-number_of_trials", help="number of trials to query gpt", type=int, default=3)
    parser.add_argument("-temperature", help="temperature for gpt engine", type=float, default=1.0)
    parser.add_argument("-utterances_col", help="the transcript uterence column when the input file is CSV", type=str)   
    parser.add_argument("-prompt_file",  type=str, required=True)
    
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-node", action='append')
    #args = parser.parse_args()
    args, option_file_index_args = parser.parse_known_args()
    working_dir = args.workingDir
    program_dir = args.programDir
    if working_dir is None:
        working_dir = ".//"
    if program_dir is None:
        program_dir = ".//"
    transcript_file_type = args.transcript_file_type
    openai_api_key = args.openai_api_key
    max_token = args.max_token
    num_trails = args.number_of_trials
    temperature = args.temperature
    utterances_col = args.utterances_col
    prompt_file = args.prompt_file
    
    #process files for WF:
    if args.node is not None:
        for x in range(len(args.node)):
            if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
                transcript_file = args.fileIndex[x][1]
     
#for test                  
else:
    working_dir = ".//"
    program_dir = ".//"
    transcript_file_type = "Zip of TXT Files" #Zip of VTT Files, Zip of CSV Files, Zip of TXT Files, VTT, CSV, TXT
    openai_api_key = "your key"
    #test_vtt.zip, test_text_transcripts.zip, ../NTO/test_csv.zip,drive-download-20250325T133806Z-001/878010491_captions.vtt, 878010491_captions_converted.csv, A2 Transcript of user 216886 tutor session on 2023-09-25 LS id 4760786.vtt
    transcript_file = "UpChieve_4_transcripts.zip" 
    #prompt_file = "math_error_filter_prompt.txt" math_error_evaluation_prompt.txt math_error_by_line_prompt_gpt35
    prompt_file = "math_error_filter_prompt.txt" 
    max_token = 200
    num_trails = 3
    temperature = 1
    utterances_col = "Text"
    
#ensure required arguments are present:
if transcript_file is None:
    print("The required argument, transcript_file, is missing")
    sys.exit(1)
if prompt_file is None:
    print("The required argument, prompt_file, is missing")
    sys.exit(1)
if transcript_file_type is None:
    print("The required argument, transcript_file_type, is missing")
    sys.exit(1)
if (transcript_file_type == "CSV" or transcript_file_type == "Zip of CSV Files") and utterances_col is None:
    print("The argument, utterances_col, is required for CSV transcript file and is missing")
    sys.exit(1)    


#test
# print(transcript_file_type)
# print(openai_api_key)
# print(num_trails)
# print(temperature)
# print(utterances_col)
# print(transcript_file)
# print(prompt_file)

all_results = {}
log_file_name = os.path.join(working_dir, "tutor_transcript_evaluation.wfl")
#version 3.5
openai.api_key = openai_api_key
#version 4
#client = OpenAI(api_key=openai_api_key)

if transcript_file_type == 'Zip of VTT Files' or transcript_file_type == 'Zip of TXT Files':
    unzipped_file_folder = os.path.join(working_dir, "unzipped_files_temp")
    all_files = get_files_in_zip(transcript_file, unzipped_file_folder)
    cnt = 1
    for a_transcript_file in all_files:
        #delete the working dir:
        cleaned_transcript_file = a_transcript_file.replace(unzipped_file_folder, "", 1)
        cleaned_transcript_file = clean_filename(cleaned_transcript_file)
        logToWfl(f"Processing file: {cleaned_transcript_file}")
        print(f"Processing file: {cleaned_transcript_file}")
        try: 
            response = evaluation_file(a_transcript_file, prompt_file, cnt, len(all_files), num_tries=num_trails,
                                                  temperature=temperature, max_tokens=max_token) 
            #a_transcript_file = a_transcript_file.replace(unzipped_file_folder, "", 1)
            all_results[cleaned_transcript_file] = response
        except FileTypeError as e:
            logToWfl(f"File error occurred for file {a_transcript_file}: {e}")
            print(f"File error occurred for file {a_transcript_file}: {e}")
            all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"File error occurred for file {a_transcript_file}: {e}"}])
            continue
        except Exception as e:
            logToWfl(f"An error occurred for file {transcript_file}: {e}")
            print(f"An error occurred for file {transcript_file}: {e}")
            all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"An error occurred for file {a_transcript_file}: {e}"}])
            continue
        cnt = cnt + 1
                
    #delete the temp folder
    if os.path.exists(unzipped_file_folder) and os.path.isdir(unzipped_file_folder):
        shutil.rmtree(unzipped_file_folder)
        
elif transcript_file_type == 'Zip of CSV Files':
    unzipped_file_folder = os.path.join(working_dir, "unzipped_files_temp")
    all_files = get_files_in_zip(transcript_file, unzipped_file_folder)
    cnt = 1
    for a_transcript_file in all_files:
        #delete the working dir:
        cleaned_transcript_file = a_transcript_file.replace(unzipped_file_folder, "", 1)
        cleaned_transcript_file = clean_filename(cleaned_transcript_file)
        logToWfl(f"Processing file: {cleaned_transcript_file}")
        print(f"Processing file: {cleaned_transcript_file}")
        try: 
            response = evaluation_file(a_transcript_file, prompt_file, cnt, len(all_files), num_tries=num_trails, col=utterances_col,
                                                  temperature=temperature, max_tokens=max_token)
            #a_transcript_file = a_transcript_file.replace(unzipped_file_folder, "", 1)
            all_results[cleaned_transcript_file] = response
        except FileTypeError as e:
            logToWfl(f"File error occurred for file {a_transcript_file}: {e}")
            print(f"File error occurred for file {a_transcript_file}: {e}")
            all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"File error occurred for file {a_transcript_file}: {e}"}])
            continue
        except Exception as e:
            logToWfl(f"An error occurred for file {a_transcript_file}: {e}")
            print(f"An error occurred for file {a_transcript_file}: {e}")
            all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"An error occurred for file {a_transcript_file}: {e}"}])
            continue
        cnt = cnt + 1
                
    #delete the temp folder
    if os.path.exists(unzipped_file_folder) and os.path.isdir(unzipped_file_folder):
        shutil.rmtree(unzipped_file_folder)
            
elif transcript_file_type == 'VTT' or transcript_file_type == 'TXT':
    cleaned_transcript_file = transcript_file.replace(working_dir, "", 1)
    cleaned_transcript_file = clean_filename(cleaned_transcript_file)
    cleaned_transcript_file = os.path.basename(cleaned_transcript_file)
    logToWfl(f"Processing file: {cleaned_transcript_file}")
    print(f"Processing file: {cleaned_transcript_file}")
    try:
        response = evaluation_file(transcript_file, prompt_file, 1, 1, num_tries=num_trails,
                                              temperature=temperature, max_tokens=max_token)
        all_results[cleaned_transcript_file] = response
    except FileTypeError as e:
        logToWfl(f"File error occurred for file {transcript_file}: {e}")
        print(f"File error occurred for file {transcript_file}: {e}")
        all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"File error occurred for file {transcript_file}: {e}"}])
    except Exception as e:
        logToWfl(f"An error occurred for file {transcript_file}: {e}")
        print(f"An error occurred for file {transcript_file}: {e}")
        all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"An error occurred for file {transcript_file}: {e}"}])
        
elif transcript_file_type == 'CSV':
    cleaned_transcript_file = transcript_file.replace(working_dir, "", 1)
    cleaned_transcript_file = clean_filename(cleaned_transcript_file)
    cleaned_transcript_file = os.path.basename(cleaned_transcript_file)
    logToWfl(f"Processing file: {cleaned_transcript_file}")
    print(f"Processing file: {cleaned_transcript_file}")
    try:
        response = evaluation_file(transcript_file, prompt_file, 1, 1, num_tries=num_trails, col=utterances_col,
                                              temperature=temperature, max_tokens=max_token) #
        all_results[cleaned_transcript_file] = response
    except FileTypeError as e:
        logToWfl(f"File error occurred for file {transcript_file}: {e}")
        print(f"File error occurred for file {transcript_file}: {e}")
        all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"File error occurred for file {transcript_file}: {e}"}])
    except Exception as e:
        logToWfl(f"An error occurred for file {transcript_file}: {e}")
        print(f"An error occurred for file {transcript_file}: {e}")
        all_results[cleaned_transcript_file] = pd.DataFrame([{"Other Error": f"An error occurred for file {transcript_file}: {e}"}])
        
# print("all results")
# print(all_results)

#output result
#make df with 
df = None
columns = ['Transcript File Name']

df = pd.DataFrame()
#is a dict with file name as key, a dict as value which has trial name is key and value is a dataframe
for transcript_file, transcript_value in all_results.items():
    for trial_name, data_df in transcript_value.items():
        if data_df is None or data_df.empty:
            new_row = {}
            new_row['Transcript Name'] = transcript_file
            new_row['Trial'] = trial_name
            new_row['Response'] = "Blank response from server"
            df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
        elif not isinstance(data_df, pd.DataFrame):
            new_row = {}
            new_row['Transcript Name'] = transcript_file
            new_row['Trial'] = trial_name
            new_row['Response'] = data_df
            df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
        else:
            data_columns = data_df.columns
            for data_index, data_row in data_df.iterrows():
                new_row = {}
                new_row['Transcript Name'] = transcript_file
                new_row['Trial'] = trial_name
                for data_col in data_columns:
                    new_row[data_col] = data_row[data_col]
                df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)
df.to_csv(os.path.join(working_dir,'tutor_evaluation_result.csv'), index=False) 
                
                
    


# In[ ]:




