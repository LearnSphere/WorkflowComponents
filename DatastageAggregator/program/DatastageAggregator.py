import csv
import numpy as np
import pandas as pd
import gc
from datetime import datetime
import re
import argparse


def newAggrDataRow():
    aggrDataRow = {"page_view_count" : 0,
                "distinct_page_view_count": 0,
                "problem_action_count" : 0,
                "distinct_problem_action_count" : 0,
                "video_play_count" : 0,
                "distinct_video_play_count" : 0,
                "forum_session_count": 0,
                "forum_HL_participation_count": 0,
                "distinct_forum_HL_participation_count": 0,
                "forum_LL_participation_count": 0,
                "distinct_forum_LL_participation_count": 0,
                "performance_grade": 0}
    return aggrDataRow




def parseEventType(eventType):
    event = ""
    value = ""
    eventTypeElements = eventType.split('/')
    #for page reading, the last character should be /
    if eventTypeElements[len(eventTypeElements)-1].strip() == "":
        event = readEvent
        value = eventType
    #for action,
    elif eventTypeElements[len(eventTypeElements)-1].strip() == "problem_get":
        #get problemId
        for ele in eventTypeElements:
            if "type@problem" in ele:
                value = ele
        event = actionEvent
    #for instructor
    elif "instructor" in eventTypeElements:
        event = instructorEvent
    elif "discussion" in eventTypeElements:
        if eventTypeElements[len(eventTypeElements)-1].strip() in forum_HL_Participations:
            event = forumHLParticipationEvent
        elif eventTypeElements[len(eventTypeElements)-1].strip() in forum_LL_Participations:
            event = forumLLParticipationEvent
        else:
            event = forumOtherEvent
        if event == forumHLParticipationEvent or event == forumLLParticipationEvent:
            for i in range(0, len(eventTypeElements)):
                if eventTypeElements[i] in ["comments", "threads"] and len(eventTypeElements) > (i+1):
                    value = value + eventTypeElements[i+1]
                    break
        
    return [event, value]




def addToPageView(studentId, eventType):
    #add to studentAggData
    studentAggData[studentId]["page_view_count"] = studentAggData[studentId]["page_view_count"] + 1
    #treat unique count
    if eventType not in uniquePages:
        uniquePages.append(eventType)
    #see if student has already seen this page
    ind = uniquePages.index(eventType)
    if studentId not in studentUniquePages.keys():
        studentUniquePages[studentId] = set()
    studentUniquePages[studentId].add(ind)
    return "page add 1"




def addToAction(studentId, problemId):
    #add to studentAggData
    studentAggData[studentId]["problem_action_count"] = studentAggData[studentId]["problem_action_count"] + 1
    #treat unique count
    if problemId not in uniqueProblems:
        uniqueProblems.append(problemId)
    #see if student has already seen this problem
    ind = uniqueProblems.index(problemId)
    if studentId not in studentUniqueProblems.keys():
        studentUniqueProblems[studentId] = set()
    studentUniqueProblems[studentId].add(ind)
    return "action add 1"



def addToVideo(studentId, videoId):
    #add to studentAggData
    studentAggData[studentId]["video_play_count"] = studentAggData[studentId]["video_play_count"] + 1
    #treat unique count
    if videoId not in uniqueVideos:
        uniqueVideos.append(videoId)
    #see if student has already seen this video
    ind = uniqueVideos.index(videoId)
    if studentId not in studentUniqueVideos.keys():
        studentUniqueVideos[studentId] = set()
    studentUniqueVideos[studentId].add(ind)
    return "video add 1"




def addToInstructor(studentId):
    #add to instructor set and delete from studentAggdata
    instructors.add(studentId)
    if studentId in studentAggData.keys():
        del studentAggData[studentId]
    return "skip instructor"




def addToForumSession(studentId, eventTime):
    returnVal = ""
    #add to forum_session_count only when longer than half hour after the last forum session
    if differentForumSession(studentId, eventTime) == True:
        studentAggData[studentId]["forum_session_count"] = studentAggData[studentId]["forum_session_count"] + 1
        returnVal = "forum session add 1"
        #reset because this is a new forum session
        lastSavedForum[studentId] = {"last_forum_time":None, "HL_threads":set(), "LL_threads":set()}
    lastSavedForum[studentId]["last_forum_time"] = eventTime
    return returnVal




def addToForumLLParticipation(studentId, forumId, eventTime):
    returnVal = addToForumSession(studentId, eventTime)
    #add to studentAggData if student has done anything for this thread
    if forumId not in lastSavedForum[studentId]["LL_threads"]:
        studentAggData[studentId]["forum_LL_participation_count"] = studentAggData[studentId]["forum_LL_participation_count"] + 1
        lastSavedForum[studentId]["LL_threads"].add(forumId)
        if returnVal == "":
            returnVal = "forum LL participation add 1"
        else:
            returnVal = returnVal + ", forum LL participation add 1"
    #treat unique count
    if forumId not in uniqueForums:
        uniqueForums.append(forumId)
    #see if student has already seen this forum
    ind = uniqueForums.index(forumId)
    if studentId not in studentUniqueForumLLParticipations.keys():
        studentUniqueForumLLParticipations[studentId] = set()
    studentUniqueForumLLParticipations[studentId].add(ind)
    return returnVal





def addToForumHLParticipation(studentId, forumId, eventTime):
    returnVal = addToForumSession(studentId, eventTime)
    #add to studentAggData if student has done anything for this thread
    if forumId not in lastSavedForum[studentId]["HL_threads"]:
        studentAggData[studentId]["forum_HL_participation_count"] = studentAggData[studentId]["forum_HL_participation_count"] + 1
        lastSavedForum[studentId]["HL_threads"].add(forumId)
        if returnVal == "":
            returnVal = "forum HL participation add 1"
        else:
            returnVal = returnVal + ", forum HL participation add 1"
    #treat unique count
    if forumId not in uniqueForums:
        uniqueForums.append(forumId)
    #see if student has already seen this forum
    ind = uniqueForums.index(forumId)
    if studentId not in studentUniqueForumHLParticipations.keys():
        studentUniqueForumHLParticipations[studentId] = set()
    studentUniqueForumHLParticipations[studentId].add(ind)
    return returnVal





def shorterThanHalfHour(time1, time2) :
    #if the time difference is longer than 30 min, return false, else true
    time1 = datetime.strptime(time1, "%Y-%m-%d %H:%M:%S.%f")
    time2 = datetime.strptime(time2, "%Y-%m-%d %H:%M:%S.%f")
    diff = None
    if time1 > time2:
        diff = time1 - time2
    else:
        diff = time2 - time1
    if abs(diff.seconds/60) <= 30:
        return True
    else:
        return False  





def differentVideoSession(studentId, videoId, eventType, time):
    #a play-video should be counted if this student's last saved video is a different or it's half hour ago
    returnVal = True
    if studentId not in lastSavedVideo.keys():
        lastSavedVideo[studentId] = {"id":"", "vtime":None, "event": None}
    if (lastSavedVideo[studentId]["id"] != videoId or lastSavedVideo[studentId]["vtime"] is None or
        shorterThanHalfHour(time, lastSavedVideo[studentId]["vtime"]) == False or
        lastSavedVideo[studentId]["event"] == "load_video"):
        returnVal = True
        lastSavedVideo[studentId]["id"] = videoId
    else:
        returnVal = False
    lastSavedVideo[studentId]["vtime"] =time
    lastSavedVideo[studentId]["event"] =eventType
    return returnVal
    





def differentForumSession (studentId, forumTime) :
    #a new forum session starts when the last forum is at least half hour ago 
    returnVal = True
    if studentId not in lastSavedForum.keys():
        lastSavedForum[studentId] = {"last_forum_time":None, "HL_threads":set(), "LL_threads":set()}
    if (lastSavedForum[studentId]["last_forum_time"] is None or
        shorterThanHalfHour(forumTime, lastSavedForum[studentId]["last_forum_time"]) == False):
        returnVal = True
    else:
        returnVal = False
    return returnVal




# command-line call: python DatastageAggregator.py
# -eventXtractFile "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_EventXtract.csv"
# -homeworkFile "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_ActivityGrade.csv"
# -videoInteractionFile "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_VideoInteraction.csv"
# -performanceFile "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_Performance.csv"
if __name__ == "__main__":
    #input files
    #eventXtractFile = "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_EventXtract_test.csv"
    #homeworkFile = "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_ActivityGrade_test.csv"
    #videoInteractionFile = "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_VideoInteraction_test.csv"
    parser = argparse.ArgumentParser(description='Datastage doer effect aggregator.')
    parser.add_argument('-eventXtractFile', type=str, help='EventXtract data location', default="")
    parser.add_argument('-homeworkFile', type=str, help='Homework data location', default="")
    parser.add_argument('-videoInteractionFile', type=str, help='VideoInteraction data location', default="")
    parser.add_argument('-performanceFile', type=str, help='Performance data location', default="")
    args, unknown = parser.parse_known_args()
 
    eventXtractFile = args.eventXtractFile
    homeworkFile = args.homeworkFile
    videoInteractionFile = args.videoInteractionFile
    performanceFile = args.performanceFile
    
    debug = False
    debugEventXtractOutputFile = None
    debugHomeworkOutputFile = None
    debugVideoInteractionOutputFile = None
    debugEventXtractOutputFileName = "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_EventXtract_test_QA.csv"
    debugHomeworkOutputFileName = "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_ActivityGrade_test_QA.csv"
    debugVideoInteractionOutputFileName = "course-v1_OLI_UCLA+StatReasoning+Stigler_PSYC100A_001_Winter2017_VideoInteraction_test_QA.csv"
    if debug:
        debugEventXtractOutputFile = open(debugEventXtractOutputFileName, 'w')
        debugHomeworkOutputFile = open(debugHomeworkOutputFileName, 'w')
        debugVideoInteractionOutputFile = open(debugVideoInteractionOutputFileName, 'w')

    #a few constant variables:
    readEvent = "read"
    actionEvent = "action"
    videoEvent = "video"
    forumLLParticipationEvent = "forum_LL_participation"
    forumHLParticipationEvent = "forum_HL_participation"
    forumOtherEvent = "forum_other"
    instructorEvent = "instructor"

    forum_HL_Participations = ["upload", "create", "reply"]
    forum_LL_Participations = ["endorse", "upvote", "unvote", "update", "follow", "search"]

    # student aggregated data is dictionary with student id is key
    studentAggData = {}
    studentUniquePages = {}
    studentUniqueProblems = {}
    studentUniqueVideos = {}
    studentUniqueForumHLParticipations = {}
    studentUniqueForumLLParticipations = {}
    lastSavedVideo = {}
    lastSavedForum = {}

    #should be list not set because the order should not change
    uniquePages = list()
    uniqueProblems = list()
    uniqueVideos = list()
    uniqueForums = list()
    instructors = set()


    #eventXtract
    with open(eventXtractFile,'r') as dest_f:
        data_iter = csv.reader(dest_f, 
                               delimiter = ",", 
                               quotechar = '"')
            
        data = [data for data in data_iter]
    rows = len(data)
    cols = len(data[0])
    #set default
    eventStudentIdColInd = 0
    eventEventTypeColInd = 1
    eventTimeColInd = 3
    #get column index for import fields
    for i in range(1, cols):
        if data[0][i].strip('\'') == "anon_screen_name":
            eventStudentIdColInd = i
        elif data[0][i].strip('\'') == "event_type":
            eventEventTypeColInd = i
        elif data[0][i].strip('\'') == "time":
            eventTimeColInd = i
        
    if debug:
        cells = data[0]
        cells = np.append(cells, ["aggregation"])
        row = ",".join(cells.tolist()) + "\n"
        debugEventXtractOutputFile.write(row)
            
    for i in range(1, rows):
        studentId = data[i][eventStudentIdColInd]
        if studentId in instructors:
            continue
        eventTime = data[i][eventTimeColInd]
        if studentId not in studentAggData.keys():
            studentAggData[studentId] = newAggrDataRow()
        eventType = data[i][eventEventTypeColInd]
        [event, returnedVal] = parseEventType(eventType)
        aggAction = ""
        if event == readEvent:
            aggAction = addToPageView(studentId, returnedVal)
        elif event == actionEvent:
            aggAction = addToAction(studentId, returnedVal)
        elif event == instructorEvent:
            aggAction = addToInstructor(studentId)
            continue
        elif event == forumOtherEvent:
            aggAction = addToForumSession(studentId, eventTime)
        elif event == forumLLParticipationEvent:
            aggAction = addToForumLLParticipation(studentId, returnedVal, eventTime)
        elif event == forumHLParticipationEvent:
            aggAction = addToForumHLParticipation(studentId, returnedVal, eventTime)
        if debug:
            cells = data[i]
            cells = np.append(cells, [aggAction])
            row = ",".join(cells.tolist()) + "\n"
            debugEventXtractOutputFile.write(row)
            
    data = None
    gc.collect()

     
    #HOMEWORK
    #important fields in activityGrade
    if homeworkFile != "":
        #default column index
        homeworkStudentIdColInd = 12
        homeworkModuleTypeColInd = 11
        homeworkModuleIdColInd = 14
        with open(homeworkFile,'r') as dest_f:
            data_iter = csv.reader(dest_f, 
                                   delimiter = ",", 
                                   quotechar = '"')
            data = [data for data in data_iter]
        rows = len(data)
        cols = len(data[0])
        
        #get column index for import fields
        for i in range(1, cols):
            if data[0][i].strip('\'') == "anon_screen_name":
                homeworkStudentIdColInd = i
            elif data[0][i].strip('\'') == "module_type":
                homeworkModuleTypeColInd = i
            elif data[0][i].strip('\'') == "module_id":
                homeworkModuleIdColInd = i

        if debug:
            cells = data[0]
            cells = np.append(cells, ["aggregation"])
            row = ",".join(cells.tolist()) + "\n"
            debugHomeworkOutputFile.write(row)
        for i in range(1, rows):
            studentId = data[i][homeworkStudentIdColInd]
            aggAction = ""
            if studentId in instructors:
                continue
            if studentId not in studentAggData.keys():
                #studentAggData[studentId] = newAggrDataRow()
                continue
            moduleType = data[i][homeworkModuleTypeColInd]
            moduleId = data[i][homeworkModuleIdColInd]
            if moduleType == "problem":
                aggAction = addToAction(studentId, moduleId)

            if debug:
                cells = data[i]
                cells = np.append(cells, [aggAction])
                row = ",".join(cells.tolist()) + "\n"
                debugHomeworkOutputFile.write(row)

        data = None
        gc.collect()


    #video
    #important fields in videoInteraction
    if videoInteractionFile != "":
        #default column index
        videoStudentIdColInd = 13
        videoEventTypeColInd = 0
        videoCurrentTimeColInd = 2
        videoTimeColId = 10
        videoIdColInd = 14
        with open(videoInteractionFile,'r') as dest_f:
            data_iter = csv.reader(dest_f, 
                                   delimiter = ",", 
                                   quotechar = '"')
            data = [data for data in data_iter]

        rows = len(data)
        cols = len(data[0])
        for i in range(1, cols):
            if data[0][i].strip('\'') == "anon_screen_name":
                videoStudentIdColInd = i
            elif data[0][i].strip('\'') == "event_type":
                videoEventTypeColInd = i
            elif data[0][i].strip('\'') == "video_current_time":
                videoCurrentTimeColInd = i
            elif data[0][i].strip('\'') == "time":
                videoTimeColId = i
            elif data[0][i].strip('\'') == "video_id":
                videoIdColInd = i
                
        if debug:
            cells = data[0]
            cells = np.append(cells, ["aggregation"])
            row = ",".join(cells.tolist()) + "\n"
            debugVideoInteractionOutputFile.write(row)
        for i in range(1, rows):
            studentId = data[i][videoStudentIdColInd]
            aggAction = ""
            if studentId in instructors:
                continue
            if studentId not in studentAggData.keys():
                studentAggData[studentId] = newAggrDataRow()
            eventType = data[i][videoEventTypeColInd]
            currentTime = data[i][videoCurrentTimeColInd]
            time = data[i][videoTimeColId]
            videoId = data[i][videoIdColInd]
            inDifferentVideoSession = differentVideoSession(studentId, videoId, eventType, time)
            if (eventType == "play_video" and 
                    (currentTime is not None and currentTime.lower() != "none" and currentTime != "") and 
                     inDifferentVideoSession == True):
                aggAction = addToVideo(studentId, videoId)

            if debug:
                cells = data[i]
                cells = np.append(cells, [aggAction])
                row = ",".join(cells.tolist()) + "\n"
                debugVideoInteractionOutputFile.write(row)

        dat = None
        gc.collect()

    #set distinct counts
    for studentId in studentAggData.keys():
        #distinct read
        if studentId in studentUniquePages.keys():
            studentAggData[studentId]["distinct_page_view_count"] = len(studentUniquePages[studentId])
        #distinct action
        if studentId in studentUniqueProblems.keys():
            studentAggData[studentId]["distinct_problem_action_count"] = len(studentUniqueProblems[studentId])
        #distinct video
        if studentId in studentUniqueVideos.keys():
            studentAggData[studentId]["distinct_video_play_count"] = len(studentUniqueVideos[studentId])
        #distinct forum action
        if studentId in studentUniqueForumHLParticipations.keys():
            studentAggData[studentId]["distinct_forum_HL_participation_count"] = len(studentUniqueForumHLParticipations[studentId])
        #distinct forum participation
        if studentId in studentUniqueForumLLParticipations.keys():
            studentAggData[studentId]["distinct_forum_LL_participation_count"] = len(studentUniqueForumLLParticipations[studentId])

    #set performance
    if performanceFile != "":
        performanceStudentInd = 0
        performanceGradeColInd = 2
        with open(performanceFile,'r') as dest_f:
            data_iter = csv.reader(dest_f, 
                                   delimiter = ",", 
                                   quotechar = '"')
            data = [data for data in data_iter]
        rows = len(data)
        for i in range(1, rows):
            studentId = data[i][performanceStudentInd]
            if studentId in studentAggData.keys():
                studentAggData[studentId]["performance_grade"] = data[i][performanceGradeColInd]
            
    if debug:
        debugEventXtractOutputFile.close()
        debugHomeworkOutputFile.close()
        debugVideoInteractionOutputFile.close()
        
    #output file
    aggOutputFileName = "datastage_aggregated_data.txt"
    aggOutputFile = open(aggOutputFileName, "w")
    #output header
    aggrDataHeader = None
    if videoInteractionFile != "":
        aggrDataHeader = ["page_view_count",
                    "distinct_page_view_count",
                    "problem_action_count",
                    "distinct_problem_action_count",
                    "video_play_count",
                    "distinct_video_play_count",
                    "forum_session_count",
                    "forum_HL_participation_count",
                    "distinct_forum_HL_participation_count",
                    "forum_LL_participation_count",
                    "distinct_forum_LL_participation_count"]
    else:
        aggrDataHeader = ["page_view_count",
                    "distinct_page_view_count",
                    "problem_action_count",
                    "distinct_problem_action_count",
                    "forum_session_count",
                    "forum_HL_participation_count",
                    "distinct_forum_HL_participation_count",
                    "forum_LL_participation_count",
                    "distinct_forum_LL_participation_count"]

    if performanceFile != "":
        aggrDataHeader.append("performance_grade") 
    

    aggOutputFile.write("student\t" + "\t".join(aggrDataHeader) + "\n")
    for studentId in studentAggData.keys():
        row = studentId
        for colId in aggrDataHeader:
            row = "{0}\t{1}".format(row, studentAggData[studentId][colId])
        row = row + "\n"
        aggOutputFile.write(row)
    aggOutputFile.close()
