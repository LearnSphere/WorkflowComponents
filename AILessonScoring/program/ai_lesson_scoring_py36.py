
# coding: utf-8

# In[3]:


import argparse
import pandas as pd
import openai
import os
import sys
import json
import datetime as dt
import configparser


# In[4]:


def extract_response(response_obj):
    response_json = json.loads(response_obj['choices'][0]['text'])
    rationale = response_json["Rationale"]
    score = response_json["Score"]
    return (score, rationale)
    
def logProgressToWfl(progressMsg):
    logFile = open(log_file_name, "a")
    now = dt.datetime.now()
    progressPrepend = "%Progress::"
    logFile.write(progressPrepend + "@" + str(now) + "@" + progressMsg + "\n");
    logFile.close();


# In[5]:


def score_inputs(inputs, prompt_start):
    new_df = pd.DataFrame(columns = ['score','rationale'])
    #not over RUN_UP_TO, If an upper bound is set, get response less than this number
    if RUN_UP_TO >=  0:  
        inputs_upto = inputs[:RUN_UP_TO]
    else:
        inputs_upto = inputs  # Take the whole set of responses
    loop_cnt = 1
    for inpt in inputs_upto:
        progress = loop_cnt/len(inputs_upto)
        progress = f"{progress * 100:.{0}f}%"
        logProgressToWfl(progress)
        new_row = {}
        if pd.isnull(inpt):
            new_row['score'] =  "---"
            new_row['rationale'] = "---" 
        else:
            prompt = f"""{prompt_start} {inpt} --- Response End.
                    Given the response, please score the tutor response and give your rationale in a JSON string following the format, {{"Rationale": "your reasoning here", "Score":0/1}}. """            
            try:
                response = openai.Completion.create(engine = MODEL, prompt = prompt, temperature = TEMPERATURE, max_tokens = MAX_TOKENS)
                score, rationale = extract_response(response)
                new_row['score'] = score
                new_row['rationale'] = rationale
            except Exception as e:
                new_row['score'] =  "---"
                new_row['rationale'] = f"OpenAI experienced error: {e}" 
        new_df = pd.concat([new_df, pd.DataFrame([new_row])], ignore_index=True) # Failsafe
        loop_cnt = loop_cnt + 1
    
    #if data is more than RUN_UP_TO
    if len(inputs) > len(new_df):
        for i in range(len(inputs)-len(new_df)):
            new_row = {'score':"---", 'rationale':"---"}
            new_df = pd.concat([new_df, pd.DataFrame([new_row])], ignore_index=True) 
    new_df = new_df[["score", "rationale"]]
    return new_df


# In[10]:


#test command
#test: explain with key
#C:\Users\hchen\Anaconda3\python.exe ai_lesson_scoring_py36.py -programDir . -workingDir . -userId hcheng -lesson "Helping Students Manage Inequity" -openai_api_key somekey -predict_explain Explain -scoringCol_nodeIndex 0 -scoringCol_fileIndex 0 -scoringCol Input -node 0 -fileIndex 0 HSME_predict.csv -node 1 -fileIndex 0 config_file.txt
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description="AI Lessons Scoring")
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-node", action='append')
    parser.add_argument("-lesson", help="4 lessons to pick", type=str, required=True)
    parser.add_argument("-predict_explain", help="predict or explain", type=str, required=True, choices=['Predict', 'Explain'])
    parser.add_argument("-scoringCol", type=str, help='column to score')
    #parser.add_argument("-have_api_key", help="Boolean to decide which key to use.", type=str, choices=['Yes', 'No'], default="Yes")
    #parser.add_argument("-use_config", help="Boolean to decide if key is from config file.", type=str, choices=['Yes', 'No'], default="Yes")
    parser.add_argument("-openai_api_key", help="API key for the account that we want to use azure", type=str)
    
    args, option_file_index_args = parser.parse_known_args()
    
    working_dir = args.workingDir
    program_dir = args.programDir
    data_file = None
    #config_file = None
    
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            data_file = args.fileIndex[x][1]
#         if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
#             config_file = args.fileIndex[x][1]
            
    column_to_score = args.scoringCol
    lesson = args.lesson
    predict_explain = (args.predict_explain).lower()
    
    #have_api_key = (args.have_api_key).lower()
    #use_config = (args.use_config).lower()
    api_key = args.openai_api_key
                    
#     if have_api_key == "yes" and use_config == "yes":
#         if config_file is not None:
#             config = configparser.ConfigParser()
#             config.read(config_file)
#             api_key = config.get('section_key', 'OPENAI_API_KEY')
    
#     if have_api_key == "no":
#         api_key = None

else:
    working_dir = "."
    program_dir = "."
    data_file = "HSME_predict.csv"
    #data_file = "Helping Students Manage Inequity_test.csv"
#     config_file = "config_file.txt"
#     config = configparser.ConfigParser()
#     config.read(config_file)
#     api_key = config.get('section_key', 'OPENAI_API_KEY')
    api_key = "some key"
    column_to_score = "Input"
    #column_to_score = "Response"
    lesson = "Helping Students Manage Inequity"
    #lesson = "Giving Effective Praise"
    predict_explain = "predict"
    
# print(data_file)
# print(config_file)
# print(column_to_score)
# print(lesson)
# print(predict_explain)
# print(api_key)

#get constant from file config.properties
config = configparser.ConfigParser()

sys_config_file = os.path.join(program_dir, "program")
sys_config_file = os.path.join(sys_config_file, 'config.properties')
config.read(sys_config_file)           
            
#set parameters
MAX_TOKENS = int(config.get('section_engine', 'MAX_TOKENS'))
TEMPERATURE = int(config.get('section_engine', 'TEMPERATURE'))
RUN_UP_TO = int(config.get('section_engine', 'RUN_UP_TO'))
MODEL = config.get('section_engine', 'MODEL')

# if api_key is None or api_key == "":
#     api_key = config.get('section_key', 'OPENAI_API_KEY')

lesson_prompt_dic = {"Helping Students Manage Inequity" : "Helping Students Manage Inequity.csv",
                    "Determining What students Know" : "Determining What students Know.csv",
                    "Giving Effective Praise" : "Giving Effective Praise.csv",
                    "Reacting to Errors" : "Reacting to Errors.csv"}

#fresh new log file
log_file_name = "AI_lesson_scoring.wfl"
logFile = open(log_file_name, "w")
logFile.close();


#data file
df = pd.read_csv(data_file, encoding="ISO-8859-1")
inputs_to_score = df[column_to_score].tolist()

#prompt file
prompt_file_name = lesson_prompt_dic[lesson]
prompt_file = None
if prompt_file_name is not None and prompt_file_name != "":
    prompt_file = os.path.join(program_dir, "program")
    prompt_file = os.path.join(prompt_file, prompt_file_name)
else:
    sys.exit(f'Lesson: {lesson} is not supported')
df_prompt = None
#check if prompt_file exist
if os.path.exists(prompt_file):
    df_prompt = pd.read_csv(prompt_file, encoding="ISO-8859-1")
else:
    sys.exit(f'Prompt file not found for lesson: {lesson}')
    
scoring_prompt_start = df_prompt.loc[df_prompt['type'] == predict_explain, 'scoring_prompt_start'].values[0]
#scoring_format_prompt = df_prompt.loc[df_prompt['type'] == predict_explain, 'scoring_format_prompt'].values[0]

openai.api_key = api_key

#scored_df = score_inputs(inputs_to_score, scoring_prompt_start, scoring_format_prompt)
scored_df = score_inputs(inputs_to_score, scoring_prompt_start)


# In[11]:


#concatenate with original df
df_with_score = pd.concat([df, scored_df], axis=1)
#reorder column and put new columns next to column_to_score
df_with_score_cols = df_with_score.columns.tolist()
column_to_score_ind = df_with_score_cols.index(column_to_score)
new_cols =  df_with_score_cols[:column_to_score_ind+1] + df_with_score_cols[len(df_with_score_cols)-2:] + df_with_score_cols[column_to_score_ind+1:len(df_with_score_cols)-2]
df_with_score = df_with_score[new_cols] 
#rename
df_with_score.rename(columns={'score': 'openAI_score', 'rationale': 'openAI_rationale'}, inplace=True)

#new file name
new_file_name = os.path.splitext(os.path.basename(data_file))[0] + "_scored" + os.path.splitext(os.path.basename(data_file))[1]
new_file_name = os.path.join(working_dir, new_file_name)
df_with_score.to_csv(new_file_name, index=False) 

