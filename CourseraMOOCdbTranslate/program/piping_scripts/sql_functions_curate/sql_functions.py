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
from utilities import logger

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

def executeSQL(vars, connection,command,parent_conn=None):
    ''' command is a sequence of SQL commands
        separated by ";" and possibly "\n"
        connection is a MySQLdb connection
        returns the output from the last command
        in the sequence
    '''
    
    
        
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
            #vars['logger'].Log(vars, "executing SQL statement")
            cur.execute(statement)
        cur.close()
    connection.commit()
    if parent_conn:
        parent_conn.send(True)
    return True

def executeSQLTimeout(vars,connection,command, timeout):
    ''' command is a sequence of SQL commands
        separated by ";" and possibly "\n"
        connection is a MySQLdb connection
        returns the output from the last command
        in the sequence
    '''
      
    conn1_rcv, conn2_send = multiprocessing.Pipe(False)
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
            data_str = str(block)[1:-1]
            grounded_command = command % (data_str)
            cursor.execute(grounded_command)
            conn.commit()
            current_offset += block_size

def replaceWordsInFile(vars, fileName,toBeReplaced, replaceBy):
       
    # toBeReplaced and replaceBy must be two string lists of same size
    txt = open(fileName, 'r').read()
    if len(toBeReplaced)!=len(replaceBy):
        vars['logger'].Log(vars, 'CAREFUL: sizes must be the same')
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

def runSQLFile(vars, conn, fileName, dbName, toBeReplaced, toReplace, timeout):
       
    commands = replaceWordsInFile(fileName,toBeReplaced, toReplace)
    try:
        executeSQLTimeout(conn,commands, timeout)
        vars['logger'].Log(vars, fileName + "script run successfully")
        return True
    except (RuntimeError, TypeError, NameError) as e:
        print e
        vars['logger'].Log(vars, "not able to run: " + fileName)
        return False
    except TimeoutException as e:
        print e
        return False
    except:
        vars['logger'].Log(vars, "unknown error")
        vars['logger'].Log(vars, "not able to run: " + fileName)
        return False

def runPythonFile(vars,conn, conn2, module, fileName, dbName, startDate,
        currentDate, timeout = 100000):
        
    imported = getattr(__import__(module, fromlist=[fileName]), fileName)
    try:
        conn1_rcv, conn2_send = multiprocessing.Pipe(False)
        subproc = multiprocessing.Process(target=imported.main,args=(conn,
                                        conn2, dbName, startDate, currentDate,conn2_send))
        subproc.start()
        subproc.join(timeout)
        if conn1_rcv.poll():
            return conn1_rcv.recv()
        subproc.terminate()
        raise TimeoutException("Query ran for > %s seconds" % (timeout))

    except (RuntimeError, TypeError, NameError) as e:
        print e
        vars['logger'].Log(vars,"not able to run: " + fileName)
        return False
    except TimeoutException as e:
        print e
        return False
    except:
        vars['logger'].Log(vars,"unknown error")
        vars['logger'].Log(vars,"not able to run: " + fileName)
        return False
    else:
        vars['logger'].Log(vars,fileName + "script run successfully")
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



