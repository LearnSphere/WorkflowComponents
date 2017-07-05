from utilities import db, moocdb_utils
from coursera_quizzes import *
from common import *
from datetime import datetime

def GetSubmissionAndAssessmentData(vars, test):
    
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    output_items = []
    gen_anon = vars['general_anon_col_name']
    
    if 'quiz_' in test['original_id']:
        stud_quiz_submissions = GetStudentQuizResponses(vars, test['original_id'].replace("quiz_", ""))
        
        vars["logger"].Log(vars, "\t\tCounts: Read {} submissions from source for {}".format(len(stud_quiz_submissions.keys()), test['original_id']))
        
        for uid in stud_quiz_submissions.keys():
            for qid in stud_quiz_submissions[uid].keys():
                attempt_number = 0
                for answer in stud_quiz_submissions[uid][qid]:
                    attempt_number += 1
                    submission = {
                        'user_original_id': uid,
                        'problem_original_id': 'quiz_question_' + qid,
                        'submission_timestamp': answer['submission_time'],
                        'submission_answer': json.dumps([answer['answer']]),
                        'submission_attempt_number': attempt_number,
                        'submission_is_submitted': 1,
                        'assessments': [],
                    }
                    if answer['grade'] != -1:
                        submission['assessments'].append({
                            'grader_original_id': 0,
                            'grade': answer['grade'],
                            'max_grade': 1,
                            'assessment_timestamp': answer['submission_time'],
                        })
                    output_items.append(submission)

    return output_items
