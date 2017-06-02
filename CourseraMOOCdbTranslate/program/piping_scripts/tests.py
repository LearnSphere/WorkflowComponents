from utilities import db, moocdb_utils
from observations import InsertObservedEvents

def TransformTestData(vars):
    test_id_maps = {}
    
    # Problems
    # ---------
    
    fields = {
        'problem_id': 'num',
        'problem_name': 'string',
        'problem_type_id': 'num',
        'problem_parent_id': 'num',
        'resource_id': 'num',
        'problem_child_number': 'num',
        'problem_release_timestamp': 'datetime',
        'problem_soft_deadline': 'datetime',
        'problem_hard_deadline': 'datetime',
        'problem_max_submission': 'num',
    }
    
    t = vars['target']
    problem_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'problems', fields)
    problem_id_map = {}
    
    problems = vars['queries'].GetProblems(vars)
    problem_index = 1
    for problem in problems:
        problem['problem_id'] = problem_index
        problem_id_map[problem['problem_original_id']] = problem_index
        problem_index += 1
        
        problem['problem_parent_id'] = None
        if problem['problem_parent_original_id'] != None:
            problem['problem_parent_id'] = vars['id_maps']['tests'][problem['problem_parent_original_id']]
        
        problem['resource_id'] = None
        if problem['resource_original_id'] != None:
            problem['resource_id'] = vars['id_maps']['tests'][problem['resource_original_id']]
        problem_inserter.addRow({k: problem[k] for k in fields})
    
    problem_inserter.insertPendingRows()
    
    test_id_maps['problems'] = problem_id_map
    
    # Submissions, submission_events and assessments
    # -----------------------------------------------
    
    submission_index = 1
    assessment_index = 1
    tests = vars['queries'].GetTests(vars)
    ti = 0
    oetid = moocdb_utils.GetObservedEventTypeMap(vars)['problem_submission']
    for test in tests:
        ti += 1
        vars['logger'].Log(vars, "\tSubmissions, assessments, and observed events for test {} out of {}".format(ti, len(tests)))
        
        user_num_submissions = {}
        submission_assessment_data = vars['queries'].GetSubmissionAndAssessmentData(vars, test)
        
        submissions = []
        assessments = []
        observed_events = []
        
        for submission in submission_assessment_data:
            user_original_id = submission['user_original_id']
            user_id = vars['id_maps']['users'][user_original_id]
            if user_id not in user_num_submissions.keys():
                user_num_submissions[user_id] = 1
                
            submissions.append({
                'submission_id': submission_index,
                'user_id': user_id,
                'problem_id': problem_id_map[submission['problem_original_id']],
                'submission_timestamp': submission['submission_timestamp'],
                'submission_answer': submission['submission_answer'],
                'submission_attempt_number': user_num_submissions[user_id],
                'submission_is_submitted': 1,
            })
            user_num_submissions[user_id] += 1
            
            for assn in submission['assessments']:
                grader_id = 0 if assn['grader_original_id'] == 0 else vars['id_maps']['user'][assn['grader_original_id'].lower()]
                assessments.append({
                    'assessment_id': assessment_index,
                    'submission_id': submission_index,
                    'assessment_grade': assn['grade'],
                    'assessment_max_grade': assn['max_grade'],
                    'assessment_grader_id': grader_id,
                    'assessment_timestamp': assn['assessment_timestamp'],
                })
                assessment_index += 1
            
            observed_events.append({
                'observed_event_type_id': oetid,
                'user_id': user_id,
                'item_id': problem_id_map[problem['problem_original_id']],
                'observed_event_timestamp': submission['submission_timestamp'],
                'observed_event_data': '{}',
            })
            
            submission_index += 1
        
        InsertSubmissions(vars, submissions)
        InsertAssessments(vars, assessments)
        InsertObservedEvents(vars, observed_events)
    
    return test_id_maps
    
def InsertSubmissions(vars, submissions):
    fields = {
        'submission_id': 'num',
        'user_id': 'num',
        'problem_id': 'num',
        'submission_timestamp': 'datetime',
        'submission_answer': 'string',
        'submission_attempt_number': 'num',
        'submission_is_submitted': 'num',
    }
    
    t = vars['target']
    submission_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'submissions', fields)
    for submission in submissions:
        submission_inserter.addRow({k: submission[k] for k in fields})

    submission_inserter.insertPendingRows()
    
def InsertAssessments(vars, assessments):
    fields = {
        'assessment_id': 'num',
        'submission_id': 'num',
        'assessment_grade': 'num',
        'assessment_max_grade': 'num',
        'assessment_grader_id': 'num',
        'assessment_timestamp': 'datetime',
    }
    
    t = vars['target']
    assessment_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'assessments', fields)
    for assessment in assessments:
        assessment_inserter.addRow({k: assessment[k] for k in fields})

    assessment_inserter.insertPendingRows()