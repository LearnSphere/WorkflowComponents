"""
Helper functions for interfacing with SQL databases
"""
import MySQLdb
import MySQLdb.cursors as cursors
import getpass
import shutil
import os
from subprocess import Popen, PIPE
import re
import sqlparse
import multiprocessing
from logger import Logger
import time

class TimeoutException(Exception):
    pass

def openSQLConnection(databaseName, userName, host, port):
    return MySQLdb.connect(host=host, port=port,
                           user=userName, passwd=getpass.getpass(),
                           db=databaseName, cursorclass=cursors.SSCursor)
def openSQLConnectionP(databaseName, userName,passwd, host, port):
    return MySQLdb.connect(host=host, port=port,
                           user=userName, passwd=passwd, db=databaseName,
                           cursorclass=cursors.SSCursor)

def closeSQLConnection(connection):
    connection.close()

    
def executeSQL(connection,command,parent_conn = None, logger=None):
    ''' command is a sequence of SQL commands
        separated by ";" and possibly "\n"
        connection is a MySQLdb connection
        returns the output from the last command
        in the sequence
    '''
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    #split commands by \n
    commands = command.split("\n")
    #remove comments and whitespace"
    commands = [x for x in commands if x.lstrip()[0:2] != '--']
    commands = [re.sub('\r','',x) for x in commands if x.lstrip() != '\r']
    command = ' '.join(commands)

    statements = sqlparse.split(command)
    count = 0
    for statement in statements:
        cur = connection.cursor()
        #make sure actually does something
        if sqlparse.parse(statement):
            cur.execute(statement)
        cur.close()
    connection.commit()
    if parent_conn:
        parent_conn.send(True)
    return True

def executeSQLWithConnParams(userName,passwd, host, port,databaseName,command,parent_conn = None, logger=None):
    ''' Used by windows platform whcih doesn't allow connection object passed bw processes
        command is a sequence of SQL commands
        separated by ";" and possibly "\n"
        userName, passwd, host and port are used to establish db connecion
        returns the output from the last command
        in the sequence
    '''
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    connection = MySQLdb.connect(host=host, port=port,
                           user=userName, passwd=passwd, db=databaseName,
                           cursorclass=cursors.SSCursor)

    #split commands by \n
    commands = command.split("\n")
    #remove comments and whitespace"
    commands = [x for x in commands if x.lstrip()[0:2] != '--']
    commands = [re.sub('\r','',x) for x in commands if x.lstrip() != '\r']
    command = ' '.join(commands)

    statements = sqlparse.split(command)
    count = 0
    for statement in statements:
        cur = connection.cursor()
        #make sure actually does something
        if sqlparse.parse(statement):
            cur.execute(statement)
        cur.close()
    connection.commit()
    connection.close()
    if parent_conn:
        parent_conn.send(True)
    return True

def executeSQLTimeout(connection,command, timeout,
                      host=None, port=None, userName=None, passwd=None, dbName=None,
                      logger=None):
    ''' command is a sequence of SQL commands
        separated by ";" and possibly "\n"
        connection is a MySQLdb connection
        on windows platform, connection object is not passed. Instead, connection args are passed
        returns the output from the last command
        in the sequence
    '''
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    conn1_rcv, conn2_send = multiprocessing.Pipe(False)
    if connection is None:
        #log.log("calling multiprocessing subprocess on windows platform")
        subproc = multiprocessing.Process(target=executeSQLWithConnParams,
                                          args=(userName,passwd, host, port,dbName,command,conn2_send))
    else:
        #log.log("calling multiprocessing subprocess on non-windows platform")
        subproc = multiprocessing.Process(target=executeSQL,args=(connection,command, conn2_send))
    subproc.start()
    subproc.join(timeout)
    if conn1_rcv.poll():
        return conn1_rcv.recv()
    subproc.terminate()
    raise TimeoutException("Query ran for > %s seconds" % (timeout))

def block_sql_command(conn, cursor, command, data, block_size):
    last_block = False
    current_offset = 0
    while last_block == False:
        if current_offset + block_size < len(data):
            block = data[current_offset:current_offset+block_size]
        else:
            block = data[current_offset:]
            last_block = True
        if block:
            cursor.executemany(command, block)
            conn.commit()
            current_offset += block_size

def replaceWordsInFile(fileName,toBeReplaced, replaceBy, logger=None):
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    # toBeReplaced and replaceBy must be two string lists of same size
    txt = open(fileName, 'r').read()
    if len(toBeReplaced)!=len(replaceBy):
        log.log('CAREFUL: sizes must be the same')
        return
    else:
        for i in range(0,len(toBeReplaced)):
            txt=replaceWordInString(txt,toBeReplaced[i], replaceBy[i])
    return txt

def replaceWordInString(txt,toBeReplaced, replaceBy):
    (newTxt, instances) = re.subn(re.escape(toBeReplaced), replaceBy, txt)
    #print("Number of instances changed in the text = ", instances)
    return newTxt

def createAndEnterTmpDirectory():
    cwd = os.getcwd()
    if not os.path.exists(cwd + "/tmp"):
        os.makedirs(cwd+ "/tmp")
    else:
        shutil.rmtree(cwd+'/tmp')
        os.makedirs(cwd+ "/tmp")
    os.chdir(os.getcwd()+"/tmp")

def createTmpSqlScriptFromText(fileName, fileContents):
    fd = open(fileName + ".sql", 'w')
    fd.write(fileContents)
    fd.close()

def removeTmpDirectoryFromTmpDirectory():
    os.chdir("..");
    shutil.rmtree(os.getcwd()+'/tmp')

def findFeatureExtractionFiles():
    sqlFilesFile = open('feat_extract_sql/sql_files.txt','r')
    sqlFiles = [x.translate(None,'\n') for x in sqlFilesFile if len(x.translate(None,'\n'))>0]
    pythonFilesFile = open('feat_extract_sql/python_files.txt','r')
    pythonFiles = [x.translate(None,'\n') for x in pythonFilesFile if len(x.translate(None,'\n'))>0]
    return [sqlFiles, pythonFiles]

def runSQLFile(conn, fileName, dbName, toBeReplaced, toReplace, timeout,
               host=None, port=None, userName=None, passwd=None, logger=None):
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    commands = replaceWordsInFile(fileName,toBeReplaced, toReplace)
    try:
        executeSQLTimeout(connection=conn,command=commands, timeout=timeout, dbName=dbName,
                          host=host, port=port, userName=userName, passwd=passwd)            
        log.log(fileName + "script run successfully")
        return True
    except (RuntimeError, TypeError, NameError) as e:
        log.log(str(e))
        log.log("not able to run: " + fileName)
        return False
    except TimeoutException as e:
        log.log(str(e))
        return False
    except:
        log.log("unknown error")
        log.log("not able to run: " + fileName)
        return False

def runPythonFile(conn, conn2, module, fileName, dbName, startDate,
                currentDate, numWeeks, featureExtractionId,
                timeout = 100000, host=None, port=None, userName=None, passwd=None, logger=None):
    #use default logger if logger is not provided
    log = Logger(logToConsole=True, logFilePath=None)
    if logger is not None:
        log = logger
        
    module = module.replace("/",".") # Edit
    # try:
    imported = getattr(__import__(module, fromlist=[fileName]), fileName)
    # except:
    #     top_level = 'feature_extraction'
    #     imported = getattr(__import__(top_level+'.'+module,fromlist=[fileName]), fileName)
    begin = time.time()
    try:
        conn1_rcv, conn2_send = multiprocessing.Pipe(False)
        if conn is None:
            subproc = multiprocessing.Process(target=imported.main_windows,args=(host, port, userName, passwd, dbName,
                                                                                 startDate, currentDate, numWeeks, featureExtractionId, conn2_send))
        else:
            subproc = multiprocessing.Process(target=imported.main,args=(conn, conn2, dbName, startDate, 
                                        currentDate,numWeeks, featureExtractionId, conn2_send))
        subproc.start()
        subproc.join(timeout)
        if conn1_rcv.poll():
            return conn1_rcv.recv()
        subproc.terminate()
        end = time.time()
        if (end-begin)>=timeout:
            raise TimeoutException("Query ran for > %s seconds" % (timeout))
        else:
            return True

    except (RuntimeError, TypeError, NameError) as e:
        log.log(str(e))
        log.log("not able to run: " + fileName)
        return False
    except TimeoutException as e:
        log.log(str(e))
        return False
    except:
        log.log("unknown error")
        log.log("not able to run: " + fileName)
        return False
    else:
        log.log( fileName + "script run successfully")
        return True



def return_listOfOrderedScripts(OrderFileName,StringStopSequence):
    L=list()
    txt = open(OrderFileName, 'r').read()
    cursor=0
    index=0
    current_focus=txt[0:4]
    print current_focus
    while cursor<len(txt)-4:
        if current_focus!=StringStopSequence:
            index+=1
            current_focus=txt[index:index+4]
        else:
            L.append(txt[cursor:index])
            cursor=index+4
            index+=4
            current_focus=txt[index:index+4]
    return L


def extract_NumberEnrollments(Name_database):
    txt='Select count(distinct user_id) from observed_events'
    c=executeAndReturn_cursor2(Name_database,txt)
    return c.fetchall()[0][0]

def extract_NumberResources(Name_database):
    txt='Select count(distinct resource_id) from resources'
    c=executeAndReturn_cursor2(Name_database,txt)
    txt="Select count(distinct resource_id) from resources where resource_type_id='0' "
    d=executeAndReturn_cursor2(Name_database,txt)
    return [c.fetchall()[0][0],d.fetchall()[0][0]]

def extract_NumberProblems(Name_database):
    txt='Select count(*) from problems'
    c=executeAndReturn_cursor2(Name_database,txt)
    txt="Select count(*) from problems where problem_type_id='0' "
    d=executeAndReturn_cursor2(Name_database,txt)
    return [c.fetchall()[0][0],d.fetchall()[0][0]]

def extract_NumberObservedEvents(Name_database):
    txt='Select count(distinct observed_event_id) from observed_events'
    c=executeAndReturn_cursor2(Name_database,txt)
    return c.fetchall()[0][0]

def extract_NumberCollaboration(Name_database):
    txt='Select count(distinct collaboration_id) from collaborations'
    c=executeAndReturn_cursor2(Name_database,txt)
    return c.fetchall()[0][0]

def extract_NumberSubmission(Name_database):
    txt='Select count(distinct submission_id) from submissions'
    c=executeAndReturn_cursor2(Name_database,txt)
    return c.fetchall()[0][0]

def extract_MainInfo(Name_database):
    file=open("Global report on "+Name_database,"w")
    file.write("General report about course : "+Name_database)
    print "Number of enrollments = ",extract_NumberEnrollments(Name_database)
    res=extract_NumberResources(Name_database)
    print "Number of resources = ",res[0]," of which ",int(100*res[1]/res[0]),"% do not have type"
    pb=extract_NumberProblems(Name_database)
    print "Number of problems = ",pb[0]," of which ",int(100*pb[1]/pb[0]),"% do not have type"
    print "Number of observed events = ",extract_NumberObservedEvents(Name_database)
    print "Number of collaborations = ",extract_NumberCollaboration(Name_database)
    print "Number of submissions = ",extract_NumberSubmission(Name_database)
    file.close()



def RatioOfUsers_CountryRanking():  #SCRIPTS EXAMPLE
    cur=executeAndReturn_cursor2('moocdb',"select * from countries")
    ratio=list()
    country_names=list()
    list_country=cur.fetchall()

    for i in range(0,len(list_country)):
        if isinstance(list_country[i][2], int):
            ratio.append(list_country[i][2]/list_country[i][1])
            country_names.append(list_country[i][0])
    SortedRatioIndex=np.argsort(-np.array(ratio))
    ratio=[100*ratio[x] for x in SortedRatioIndex]
    country_names=[country_names[x] for x in SortedRatioIndex]
    print ratio
    print country_names


if __name__ == '__main__':
    tempCommand()
