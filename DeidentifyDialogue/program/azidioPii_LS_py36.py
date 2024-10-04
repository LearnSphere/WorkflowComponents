
# coding: utf-8

# In[22]:


"""
Deidentification script that gives the user the option between Azure langauge services (paid), Amazon Comprehend(paid), or Presidio (free) for pii
detection. Also, when it comes to replacing the pii the user has the option to either encode with hash values or to use HIPS.
"""
import argparse
import csv
import os
import shutil
import sys
import warnings
from typing import Union, Tuple
from datetime import datetime
import pandas as pd
from faker import Faker
from azure.core.credentials import AzureKeyCredential
from azure.ai.textanalytics import TextAnalyticsClient
from presidio_analyzer import AnalyzerEngine
from presidio_analyzer.nlp_engine import SpacyNlpEngine
import spacy
with warnings.catch_warnings():
    warnings.simplefilter("ignore")
    import boto3
    import botocore.exceptions
from jproperties import Properties
from email_validator import validate_email, EmailNotValidError

#from azureConfig import loginConfig


# In[11]:


def get_azure_client(API_KEY, END_POINT) -> TextAnalyticsClient:
    """Retrieves the azure client so we can interact with api"""

    client = TextAnalyticsClient(
        endpoint=END_POINT,
        credential=AzureKeyCredential(API_KEY)
    )
    return client
#print(get_azure_client())


# In[12]:


def get_presidio_client() -> AnalyzerEngine:
    """Initializes the presidio engine we will use for pii detection"""
    nlp = spacy.load('en_core_web_lg')
    #nlp = spacy.load('en_core_web_sm')
    class CustomSpacyNlpEngine(SpacyNlpEngine):
        """Custom nlp model that extends the SpacyNlpEngine from presidio."""
        def __init__(self, model):
            super().__init__()
            self.nlp = {"en": model}
    return AnalyzerEngine(nlp_engine=CustomSpacyNlpEngine(nlp))
#print(get_presidio_client())


# In[13]:


def get_comprehend_client(access_key, secret_key) -> boto3.client:
    """
    Retrieves the AWS Comprehend client so we can interact with api.
    After configuring your aws account on the cl a .aws/credentials is created in the local directory
    for boto3 to reference when it comes to access keys
    """

    client = boto3.client(
        service_name='comprehend',
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        region_name='us-west-2'
    )

    return client



# In[14]:


def update_encoding_file(hash_key, value) -> None:
    """update our encoding file as we go with any new hashes we find"""
    with open(updated_encoding_file, 'a', newline='', encoding='utf-8') as f:
        if f.tell() == 0:
            f.write('\n')
        w = csv.writer(f)
        w.writerow([value, hash_key])

def load_name_hash_mapping(csv_file, name_col, hash_col) -> None:
    """
    This function reads in a csv file and fills our name_hash_dict dictionary that represents the key, value pairs 
    between names and their encoded hashes.
    """

    name_hash_df = pd.read_csv(csv_file)
    name_id = name_hash_df.columns.get_loc(name_col)
    hash_id = name_hash_df.columns.get_loc(hash_col)
    for _, row in name_hash_df.iterrows():
        name, hash_value = row.iloc[name_id].strip(), row.iloc[hash_id].strip()
        name_hash_dict[name.lower()] = hash_value
        
def is_email(entity) -> bool:
    """Checks if a string is a valid email address"""
    try:
        validate_email(entity, check_deliverability=False)
        return True
    except EmailNotValidError:
        return False


# In[15]:


def redact_pii(text, client, method, hips_boolean) -> Union[str, Tuple[None, Union[str, Exception]]]:
    """
    Takes in text and using the apropiate client (Azure/Presidio/Comprehend) will either encode/HIPS for all instances of pii in the text 
    PARAMETERS:
    client - 
        The tool we're using to detect pii -> Azure/Presidio/Comprehend
    method - 
        String representation of the tool that we'll be using so that we can make the apropiate client calls
    """
    if not isinstance(text, str) or len(text) == 0:
        return text
    try:
        #build aprop client for our method
        if method == 'azure':
            response = client.recognize_pii_entities([text], language="en")
            if response is None:
                return None, "Error: No response received from recognize_pii_entities."
            response = response[0].entities

        elif method == 'presidio':
            response = client.analyze(text=text, entities=["PERSON", "EMAIL_ADDRESS"], language='en')
            if response is None:
                return None, "Error: No response received from analyze."
        elif method == 'comprehend':
            response = client.detect_entities(Text=text, LanguageCode='en')
            if response is None:
                return None, "Error: No response received from detect_entities"
            response = response['Entities']
        redacted_text = text
        # sort in reverse so we dont mess up index order, but not needed
        #response.sort(key=lambda x: x.start, reverse=True)
        #print(response)
        for entity in response: 
            #filter out all values that dont fall into the aprop person/email categories for our methods
            if method == 'azure':
                category = entity.category
                entity_text = entity.text
            elif method == 'presidio':
                category = entity.entity_type
                entity_text = text[entity.start:entity.end]
            elif method == 'comprehend':
                category = entity['Type']
                entity_text = entity['Text']

            if category not in ['Person', 'Email'] and method == 'azure':
                continue
            if category not in ['PERSON', 'EMAIL_ADDRESS'] and method == 'presidio':
                continue
            if category not in ['PERSON', 'OTHER', 'ORGANIZATION'] and method == 'comprehend':
                continue
            #Comprehend doesn't have an email tag, so we have to make sure this OTHER instance is actually an email
            if category in ['OTHER'] and not is_email(entity_text):
                continue 

            #switch other -> email for comprehend so we can get prefixes when encoding 
            if category in ['OTHER'] and method == 'comprehend':
                category = 'email'

            #switch org -> person for comprehend so we can get prefixes when encoding
            if category in ['ORGANIZATION'] and method == 'comprehend':
                category = 'person'

            #determine if we'll use hips/encoding
            if hips_boolean:
                redacted_text = hide_pii(entity_text, category, redacted_text)
            else:
                redacted_text = encode_pii(entity_text, category, redacted_text)
        return redacted_text
    
    #catch if anything goes wrong, return as tuple so we can be flagged at the None then print the error to user
    except Exception as e:
        return None, e
    
#test presidio
#line_text = redact_pii("hello, Jaden, John Smith can you hear me? hcheng688@hotmail.com", get_presidio_client(), "presidio", True)
#line_text = redact_pii("hello, Jaden, John Smith can you hear me? hcheng688@hotmail.com", get_presidio_client(), "presidio", False)
#print(line_text)


# In[16]:


def encode_pii(entity_text, category, current_text) -> str:
    """Takes in the text of our instance and encodes it based off our hash values"""
    prefix = category.lower() if category.lower() in ['person', 'email', 'email_address'] else 'obj'
    if entity_text.lower() in name_hash_dict:
        current_text = current_text.replace(entity_text, name_hash_dict[entity_text.lower()])
    else:
        #if not in our dict generate new hash value for entity and update the dict
        new_index = len(name_hash_dict) + 1
        hash_value = f"{prefix}_{new_index:07d}"
        name_hash_dict[entity_text.lower()] = hash_value
        update_encoding_file(hash_value, entity_text.lower())
        current_text = current_text.replace(entity_text, name_hash_dict[entity_text.lower()])
    return current_text

def hide_pii(entity_text, category, current_text) -> str:
    """Takes in the text of our instance and generates a fake value to hip"""
    if category.lower() == "person":
        if entity_text.lower() in name_hips_dict:
            random_value = name_hips_dict[entity_text.lower()]
        else:
            random_value = RANDOMS.first_name()
            name_hips_dict[entity_text.lower()] = random_value
            update_encoding_file(random_value, entity_text.lower())

    elif category.lower() == "email" or category.lower() == 'email_address' or category.lower() == 'other':
        if entity_text.lower() in name_hips_dict:
            random_value = name_hips_dict[entity_text.lower()]
        else:
            random_value = RANDOMS.email()
            name_hips_dict[entity_text.lower()] = random_value
            update_encoding_file(random_value, entity_text.lower())
    else:
        #keep the same 
        random_value = entity_text
    current_text = current_text.replace(entity_text, random_value)
    return current_text


# In[17]:


def handle_csv(output_file, transcript_file, client, method, hips_boolean, ignore_columns) -> None:
    """This file is meant to handle when csvs are passed in"""

    df = pd.read_csv(transcript_file)
    print(f"starting to encode: {transcript_file}")

    with open(output_file, 'w', encoding='utf-8', newline='\n') as file:
        file.write(','.join(df.columns) + '\n')

        columns = df.columns
        for index, row in df.iterrows():
            cleaned_row = []

            column_index = 0
            #goes cell by cell -> row by row to detect, update pii
            for cell in row:

                #check if we should skip
                cell_column = columns[column_index]
                if cell_column in ignore_columns:
                    column_index += 1
                    cleaned_row.append(cell)
                    continue

                if pd.isnull(cell):
                    cell = ''

                redacted_cell = redact_pii(cell, client, method, hips_boolean)
                if isinstance(redacted_cell, tuple):
                    sys.exit(f"Managed to process {index-1} rows before failing. We encountered the following error: {redacted_cell[1]}")

                cleaned_row.append(f'{redacted_cell}')
                column_index += 1

            file.write(','.join(map(str, cleaned_row)) + '\n')
            if (index+1) % CSV_ROW_UPDATE == 0:
                print(f"In progress: {index} rows encoded")
        print(f"{len(df)} rows completed")

def handle_other(output_file, transcript_file, client, method, hips_boolean) -> None:
    """
    This method is meant to handle text files that can be passed in
    PARAMETERS:
    output_file - 
        The file we mean to write all of our output to
    transcript_file - 
        The file with pii that was originall passed into the script on the cl
    """
    with open(transcript_file, 'r', encoding='utf-8') as file:
        transcript = file.read()

    #azure has a cap on the max amount of lines that can be passed in at once, batch here
    chunks = [transcript[i:i+5120] for i in range(0, len(transcript), 5120)]
    print(f"starting to encode: {transcript_file}")
    with open(output_file, 'w', encoding='utf-8', newline='\n') as file:
        index = 1
        for chunk in chunks:
            modified_chunk = redact_pii(chunk, client, method, hips_boolean)
            if isinstance(modified_chunk, tuple):
                sys.exit(f"Managed to process {index-1} chunks before failing. We encountered the following error: {modified_chunk[1]}")
            file.write(modified_chunk)
            print(f"finished chunk {index}")
            index += 1

#handle_other("random_transcript_cleaned.json", "random_transcript.json", get_presidio_client(), "presidio", False)
#handle_other("random_transcript_cleaned.json", "random_transcript.json", get_presidio_client(), "presidio", True)


# In[18]:


def hips_method(pii_file, client, method, ignore_columns) -> None:
    """Declares file name for the returned pii file and preps clients to use hips when replacing pii"""
    filename, file_extension = os.path.splitext(os.path.basename(pii_file))
    return_pii_file = f"{filename}_cleaned{file_extension}"
    
    with open(updated_encoding_file, 'w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerow(["name", "hash"])

    try:
        if file_extension == '.csv':
            handle_csv(return_pii_file, pii_file, client, method, True, ignore_columns)
        else:
            handle_other(return_pii_file, pii_file, client, method, True)
    except Exception as e:
        print(f"An error occurred while reading the PII file: {e}")
        sys.exit(1)
        
def encoding_method(encoding_file, pii_file, name_col, hash_col, client, method, ignore_columns) -> None:
    """Declares file names for the returned pii file and the encodings we have. Preps clients to use encoding when replacing pii"""
    global updated_encoding_file
    print(1)
    try:
        print(2)
        load_name_hash_mapping(encoding_file, name_col, hash_col)
        filename, file_extension = os.path.splitext(os.path.basename(encoding_file))
        updated_encoding_file = f"{filename}_updated{file_extension}"
        shutil.copyfile(encoding_file, updated_encoding_file)
    except:
        print('No passed in encoding file, will start with empty one')
        with open(updated_encoding_file, 'w', newline='', encoding='utf-8') as file:
            writer = csv.writer(file)
            writer.writerow(["name", "hash"])

    filename, file_extension = os.path.splitext(os.path.basename(pii_file))
    return_pii_file = f"{filename}_cleaned{file_extension}"

    try:
        if file_extension == '.csv':
            handle_csv(return_pii_file, pii_file, client, method, False, ignore_columns)
        else:
            handle_other(return_pii_file, pii_file, client, method, False)
    except Exception as e:
        print(f"An error occurred while reading the PII file: {e}")
        sys.exit(1)



# In[19]:


# Globals
#API_KEY = loginConfig['API_KEY']
#END_POINT = loginConfig['END_POINT']
name_hash_dict = {}
name_hips_dict = {}
updated_encoding_file = "updated_encoding_file.csv"
CSV_ROW_UPDATE = 100
RANDOMS = Faker()
DATE_TIME = datetime.now().strftime("%Y%m%d_%H%M%S")


# In[21]:


#test on command line
#C:\Users\hchen\Anaconda3\envs\36_env\python.exe azidioPii_LS_py36.py -programDir . -workingDir . -userId 1 -Hips_boolean No -method Presidio -piiFileType Non-CSV -skipCol No -useEncoding No -node 0 -fileIndex 0 random_transcript.json
#config file, hips no, azure, noncsv, skip no
#C:\Users\hchen\Anaconda3\python.exe azidioPii_LS.py -programDir . -workingDir . -userId hcheng -Hips_boolean No -method Azure -piiFileType Non-CSV -skipCol No -useEncoding No -node 0 -fileIndex 0 random_transcript.json -node 2 -fileIndex 0 config_file.txt
#command line
command_line = True
if command_line:
    parser = argparse.ArgumentParser(description="Deidentification script using Azure, Comprehend, or Presidio")
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument("-node", action='append')
    parser.add_argument("-method", help="Method to use for deidentification: 'Azure', 'Comprehend' or 'Presidio'", type=str, required=True, choices=['Azure', 'Presidio', 'Comprehend'])
    parser.add_argument("-Hips_boolean", help="Boolean to decide which method to use.", type=str, choices=['Yes', 'No'], default="No")
    parser.add_argument("-useEncoding", help="Boolean to decide if using encoding.", type=str, choices=['Yes', 'No'], default="No")
    parser.add_argument("-skipCol", type=str, choices=['Yes', 'No'], default="No")
    parser.add_argument("-skip_columns", action='append')
    parser.add_argument("-piiFileType", help="File type.", type=str, choices=['CSV', 'Non-CSV'], default="Non-CSV")
    parser.add_argument('-Hash_col', type=str, help='column to use for hash in the encoding file')
    parser.add_argument('-name_col', type=str, help='column to use for name in the encoding file')
    parser.add_argument("-aws_access_key", help="Access key for the account that we want to use for the amazon comprehend call", type=str)
    parser.add_argument("-aws_secret_key", help="Secret key for the account that we want to use azure", type=str)
    parser.add_argument("-api_key", help="API key for the account that we want to use azure", type=str)
    parser.add_argument("-end_point", help="END POINT for the account that we want to use azure", type=str)
    #parser.add_argument("-use_config", help="Use config file.", type=str, choices=['Yes', 'No'], default="No")
    

    #args = parser.parse_args()
    args, option_file_index_args = parser.parse_known_args()
    working_dir = args.workingDir
    program_dir = args.programDir
    pii_file = None
    encoding_file = None
    #config_file = None
    
    
    
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            pii_file = args.fileIndex[x][1]
        if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
            encoding_file = args.fileIndex[x][1]
#         if (args.node[x][0] == "2" and args.fileIndex[x][0] == "0"):
#             config_file = args.fileIndex[x][1]

    method = (args.method).lower()
    hips_boolean = False
    if (args.Hips_boolean).lower() == "yes":
        hips_boolean = True
    skip_columns = None
    if args.skipCol == "Yes" and args.skip_columns is not None:
        skip_columns = args.skip_columns
    hash_col = None
    name_col = None
    if args.Hash_col is not None:
        hash_col = args.Hash_col
    if args.name_col is not None:
        name_col = args.name_col
        
    aws_access_key = None
    aws_secret_key = None
    api_key = None
    end_point = None
    if method in ['azure', 'comprehend']:
        #if (args.use_config).lower() == "no":
        if args.aws_access_key is not None:
            aws_access_key = args.aws_access_key
        if args.aws_secret_key is not None:
            aws_secret_key = args.aws_secret_key
        if args.api_key is not None:
            api_key = args.api_key
        if args.end_point is not None:
            end_point = args.end_point
#         else:
#             if config_file is not None:
#                 configs = Properties()
#                 with open(config_file, 'rb') as cfile:
#                     configs.load(cfile)
#                     if configs.get("AWS_ACCESS_KEY") is not None:
#                         aws_access_key = configs.get("AWS_ACCESS_KEY").data
#                     if configs.get("AWS_SECRET_KEY") is not None:
#                         aws_secret_key = configs.get("AWS_SECRET_KEY").data
#                     if configs.get("API_KEY") is not None:
#                         api_key = configs.get("API_KEY").data
#                     if configs.get("END_POINT") is not None:
#                         end_point = configs.get("END_POINT").data
                    
else:
    # use proper client
    #method = "azure"
    #method = "presidio"

    # hips_boolean = True
    # encoding_file = None

    # hips_boolean = False
    # encoding_file = None

    hips_boolean = True
    encoding_file = "updated_encoding_file.csv"
    
    #pii_file = "random_transcript.json"
    pii_file = "csv_file_test.csv"
    skipCol = "Yes"
    skip_columns = ['name', 'hash']
    api_key = ""
    end_point = "https://remove-pii.cognitiveservices.azure.com/"
    hash_col = "hash"
    name_col = "name"
    
    
#test
# print(pii_file)
# print(encoding_file)
# print(method)
# print(hips_boolean)
# print(skip_columns)
# print(hash_col)
# print(name_col)
# print(aws_access_key)
# print(aws_secret_key)
# print(api_key)
# print(end_point)
    
if method == 'azure':
    client = get_azure_client(api_key, end_point)
elif method == 'presidio':
    client = get_presidio_client()
else:
    if aws_access_key is None or aws_secret_key is None:
        print("AWS access key or secret key not provided")
        sys.exit(1)
    try:
        client = get_comprehend_client(aws_access_key, aws_secret_key)
    except botocore.exceptions.NoCredentialsError:
        print("Invalid AWS credentials")
        sys.exit(1)

# track if there are columns we want to skip, if not just use empty set
try:
    skip_columns = set(skip_columns)
except:
    skip_columns = set()

# use proper encoding method
if hips_boolean:
    hips_method(pii_file, client, method, skip_columns)
    print(f"Successfully hid PII in {pii_file}")
else:
    encoding_method(encoding_file, pii_file, name_col, hash_col, client, method, skip_columns)
    print(f"Successfully encrypted {pii_file}")

