#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np
import sys
import datetime as dt
import argparse
import re
import copy
import pytz


# In[2]:


#check/convert time format
def checkDatetimeFormat(colName):
    try:
        return pd.to_datetime(df[colName])
    except ValueError:
        return None


# In[3]:


#check/convert numeric format
def checkIntegerFormat(df, colName):
    try:
        return df[colName].astype(int)
    except ValueError:
        return None


# In[4]:


#check/convert numeric format
def checkNumericData(colName):
    try:
        pd.to_numeric(df[colName])
        return True
    except ValueError:
        return False


# In[5]:


#make dataframe with unique combination of columns passes
def uniqueColumnsDF(cols, includeNullValue=True):
    uniqueDF = None
    if includeNullValue:
         uniqueDF = df[cols].drop_duplicates().reset_index()
    else:
        uniqueDF = df[cols].drop_duplicates().dropna().reset_index()
    #drop the old index column and get new index
    uniqueDF.drop('index', axis=1, inplace=True)
    uniqueDF.reset_index(inplace=True)
    return uniqueDF


# In[6]:


#adjust for time zone: timeDF has a time column followed a time zone column
#change everything to UTC
def to_UTC(row):
    return row[0]
    
def to_UTC_not_working(row):
    if not row[1]:
        return row[0]
    else:
        timezone_str = row[1]
        if timezone_str in pytz.all_timezones:
            try:
                return row[0].tz_localize(row[1]).tz_convert('UTC')
            except:
                return row[0]
        else:
            if timezone_str.upper() == "EST":
                try:
                    return row[0].tz_localize("America/New_York").tz_convert('UTC')
                except:
                    return row[0]
            elif timezone_str.upper() == "CST":
                try:
                    return row[0].tz_localize("America/Chicago").tz_convert('UTC')
                except:
                    return row[0]
            elif timezone_str.upper() == "MST":
                try:
                    return row[0].tz_localize("America/Denver").tz_convert('UTC')
                except:
                    return row[0]
            elif timezone_str.upper() == "PST":
                try:
                    return row[0].tz_localize("America/Los_Angeles").tz_convert('UTC')
                except:
                    return row[0]
            else:
                return row[0]
            
    
#change UTC back to local
def to_local(row):
    if not row[1]:
        return row[0]
    else:
        return row[0].tz_localize('UTC').tz_convert(row[1])
    

def standardizeTimeZone(timeDF):
    try:
        return timeDF.apply(to_UTC, axis=1)
    except:
        return None
    
def localizeTimeZone(timeDF):
    try:
        return timeDF.apply(to_local, axis=1)
    except:
        return None


# In[7]:


def logToWfl(msg):
    logFile = open("transactionAggregatorLog.wfl", "a")
    now = dt.datetime.now()
    logFile.write(str(now) + ": " + msg + "\n");
    logFile.close();
    
def logProgressToWfl(progressMsg):
    logFile = open("transactionAggregatorLog.wfl", "a")
    now = dt.datetime.now()
    progressPrepend = "%Progress::"
    logFile.write(progressPrepend + "@" + str(now) + "@" + progressMsg + "\n");
    logFile.close();
  


# In[8]:


#test command from WF component:
#C:/Python35/Python studentStepRollup.py -programDir . -workingDir . -userId 1 -aggregatedTo "Student-step rollup" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (Original)" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (Area)" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (Textbook)" -node 0 -fileIndex 0 "C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransactionAggregator\test\test_data\ds76_tx_All_Data_74_2018_0912_070949.txt"
#C:/Python35/Python studentStepRollup.py -programDir . -workingDir . -userId 1 -aggregatedTo "Transaction" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (Original)" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (Area)" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (Textbook)" -node 0 -fileIndex 0 "C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransactionAggregator\test\test_data\ds76_tx_All_Data_74_2018_0912_070949.txt"
#C:/Python35/Python studentStepRollup.py -programDir . -workingDir . -userId 1 -aggregatedTo "Transaction" -node 0 -fileIndex 0 "C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransactionAggregator\test\test_data\ds76_tx_All_Data_74_2018_0912_070949.txt"

#C:/ProgramData/Anaconda3/Python studentStepRollup.py -programDir . -workingDir . -userId 1 -aggregatedTo "Student-step rollup" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (MATHia)" -kcModelsToAggregate_nodeIndex 0 -kcModelsToAggregate_fileIndex 0 -kcModelsToAggregate "KC (MATHia New)" -node 0 -fileIndex 0 "C:\WPIDevelopment\dev06_dev\test\emeralds2\MD_CL\data\transactions_data\test\ds4561_tx_All_Data_6650_2020_1026_055644.txt"
#command line
parser = argparse.ArgumentParser(description='Process datashop file.')
parser.add_argument('-programDir', type=str, help='the component program directory')
parser.add_argument('-workingDir', type=str, help='the component instance working directory')
parser.add_argument("-node", nargs=1, action='append')
parser.add_argument("-fileIndex", nargs=2, action='append')
parser.add_argument('-aggregatedTo', choices=["Student-step rollup", "Transaction"], help='the file type to aggregate to(default="Student-step rollup")', default="Student-step rollup")
parser.add_argument('-kcModelsToAggregate', nargs=1, action='append', type=str, help='the KC models that you would like to aggregate; e.g., "Item"')
parser.add_argument('-userId', type=str,  help='placeholder for WF', default='')
args, option_file_index_args = parser.parse_known_args()

#fresh new log file
logFile = open("transactionAggregatorLog.wfl", "w")
logFile.close();

file_encoding = 'utf8'        # set file_encoding to the file encoding (utf8, latin1, etc.)
input_fd = open(args.fileIndex[0][1], encoding=file_encoding, errors = 'backslashreplace')
df = pd.read_csv(input_fd, na_values=['null', 'na', 'n/a', 'nan'], sep="\t", error_bad_lines=False, low_memory=False)

originalAllColNames = df.columns

#flag for converting to student step
convertToStudentStep = True
if args.aggregatedTo == "Transaction":
    convertToStudentStep = False

kcModelsToInclude = []
if args.kcModelsToAggregate is not None:
    flattened = [val for sublist in args.kcModelsToAggregate for val in sublist]
    for x in flattened:
        patternC = re.compile('\\s*KC\\s*\\(( .* )\\s*\\)', re.VERBOSE)
        kcModelsToInclude.append(patternC.sub(r'\1', x))


# In[9]:


#test with jupyter notebook
if False:
    file_encoding = 'utf8'        # set file_encoding to the file encoding (utf8, latin1, etc.)
    #input_fd = open('test_data_transaction.txt', encoding=file_encoding, errors = 'backslashreplace')
    #input_fd = open('ds445_tx_All_Data_1469_2016_0403_085024.txt', encoding=file_encoding, errors = 'backslashreplace')
    #input_fd = open('ds76_tx_All_Data_74_2018_0912_070949_ori.txt', encoding=file_encoding, errors = 'backslashreplace')
    #input_fd = open('new_aggr_sp_no_data_in_event_type_results/ds2846_tx_test_converted_with_event_type_no_data.txt', encoding=file_encoding, errors = 'backslashreplace')
    #input_fd = open('ds76_tx_All_Data_74_2018_0912_070949_noPST_noPV.txt', encoding=file_encoding, errors = 'backslashreplace')
    input_fd = open('TXN_time_corrected_ds5221_tx_All_Data_7411_2023_0613_153700.txt', encoding=file_encoding, errors = 'backslashreplace')
    
    df = pd.read_csv(input_fd, na_values=['null', 'na', 'n/a', 'nan'], sep="\t", error_bad_lines=False, low_memory=False)
    originalAllColNames = df.columns
    #print(df.dtypes)
    #fresh new log file
    logFile = open("transactionAggregatorLog.wfl", "w")
    logFile.close();

    #flag for converting to student step
    convertToStudentStep = True
    #kcModelsToInclude = ['Lasso Model']
    #kcModelsToInclude = ['KC (Area)', 'KC (Original)']
    kcModelsToInclude = ['KC (chemistry_general1 4_concatenated)']


# In[10]:


if convertToStudentStep:
    #drop ignored columns
    keepColumns = ['Transaction Id', 
                   'Anon Student Id',
                    'Time',
                    'Time Zone',
                    'Problem Name',
                    'Problem View',
                    'Problem Start Time',
                    'Step Name',
                    'Outcome',
                    'Selection',
                    'Action',
                    'Input',
                    'Condition Name',
                    'Condition Type',
                    'School',
                    'Class',
                  'Event Type']
    keepPartialMatchedColumns = ['Level (', 'KC (', 'KC Category (', 'Condition ']
    dropColumns = []
    for x in df.columns.values.tolist():
        if (x not in keepColumns):
            findPartial = False
            for y in keepPartialMatchedColumns:
                if x.find(y) == 0:
                    findPartial = True
                    break;
            if not findPartial:
                dropColumns.append(x)

    df.drop(dropColumns, axis=1, inplace=True)
    ##write dropped column names to log file
    logToWfl('Student step rollup process has dropped these columns: %s\n' % (', '.join(dropColumns)))


# In[11]:


#delete un-interested KC columns for convertToStudentStep
if convertToStudentStep:
    if len(kcModelsToInclude) > 0:
        allColNames = df.columns
        for colName in allColNames:
            if colName.find('KC (') == 0:
                KCName = colName[len('KC (') : colName.find(')')]
                if KCName not in kcModelsToInclude:
                    df.drop(colName, axis=1, inplace=True)
            elif colName.find('KC Category (') == 0:
                KCName = colName[len('KC Category (') : colName.find(')')]
                if KCName not in kcModelsToInclude:
                    df.drop(colName, axis=1, inplace=True)
                


# In[12]:


#check null values in required columns: Anon Student Id, time, problem name
#if any null values, send out error message
requiredColumns = ['Anon Student Id', 'Time', 'Problem Name']
colNullSum = df.isnull().sum()
#print(colNullSum)
errorMsg = ""
for reqCol in requiredColumns:
    if colNullSum[reqCol] != 0:
        if errorMsg == "":
            errorMsg = "Found null values in required columns "
        errorMsg += reqCol + "; "
if errorMsg != "":
    ##write error to log file
    logToWfl(errorMsg + ', Process aborted.\n')
    sys.exit(errorMsg)    


# In[13]:


#check and convert time format for Time 
newCol = checkDatetimeFormat('Time')
if newCol is None:
    errorMsg = "Time column has invalid formatted time"
    ##write error to log file
    logToWfl(errorMsg + ', Process aborted.\n')
    sys.exit(errorMsg)
else:
    df['Time'] = newCol


# In[14]:


#check if at least one of problem view and problem start time columns exist
#and check and convert problem start time format
allColNames = df.columns
pvExist = 'Problem View' in allColNames
pstExist = 'Problem Start Time' in allColNames
if not pvExist and not pstExist:
    errorMsg = "Data missing both Problem Start Time and Problem View columns. At least one of them are required."
    ##write error to log file
    logToWfl(errorMsg + ', Process aborted.\n')
    sys.exit(errorMsg)
if pstExist:
    newCol = checkDatetimeFormat('Problem Start Time')
    if newCol is None:
        errorMsg = "Problem Start Time column has invalid formatted time"
        ##write error to log file
        logToWfl(errorMsg + ', Process aborted.\n')
        sys.exit(errorMsg) 
    else:
        df['Problem Start Time'] = newCol

#if problem view column exists, check to make sure it can be converted to number if not null
if pvExist and not checkNumericData('Problem View'):
    errorMsg = "Problem View column is not all integer"
    ##write error to log file
    logToWfl(errorMsg + ', Process aborted.\n')
    sys.exit(errorMsg)


# In[15]:


#adjust for time zone for Time and problem start time column
allColNames = df.columns
timezoneExist = 'Time Zone' in allColNames
timeZoneAdjusted = False
if timezoneExist:
    if len(df['Time Zone'].dropna().unique()) > 1:
        timeZoneAdjusted = True
        #get new time for Time and assign original time to another column
        df = df.reindex(columns = df.columns.tolist() + ['Original Time'])
        df['Original Time'] = df['Time']
        newCol = standardizeTimeZone(df.loc[:,['Time', 'Time Zone']])

        if (newCol is None):
            errorMsg = "Conversion of Time column to UTC time zone failed"
            ##write error to log file
            logToWfl(errorMsg + ', Process aborted.\n')
            sys.exit(errorMsg) 
        else:
            df['Time'] = newCol

        #get new time for Start problem time and assign original problem start time to another column
        if pstExist:
            df = df.reindex(columns = df.columns.tolist() + ['Original Problem Start Time'])
            df['Original Problem Start Time'] = df['Problem Start Time']
            newCol = standardizeTimeZone(df.loc[:,['Problem Start Time', 'Time Zone']])
            if newCol is None:
                errorMsg = "Conversion of Problem Start Time column to UTC time zone failed"
                ##write error to log file
                logToWfl(errorMsg + ', Process aborted.\n')
                sys.exit(errorMsg) 
            else:
                df['Problem Start Time'] = newCol


# In[121]:


#order df by student, time, problem_name PV and/or PST
#sortColm = ['Anon Student Id', 'Time', 'Problem Name']
sortColm = ['Anon Student Id', 'Time']
if pvExist:
    sortColm.append('Problem View')
if pstExist:
    sortColm.append('Problem Start Time')
df = df.sort_values(by=sortColm)


# In[72]:


#turn levels into problem hierarchy column and drop all level columns
def row_func_levels(row, cntRound, levelName, levelColName):
    partOne = levelName
    partTwo = row[levelColName]
    if pd.isnull(partTwo):
        partTwo = ''
    if cntRound == 0:
        return '%s %s' % (partOne, partTwo)
    else:
        return '%s, %s %s' % (row['Problem Hierarchy'],partOne,partTwo)

allColNames = df.columns
levelNames = []
levelColNames = []
for colName in allColNames:
    if 'Level (' in colName:
        levelNames.append(colName[len('Level (') : colName.find(')')])
        levelColNames.append(colName)
#add new column: Problem Hierarchy
df = df.reindex(columns = df.columns.tolist() + ['Problem Hierarchy'])
if levelNames:
    for i in range(len(levelNames)):
        df['Problem Hierarchy'] = df.apply(row_func_levels, args=(i, levelNames[i], levelColNames[i], ), axis=1)
if convertToStudentStep:
    #drop level columns 
    df.drop(levelColNames, axis=1, inplace=True)
    logToWfl('Combine these Levels columns into Problem Hierarchy column: %s\n' % (', '.join(levelColNames)))


# In[73]:


#turn Condition Name, Condition Type columns to Condition column
def row_func_condition(row):
    type = row['Condition Type']
    name = row['Condition Name']
    if pd.isnull(name):
        return ''
    elif pd.isnull(type):
        return name
    else:
        return '%s, %s'%(type, name)

if convertToStudentStep:
    allColNames = df.columns
    condNameExist = 'Condition Name' in allColNames
    condTypeExist = 'Condition Type' in allColNames   
    if condNameExist and condTypeExist:
        df = df.reindex(columns = df.columns.tolist() + ['Condition'])
        df['Condition'] = df.apply(row_func_condition, axis=1)
        #drop condition columns  
        df.drop(['Condition Name', 'Condition Type'], axis=1, inplace=True)
        logToWfl('Combine Condition Name and Condition Type columns into Condition column\n')
    elif (condNameExist and not condTypeExist) or (not condNameExist and condTypeExist):
        errorMsg = "Condition Name and Condition Type must be both present"
        ##write error to log file
        logToWfl(errorMsg + ', Process aborted.\n')
        sys.exit(errorMsg)


# In[74]:


#make dataframe for unique student+school+class
#replace the student, school class columns with student_id
studentUniqueColumns = ['Anon Student Id']
if 'School' in allColNames:
    studentUniqueColumns.append('School')
if 'Class' in allColNames:
    studentUniqueColumns.append('Class')
studentUniqueDF = uniqueColumnsDF(studentUniqueColumns)
df = pd.merge(df, studentUniqueDF,  how='left', on=studentUniqueColumns)
if convertToStudentStep:
    #drop original columns
    df.drop(studentUniqueColumns, axis=1, inplace=True)
df.rename(columns={'index': 'student_id'}, inplace=True)


# In[75]:


#make uniqueProblem(hierarchy+problem),
#replace problem name with problem_id
problemUniqueColumns = ['Problem Hierarchy', 'Problem Name']
problemUniqueDF = uniqueColumnsDF(problemUniqueColumns)
df = pd.merge(df, problemUniqueDF,  how='left', on=problemUniqueColumns)
if convertToStudentStep:
    #drop original columns
    df.drop(problemUniqueColumns, axis=1, inplace=True)
df.rename(columns={'index': 'problem_id'}, inplace=True)


# In[76]:


#make uniqueStep(problemId+step)
#replace step name with step_id
stepUniqueColumns = ['problem_id', 'Step Name']
stepUniqueDF = uniqueColumnsDF(stepUniqueColumns, includeNullValue=False)
df = pd.merge(df, stepUniqueDF,  how='left', on=stepUniqueColumns)
if convertToStudentStep:
    #drop original columns
    df.drop(['Step Name'], axis=1, inplace=True)
df.rename(columns={'index': 'step_id'}, inplace=True)


# In[77]:


#combine KC (model), KC Category (model) to KC (model)
def row_func_kc(row, KCColName, KCCategoryColName):
    kcName = row[KCColName]
    kcCategory = row[KCCategoryColName]
    if pd.isnull(kcName):
        return ''
    elif pd.isnull(kcCategory):
        return kcName
    else:
        #return '%s, %s'%(kcCategory, kcName)
        #not going to combine KC category. this is behavior in DS too
        return kcName

allColNames = df.columns
KCNames = []
KCColNames = []
uniqueSkillColumns = ['model', 'skill']
uniqueSkillDF = pd.DataFrame(columns=uniqueSkillColumns)
for colName in allColNames:
    if 'KC (' in colName:
        KCColName = colName
        KCCategoryColName = colName.replace('KC (', 'KC Category (')
        KCName = KCColName[len('KC (') : colName.find(')')]
        KCNames.append(KCName)
        KCColNames.append(KCColName)
        #if this is for transaction output, 
        if not convertToStudentStep:
            #save the original KC (model columns)
            df["Original " + KCColName] = df[KCColName]
        if KCCategoryColName in allColNames:
            if len(df[KCCategoryColName].value_counts()) > 0:
                df[KCColName] = df.apply(row_func_kc, args=(KCColName, KCCategoryColName, ), axis=1)
            if convertToStudentStep:
                #drop category column 
                df.drop([KCCategoryColName], axis=1, inplace=True)
        #make uniqueSkillDF
        tempUniqueDF = uniqueColumnsDF([KCColName])
        tempUniqueDF.drop(['index'], axis=1, inplace=True)
        tempUniqueDF.rename(columns={KCColName: 'skill'}, inplace=True)
        tempUniqueDF['model'] = KCName
        #add sort=True for new pandas version
        uniqueSkillDF = pd.concat([uniqueSkillDF, tempUniqueDF], sort=True)
        
uniqueSkillDF=uniqueSkillDF.drop_duplicates().reset_index()
uniqueSkillDF.drop('index', axis=1, inplace=True)
uniqueSkillDF.reset_index(inplace=True)

#replace skill name with id
df_pct = 0.1
for i in range(len(KCNames)):
    tempDF = uniqueSkillDF.loc[(uniqueSkillDF['model'] == KCNames[i])]
    if not convertToStudentStep:
        if i/len(KCNames) > df_pct:
            logProgressToWfl("{:.0%}".format(df_pct))
            df_pct = df_pct + 0.1
    if df[KCColNames[i]].dtypes != 'object':
        df[KCColNames[i]] = df[KCColNames[i]].astype(str)
    df = pd.merge(df, tempDF,  how='left', left_on=KCColNames[i], right_on='skill')
    #drop old KC (model) column
    df.drop([KCColNames[i], 'model', 'skill'], axis=1, inplace=True)
    #rename index to KC (model)
    df.rename(columns={'index': KCColNames[i]}, inplace=True)


# In[78]:


#make uniqueSkillStep(skillId+StepId), later use for kc(model) columns
uniqueSkillStepColumns = ['skill_id', 'step_id']
uniqueSkillStepDF = pd.DataFrame(columns=uniqueSkillStepColumns)
for i in range(len(KCNames)):
    tempSkillStepDF = uniqueColumnsDF([KCColNames[i], 'step_id'])
    tempSkillStepDF.drop(['index'], axis=1, inplace=True)
    tempSkillStepDF.rename(columns={KCColNames[i]: 'skill_id'}, inplace=True)
    uniqueSkillStepDF = pd.concat([uniqueSkillStepDF, tempSkillStepDF])

uniqueSkillStepDF=uniqueSkillStepDF.drop_duplicates().reset_index()
uniqueSkillStepDF.drop('index', axis=1, inplace=True)
uniqueSkillStepDF.reset_index(inplace=True)


# In[79]:


#delete all columns for KC to save space. the mapping info of model to skill are stored in uniqueSkillStepDF
df.drop(KCColNames, axis=1, inplace=True)


# In[80]:


#convert outcome column values: 
#contains hint or help = hint; equals to “ok” or “correct” = correct; equals to “error” or “bug” or “incorrect” = “incorrect”; else =”unknown”
#Outcome is not required
if 'Outcome' in df.columns:
    df['Outcome'] = df["Outcome"].apply(lambda x: None if pd.isnull(x) else ('correct' if x.lower() in ['correct', 'ok'] else x))
    df['Outcome'] = df["Outcome"].apply(lambda x: None if pd.isnull(x) else ('incorrect' if x.lower() in ['incorrect', 'bug', 'error'] else x))
    df['Outcome'] = df["Outcome"].apply(lambda x: None if pd.isnull(x) else ('hint' if any(substring in x.lower() for substring in ['hint','help'])  else x))
    df['Outcome'] = df["Outcome"].apply(lambda x: None if pd.isnull(x) else ('unknown' if x.lower() not in ['hint','correct', 'incorrect']  else x))


# In[81]:


#add new columns: prev_txn_time
if not pvExist:
    df = df.reindex(columns = df.columns.tolist() + [ 'Problem View', 'prev_txn_time'])
if not pstExist:
    df = df.reindex(columns = df.columns.tolist() + ['Problem Start Time', 'prev_txn_time'])
if pvExist and pstExist:
    df = df.reindex(columns = df.columns.tolist() + ['prev_txn_time'])


# In[82]:


#set prev_txn_time, should come before problem event
def row_func_prev_txn_time(row):
    prev_student = row['Student shifted']
    student = row['student_id']
    prev_time = row['Time shifted']
    if pd.isnull(prev_student) or pd.isnull(student) or pd.isnull(prev_time):
        return None
    elif prev_student != student:
        return None
    else:
        return prev_time
    
df['Time shifted'] = df['Time'].shift(1)
df['Student shifted'] = df['student_id'].shift(1)
df['prev_txn_time'] = df.apply(row_func_prev_txn_time, axis=1)
df.drop(['Time shifted'], axis=1, inplace=True)
df.drop(['Student shifted'], axis=1, inplace=True)


# In[85]:


#handle problem_event
uniqueProblemEventColumns = ['student_id', 'problem_id', 'Problem View', 'Problem Start Time']
uniqueProblemEventDF = uniqueColumnsDF(uniqueProblemEventColumns)
#don't need assigned index yet
uniqueProblemEventDF.drop(['index'], axis=1, inplace=True)
#make sure of order by student, problem, pv and pst
uniqueProblemEventDF = uniqueProblemEventDF.sort_values(by=uniqueProblemEventColumns)
#handle PV and PST empty values
#go through each row of uniqueProblemEventDF
for index, row in uniqueProblemEventDF.iterrows():
    student = row['student_id']
    problem = row['problem_id']
    pv = row['Problem View']
    pst = row['Problem Start Time']
    if pd.isnull(pst) and not pd.isnull(pv):
        #is there another record with same student, problem, pv and has pst, use it
        otherRows = uniqueProblemEventDF.loc[(uniqueProblemEventDF['student_id'] == student) 
                                             & (uniqueProblemEventDF['problem_id'] == problem) 
                                             & (uniqueProblemEventDF['Problem View'] == pv) 
                                             & (uniqueProblemEventDF['Problem Start Time'].notnull())]
        if not otherRows.empty:
            pst = min(otherRows['Problem Start Time'])
        else:
            #find the min of prev_txn_time and Time from df with this student, problem and pv for pst
            tempDF = df.loc[(df['student_id'] == student)
                                              & (df['problem_id'] == problem)
                                              & (df['Problem View'] == pv)]
            min_prev_txn_time = min(tempDF['prev_txn_time'])
            min_time = min(tempDF['Time'])
            if pd.isnull(min_prev_txn_time):
                pst = min_time
            else:
                pst = min([min_prev_txn_time, min_time])
        #update uniqueProblemEventDF and df with new pst
        uniqueProblemEventDF_row_ind = uniqueProblemEventDF[(uniqueProblemEventDF['student_id'] == student)
                                              & (uniqueProblemEventDF['problem_id'] == problem)
                                              & (uniqueProblemEventDF['Problem View'] == pv)
                                              & (uniqueProblemEventDF['Problem Start Time'].isnull())].index.tolist()
        uniqueProblemEventDF.loc[uniqueProblemEventDF_row_ind, 'Problem Start Time'] = pst
        df_row_ind = df[(df['student_id'] == student) 
                                              & (df['problem_id'] == problem) 
                                              & (df['Problem View'] == pv)
                                           & (df['Problem Start Time'].isnull())].index.tolist()
        df.loc[df_row_ind, 'Problem Start Time'] = pst
    elif pd.isnull(pst) and pd.isnull(pv):
        #set pv =1 
        #find the min of prev_txn_time and Time from df with this student, problem for pst
        tempDF = df.loc[(df['student_id'] == student)
                                              & (df['problem_id'] == problem)]
        min_prev_txn_time = min(tempDF['prev_txn_time'])
        min_time = min(tempDF['Time'])
        if pd.isnull(min_prev_txn_time):
            pst = min_time
        else:
            pst = min([min_prev_txn_time, min_time])
        pv = 1
        #update uniqueProblemEventDF and df with new pst
        uniqueProblemEventDF_row_ind = uniqueProblemEventDF[(uniqueProblemEventDF['student_id'] == student)
                                              & (uniqueProblemEventDF['problem_id'] == problem)
                                              & (uniqueProblemEventDF['Problem View'].isnull())
                                              & (uniqueProblemEventDF['Problem Start Time'].isnull())].index.tolist()
        uniqueProblemEventDF.loc[uniqueProblemEventDF_row_ind, 'Problem Start Time'] = pst
        uniqueProblemEventDF.loc[uniqueProblemEventDF_row_ind, 'Problem View'] = pv
        df_row_ind = df[(df['student_id'] == student) 
                                              & (df['problem_id'] == problem) 
                                              & (df['Problem View'].isnull())
                                           & (df['Problem Start Time'].isnull())].index.tolist()
        df.loc[df_row_ind, 'Problem Start Time'] = pst
        df.loc[df_row_ind, 'Problem View'] = pv
    elif pd.isnull(pv) and not pd.isnull(pst):
        #is there another record with same student, problem, pst and has pv, use it
        otherRows = uniqueProblemEventDF.loc[(uniqueProblemEventDF['student_id'] == student) 
                                             & (uniqueProblemEventDF['problem_id'] == problem) 
                                             & (uniqueProblemEventDF['Problem Start Time'] == pst) 
                                             & (uniqueProblemEventDF['Problem View'].notnull())]
        if not otherRows.empty:
            pv = min(otherRows['Problem View'])
        else:
            #find the pv from uniqueProblemEventDF with this student, problem and pst less 
            tempDF = uniqueProblemEventDF.loc[(uniqueProblemEventDF['student_id'] == student)
                                              & (uniqueProblemEventDF['problem_id'] == problem)
                                              & (uniqueProblemEventDF['Problem Start Time'] < pst)]
            if tempDF.empty:
                pv = 1
            else:
                pv = max(tempDF['Problem View']) + 1
            
        #update uniqueProblemEventDF and df with new pst
        uniqueProblemEventDF_row_ind = uniqueProblemEventDF[(uniqueProblemEventDF['student_id'] == student)
                                              & (uniqueProblemEventDF['problem_id'] == problem)
                                              & (uniqueProblemEventDF['Problem Start Time'] == pst)
                                              & (uniqueProblemEventDF['Problem View'].isnull())].index.tolist()
        uniqueProblemEventDF.loc[uniqueProblemEventDF_row_ind, 'Problem View'] = pv
        df_row_ind = df[(df['student_id'] == student) 
                                              & (df['problem_id'] == problem) 
                                              & (df['Problem Start Time'] == pst)
                                           & (df['Problem View'].isnull())].index.tolist()
        df.loc[df_row_ind, 'Problem View'] = pv

#drop duplicates introduced by problem-event handling empty pv and pst
uniqueProblemEventDF=uniqueProblemEventDF.drop_duplicates()
uniqueProblemEventDF.reset_index(inplace=True)
#bit excessive, don't really need reset index, but after this, index is in numeric order
uniqueProblemEventDF.drop(['index'], axis=1, inplace=True)
uniqueProblemEventDF.reset_index(inplace=True)
#merge with df
df = pd.merge(df, uniqueProblemEventDF, how='left', on=uniqueProblemEventColumns)
df.rename(columns={'index': 'problem_event_id'}, inplace=True)
#at this point problem start time, problem view, problem_event_id, prev_txn_time


# In[86]:


#populate 'attempt_at_subgoal' and 'is_last_attempt'

df['Attempt At Step'] = df.groupby(['student_id','problem_event_id','step_id']).cumcount()+1
#is_last_attempt is at step level not at problem_event level
df.loc[df.groupby(['student_id','step_id'])["Time"].idxmax(), 'Is Last Attempt']=1
df.loc[df['Is Last Attempt'].isnull(), 'Is Last Attempt'] = 0
#step the rows that have no step
df.loc[df['step_id'].isnull(), 'Attempt At Step'] = np.nan
df.loc[df['step_id'].isnull(), 'Is Last Attempt'] = np.nan


# In[87]:


#compute duration
#if prob_start_time is null and prev_tx_time is null, null
#problem_start_time will never be null
#if prob_start_time is null and prev_tx_time is not null and prev_tx_time and transaction_time is larger than 10 min, null
#if prob_start_time is null and prev_tx_time is not null and prev_tx_time and transaction_time is less than 10 min, use diff*1000
#if prev_tx_time is null, duration is set to null bc it's begining of a new student
#if prob_start_time is not null and prev_tx_time is not null, use the greatest one of the two to compute with transaction time, same logic with 10 min cut off
def row_func_duration(row):
    prev_txn_time = row['prev_txn_time']
    txn_time = row['Time']
    pst = row['Problem Start Time']
    if pd.isnull(prev_txn_time):
        return None
    elif pd.isnull(pst):
        delta = (int(pd.to_datetime(txn_time).value / 1000000) - int(pd.to_datetime(prev_txn_time).value / 1000000))/1000
        if delta < 0 or delta > 600:
            return None
        else:
            return delta
    else:
        delta1 = (int(pd.to_datetime(txn_time).value / 1000000) - int(pd.to_datetime(prev_txn_time).value / 1000000))/1000
        delta2 = (int(pd.to_datetime(txn_time).value / 1000000) - int(pd.to_datetime(pst).value / 1000000))/1000
        delta = None
        if (delta1 < 0 or delta1 > 600) and (delta2 >= 0 and delta2 <= 600):
            delta = delta2
        elif (delta1 >= 0 and delta1 <=600) and (delta2 < 0 or delta2 >600):
            delta = delta1
        elif (delta1 >= 0 and delta1 <=600) and (delta2 >= 0 and delta2 <= 600):
            delta = delta1 if delta1 < delta2 else delta2
        return delta
df['duration'] = df.apply(row_func_duration, axis=1)


# In[88]:


#handle identical txn with identical timestamp
df_identical_timestamps = df.groupby(['student_id', 'problem_id', 'Time']).size().reset_index(name='counts')
df_identical_timestamps = df_identical_timestamps[df_identical_timestamps['counts'] >1]
for index, row in df_identical_timestamps.iterrows():
    student = row['student_id']
    problem = row['problem_id']
    Time = row['Time']
    cnt = row['counts']
    df_rows_ind = df[(df['student_id'] == student) & (df['problem_id'] == problem) & (df['Time'] == Time)].index.tolist()
    df.loc[df_rows_ind, 'duration'] = sum(df.loc[df_rows_ind, 'duration'])/cnt
    #set all txn to the same is_last_attempt if there is one equals to 1
    df.loc[df_rows_ind, 'Is Last Attempt'] = max(df.loc[df_rows_ind, 'Is Last Attempt'])
    #set all txn to the same attempt_at_step with the min of all rows with these conditions
    #this will cause bug bc if there are other attempts that come after this txn time, the number is not right
    df.loc[df_rows_ind, 'Attempt At Step'] = min(df.loc[df_rows_ind, 'Attempt At Step'])
    


# In[89]:


if not convertToStudentStep:
    #change duration to Duration (sec)
    df['Duration (sec)'] = df['duration']
    #drop unnecessary columns
    df.drop(['student_id','problem_id','step_id','Problem Hierarchy', 'problem_event_id', 'prev_txn_time','duration'], axis=1, inplace=True)
    if 'Original Time' in df.columns:
        df['Time'] = df['Original Time']
        df.drop(['Original Time'], axis=1, inplace=True)
    if 'Original Problem Start Time' in df.columns:
        df['Problem Start Time'] = df['Original Problem Start Time']
        df.drop(['Original Problem Start Time'], axis=1, inplace=True)
    #change Original KC(model) to KC(model) and put them pack to the right place
    allColNames = df.columns
    for KCColName in KCColNames:
        df.rename(columns={'Original '+KCColName:KCColName}, inplace=True)
    
    allColNames = df.columns
    orderedColumnNames = []
    for colName in allColNames:
        if colName.find('KC Category (') == -1 and colName.find('KC (') == -1:
            if not pvExist:
                if colName == 'Problem Start Time':
                    orderedColumnNames.append('Problem View')
                    orderedColumnNames.append('Problem Start Time')
                elif colName != 'Problem View':
                    orderedColumnNames.append(colName)
            elif not pstExist:
                if colName == 'Problem View':
                    orderedColumnNames.append('Problem View')
                    orderedColumnNames.append('Problem Start Time')
                elif colName != 'Problem Start Time':
                    orderedColumnNames.append(colName)
            else:
                orderedColumnNames.append(colName)
        else:
            if colName.find('KC Category (') != -1:
                KCName = colName[len('KC Category (') : colName.find(')')]
                KCColName = colName.replace('KC Category (', 'KC (')
                orderedColumnNames.append(KCColName)
                orderedColumnNames.append(colName)
    df = df[orderedColumnNames]
    df.to_csv('transaction.txt', sep='\t', index=False)
    sys.exit()
    


# In[90]:


#make student_step roll up table with group by student_id, problem_id, step_id, problem_view
rollupColumns = ['student_id', 'problem_id', 'step_id', 'problem_event_id', 'Problem View']
# functions to get counts for hint, corrects and incorrects
def cnt_correct(rows):
    return(rows[rows == 'correct'].count())
def cnt_incorrect(rows):
    return(rows[rows == 'incorrect'].count())
def cnt_hint(rows):
    return(rows[rows == 'hint'].count())

#functions to get correct_tx_time: min(transacion_time) correct_flag=correct 
def func_correct_txn_time(rows):
    return rows[rows['Outcome'] == 'correct']['Time'].min()

if 'Outcome' not in df.columns:
    #get step_end_time, problem_event_time, first_transaction_time, corrects, incorrects, hints, step_duration, 
    df_rollup = df.groupby(rollupColumns).agg({'Problem Start Time':'min','Time':['min','max'], 'duration':'sum' }).reset_index()
    df_rollup.columns = [' '.join(col).strip() for col in df_rollup.columns.values]
    df_rollup.rename(columns={'Time min':'first_transaction_time', 'Time max':'step_end_time', 'Problem Start Time min':'problem_event_time', 'duration sum':'step_duration'}, inplace=True)
else:
    #drop rows that has None in Outcome, 
    df.dropna(axis=0, subset=['Outcome'])
    #get step_end_time, problem_event_time, first_transaction_time, corrects, incorrects, hints, step_duration, 
    df_rollup = df.groupby(rollupColumns).agg({'Problem Start Time':'min','Time':['min','max'], 'Outcome':[cnt_correct, cnt_incorrect, cnt_hint], 'duration':'sum' }).reset_index()
    df_rollup.columns = [' '.join(col).strip() for col in df_rollup.columns.values]
    df_rollup.rename(columns={'Time min':'first_transaction_time', 'Time max':'step_end_time', 'Problem Start Time min':'problem_event_time', 'Outcome cnt_correct':'corrects', 'Outcome cnt_incorrect':'incorrects', 'Outcome cnt_hint':'hints', 'duration sum':'step_duration'}, inplace=True)

    #set correct_txn_time
    df_rollup_2= df.groupby(rollupColumns).apply(func_correct_txn_time).reset_index()
    df_rollup_2.rename(columns={0: 'correct_transaction_time'}, inplace=True)
    df_rollup = pd.merge(df_rollup, df_rollup_2,  how='left', on=rollupColumns)


#get the rows that has the min transaction time, and use it for first_attempt, prev_txn_time and condition
#prev_txn_time, should come before problem event time 
min_time_rows = df.groupby(rollupColumns)["Time"].idxmin()
if 'Outcome' not in df.columns:
    if 'Condition' not in df.columns:
        min_time_attempt_prev_txn = df.loc[min_time_rows, ['student_id', 'problem_id', 'step_id', 'problem_event_id', 'Problem View', 'prev_txn_time']]
    else:
        min_time_attempt_prev_txn = df.loc[min_time_rows, ['student_id', 'problem_id', 'step_id', 'problem_event_id', 'Problem View', 'prev_txn_time', 'Condition']]
    df_rollup = pd.merge(df_rollup, min_time_attempt_prev_txn,  how='left', on=rollupColumns)
else:
    if 'Condition' not in df.columns:
        min_time_attempt_prev_txn = df.loc[min_time_rows, ['student_id', 'problem_id', 'step_id', 'problem_event_id', 'Problem View', 'Outcome', 'prev_txn_time']]
    else:
        min_time_attempt_prev_txn = df.loc[min_time_rows, ['student_id', 'problem_id', 'step_id', 'problem_event_id', 'Problem View', 'Outcome', 'prev_txn_time', 'Condition']]
    df_rollup = pd.merge(df_rollup, min_time_attempt_prev_txn,  how='left', on=rollupColumns)
    df_rollup.rename(columns={'Outcome':'first_attempt'}, inplace=True)


# In[91]:


#get the rows that has the min transaction time, and use it for event type
#prev_txn_time, should come before problem event time 
if 'Event Type' in df.columns:
    min_time_event_type_txn = df.loc[min_time_rows, ['student_id', 'problem_id', 'step_id', 'problem_event_id', 'Problem View', 'Event Type']]
    df_rollup = pd.merge(df_rollup, min_time_event_type_txn,  how='left', on=rollupColumns)


# In[92]:


#compute step_start_time
#three related times: problem_event_time, prev_txn_time, first_txn_time
#if only one of is not null, use it for step_start_time 
#if problem_event_time is null, set step_start_time to prev_tx_time (bc it should be earlier than first txn_time)
#if prev_tx_time is null, use min of problem_event_time and earliest_txn_time
#if prev_tx_time is later than problem_event_time, take prev_tx_time else use problem_event_time
#Calculate time difference between step_start_time and earliest_tx_time. If larger than 10 min, set step_start_time to null

def row_func_step_start_time(row):
    prev_txn_time = row['prev_txn_time']
    problem_event_time = row['problem_event_time']
    first_txn_time = row['first_transaction_time']
    step_start_time = None
    if pd.notnull(problem_event_time) and pd.isnull(prev_txn_time) and pd.isnull(first_txn_time):
        step_start_time = problem_event_time
    elif pd.isnull(problem_event_time) and pd.notnull(prev_txn_time) and pd.isnull(first_txn_time):
        step_start_time = prev_txn_time
    elif pd.isnull(problem_event_time) and pd.isnull(prev_txn_time) and pd.notnull(first_txn_time):
        step_start_time = first_txn_time
    elif pd.isnull(problem_event_time) and pd.notnull(prev_txn_time) and pd.notnull(first_txn_time):
        step_start_time = prev_txn_time
    elif pd.notnull(problem_event_time) and pd.isnull(prev_txn_time) and pd.notnull(first_txn_time):
        step_start_time = min([problem_event_time, first_txn_time])
    elif pd.notnull(problem_event_time) and pd.notnull(prev_txn_time) and pd.isnull(first_txn_time):
        step_start_time = max([problem_event_time, prev_txn_time])
    elif pd.notnull(problem_event_time) and pd.notnull(prev_txn_time) and pd.notnull(first_txn_time):
        step_start_time = max([problem_event_time, prev_txn_time])
    
    delta = (int(pd.to_datetime(step_start_time).value / 1000000) - int(pd.to_datetime(first_txn_time).value / 1000000))/1000
    if delta > 600:
        step_start_time = None
    return step_start_time

df_rollup['step_start_time'] = df_rollup.apply(row_func_step_start_time, axis=1)


# In[93]:


#calculate correct_step_duration and error_step_duration and modif step_duration
#set step_duration to null if step_start_time is null; 
#set correct_step_duration to step_duration if first_attempt is correct; set error_step_duration to step_duration if first_attempt is not correct
def row_func_step_duration(row):
    if pd.isnull(row['step_start_time']):
        return None
    else:
        return row['step_duration']
def row_func_correct_step_duration(row):
    if row['first_attempt'] == 'correct':
        return row['step_duration']
    else:
        return None

def row_func_error_step_duration(row):
    if row['first_attempt'] != 'correct':
        return row['step_duration']
    else:
        return None
        
df_rollup['step_duration'] = df_rollup.apply(row_func_step_duration, axis=1)
if 'Outcome' in df.columns:
    df_rollup['correct_step_duration'] = df_rollup.apply(row_func_correct_step_duration, axis=1)
    df_rollup['error_step_duration'] = df_rollup.apply(row_func_error_step_duration, axis=1)


# In[94]:


#before compute KC and opportunitie, reorder by student, first_transaction_time, step_start_time
df_rollup = df_rollup.sort_values(by=['student_id', 'first_transaction_time', 'step_start_time', 'problem_id', 'Problem View'])


# In[95]:


def sameRow(row1, row2):
    if row1 is not None:
        row1 = list(row1)
    if row2 is not None:
        row2 = list(row2)
    if row1 is None and row2 is None:
        return True
    elif row1 is None and row2 is not None:
        return False
    elif row1 is not None and row2 is None:
        return False
    elif len(row1) != len(row2):
        return False
    else:
        for i in range(len(row1)):
            if row1[i] is None and row2[i] is None:
                continue
            elif pd.isnull(row1[i]) and pd.isnull(row2[i]):
                continue
            elif pd.isna(row1[i]) and pd.isna(row2[i]):
                continue
            elif row1[i] != row2[i]:
                return False
        return True
                
        
    
#compute KC skills and opportunity for each row. Use skill-student map to keep the highest opportunity count
#def row_func_kc_opp(step_id, student_id, modelName, skillsSubDF):
def row_func_kc_opp(row, modelName, skillsSubDF):
    global lastRow
    global lastStudentId
    global studentSkillDic
    global studentLastEventTypeDic
    global df_rollup_cnt
    global df_rollup_pct
    step_id = row['step_id']
    student_id = row['student_id']
    event_type = ""
    if 'Event Type' in row.keys() and not pd.isnull(row['Event Type']):
        event_type = row['Event Type']
    
    
    totalCnt = len(allModels) * len(df_rollup)
    #get all skills for this step
    skills = skillsSubDF[skillsSubDF['step_id']==step_id][['skill', 'skill_id']]
    skills = skills[pd.notnull(skills['skill'])]
    #exclude skill that is empty
    skills = skills[skills['skill']!='']
    skills = skills.sort_values(by='skill')
    
    
    skillsStr = '~~'.join(skills['skill'].astype(str))
    #because df_rollup isorded by student, we can refresh map for a new student
    if lastStudentId != student_id:
        studentSkillDic = {}
        studentLastEventTypeDic = {}
    lastStudentId = student_id
    
    opportunitiesStr = ''
    for skill in skills['skill']:
        oppForSkill = 1
        if not sameRow(lastRow, row) and skill in studentSkillDic and skill in studentLastEventTypeDic:
            #decide if the opp should be incremented
            last_row_event_type = studentLastEventTypeDic[skill]
            if last_row_event_type != "" and 'instruct' not in last_row_event_type:
                oppForSkill = studentSkillDic[skill]
            else: 
                oppForSkill = studentSkillDic[skill] + 1
        studentSkillDic[skill] = oppForSkill
        studentLastEventTypeDic[skill] = event_type
        if opportunitiesStr == '':
            opportunitiesStr = str(oppForSkill)
        else:
            opportunitiesStr = opportunitiesStr + "~~" + str(oppForSkill)
    lastRow = copy.copy(row)
    df_rollup_cnt = df_rollup_cnt + 1
    if df_rollup_cnt/totalCnt > df_rollup_pct:
        logProgressToWfl("{:.0%}".format(df_rollup_pct))
        df_rollup_pct = df_rollup_pct + 0.1
    return pd.Series((skillsStr, opportunitiesStr, ""))

uniqueSkillStepDF["skill_id"] = pd.to_numeric(uniqueSkillStepDF["skill_id"])
joinSkillStep = pd.merge(uniqueSkillStepDF, uniqueSkillDF,  how='left', left_on='skill_id', right_on='index')

allModels = uniqueSkillDF['model'].drop_duplicates().sort_values()
allKCRelatedColumns = []
#lastStudentId = -1
#studentSkillDic = {}
#opportunitiesStr = ''
df_rollup_cnt = 0
df_rollup_pct = 0.1
lastRow = None
for model in allModels:
    lastStudentId = -1
    studentSkillDic = {}
    studentLastEventTypeDic = {}
    opportunitiesStr = ''
    newKCColumn = 'KC (' + model + ')'
    newOppColumn = 'Opportunity (' + model + ')'
    newPredErrColumn = 'Predicted Error Rate (' + model + ')'
    allKCRelatedColumns.append(newKCColumn)
    allKCRelatedColumns.append(newOppColumn)
    allKCRelatedColumns.append(newPredErrColumn)
    df_rollup = df_rollup.reindex(columns = df_rollup.columns.tolist() + [newKCColumn, newOppColumn])
    skillsSubDF = joinSkillStep[joinSkillStep['model']==model][['step_id', 'skill', 'skill_id']]
    #df_rollup[[newKCColumn, newOppColumn]] = df_rollup.apply(lambda row: row_func_kc_opp(row['step_id'], row['student_id'], model, skillsSubDF), axis=1)
    df_rollup[[newKCColumn, newOppColumn, newPredErrColumn]] = df_rollup.apply(row_func_kc_opp, args = (model, skillsSubDF,), axis=1)


# In[96]:


#put student, school, class back
df_rollup = pd.merge(df_rollup, studentUniqueDF,  how='left', left_on="student_id", right_on="index")
df_rollup.drop(['index','student_id'], axis=1, inplace=True)
#put problem hierarch and problem name back
df_rollup = pd.merge(df_rollup, problemUniqueDF,  how='left', left_on="problem_id", right_on="index")
df_rollup.drop(['index','problem_id'], axis=1, inplace=True)
#put step back
df_rollup = pd.merge(df_rollup, stepUniqueDF,  how='left', left_on="step_id", right_on="index")
df_rollup.drop(['index','problem_id', 'step_id'], axis=1, inplace=True)
#drop problem_event_id
df_rollup.drop(['problem_event_id', 'problem_event_time'], axis=1, inplace=True)

#reorder columns
orderedColumnNames = None
if 'Outcome' not in df.columns:
    orderedColumnNames = ['Anon Student Id', 'Problem Hierarchy', 'Problem Name', 'Problem View',  'Step Name', 'step_start_time', 'first_transaction_time', 'step_end_time', 'step_duration']
else:
    orderedColumnNames = ['Anon Student Id', 'Problem Hierarchy', 'Problem Name', 'Problem View',  'Step Name', 'step_start_time', 'first_transaction_time', 'correct_transaction_time', 'step_end_time', 'step_duration', 'correct_step_duration', 'error_step_duration', 'first_attempt', 'incorrects', 'hints', 'corrects']

if 'Condition Name' in originalAllColNames:
    orderedColumnNames = orderedColumnNames + ['Condition']
if 'School' in originalAllColNames:
    orderedColumnNames = orderedColumnNames + ['School']
if 'Class' in originalAllColNames:
    orderedColumnNames = orderedColumnNames + ['Class']
df_rollup = df_rollup[orderedColumnNames + allKCRelatedColumns]
if 'Outcome' not in df.columns:
    df_rollup.rename(columns={'step_start_time':'Step Start Time', 'first_transaction_time':'First Transaction Time', 'step_end_time':'Step End Time', 'step_duration':'Step Duration (sec)'}, inplace=True)
else:
    df_rollup.rename(columns={'step_start_time':'Step Start Time', 'first_transaction_time':'First Transaction Time', 'correct_transaction_time':'Correct Transaction Time', 'step_end_time':'Step End Time', 'step_duration':'Step Duration (sec)', 'correct_step_duration':'Correct Step Duration (sec)', 'error_step_duration':'Error Step Duration (sec)', 'first_attempt':'First Attempt', 'incorrects':'Incorrects', 'hints':'Hints', 'corrects':'Corrects'}, inplace=True)
        
#close all output files and finish!!
df_rollup.to_csv('studentStepRollup.txt', sep='\t', index=False)


# In[ ]:




