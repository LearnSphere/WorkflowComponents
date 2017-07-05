from ...utilities import db, moocdb_utils
from common import *
from datetime import datetime

def GetSubmissionAndAssessmentData(vars, problem):
    problem_sm_history_items = []
    
    q = """
        SELECT
                    au.email AS email, csm.id AS id
        FROM
                    auth_user AS au
            JOIN    courseware_studentmodule AS csm ON au.id=csm.student_id
            
        WHERE
                    csm.module_type = 'problem'
            AND     csm.module_id = '{}'
            AND     csm.max_grade IS NOT NULL
            
    """.format(problem['problem_original_id'])
    if vars['options']['debug']:
        q += " AND au.email IN ({})".format(",".join(["'" + k + "'" for k in vars['id_maps']['users'].keys()]))
    
    rows = db.Select(vars['curs']['lms'], q)
    if len(rows) == 0: return []
    
    student_modules = {row['id']: {'email': row['email'].lower()} for row in rows}
    
    
    q = """
        SELECT      student_module_id, grade, max_grade, created AS submission_timestamp
        FROM        courseware_studentmodulehistory
        WHERE       student_module_id IN ({})
    """.format(",".join([str(x) for x in student_modules.keys()]))
    q += " ORDER BY student_module_id"
    
    rows = db.Select(vars['curs']['lms'], q)
    for row in rows:
        row['user_original_id'] = student_modules[row['student_module_id']]['email']
        row['assessments'] = [{'grader_original_id': 0, 'grade': row['grade'], 'max_grade': row['max_grade'], 'assessment_timestamp': row['time']}]
    return rows
    