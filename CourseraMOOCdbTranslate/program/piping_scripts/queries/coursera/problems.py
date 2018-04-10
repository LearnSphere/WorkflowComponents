from utilities import db, moocdb_utils
from common import *
from coursera_quizzes import *

def GetProblems(vars):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    problem_type_map = moocdb_utils.GetProblemTypeMap(vars)
    output_items = []
    
    quiz_ids = [x['id'] for x in general_db_selector.query("SELECT * FROM quiz_metadata")]
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} quizzes from source".format(len(quiz_ids)))
    
    for quiz_id in quiz_ids:
        quiz_metadata = GetQuizMetadata(vars, quiz_id)
        test_original_id = 'quiz_' + str(quiz_id)
        
        quiz_content = GetQuizContent(vars, quiz_id)

        question_index = 0
        for question_group in quiz_content['question_groups']:
            for question in question_group:
                
                if 'choice_type' in question.keys() and question['choice_type'] == 'radio': problem_type = 'question_mc_single'
                elif 'choice_type' in question.keys() and question['choice_type'] == 'select': problem_type = 'question_mc_multiple'
                else: problem_type = 'question_free_text'
                
                item = {
                    'original_item_type': 'quiz_question',
                    'problem_original_id': 'quiz_question_' + question['id'],
                    'problem_name': test_original_id + "." + question['id'],
                    'problem_type_id': problem_type_map[problem_type],
                    'problem_parent_original_id': test_original_id,
                    'problem_child_number': question_index,
                    'problem_release_timestamp': quiz_metadata['open_time'],
                    'problem_soft_deadline': quiz_metadata['soft_deadline'],
                    'problem_hard_deadline': quiz_metadata['hard_deadline'],
                    'problem_max_submission': quiz_metadata['max_submissions'],
                    'resource_original_id': test_original_id,
                }
                output_items.append(item)
                question_index += 1

    assn_metadata = []
    assn_metadata_rows = general_db_selector.query("SELECT * FROM assignment_metadata")
    for assn_metadata in assn_metadata_rows:
        assn_id = assn_metadata['id']
        test_original_id = 'assignment_' + str(assn_id)
        
        assn_parts = general_db_selector.query("SELECT * FROM assignment_part_metadata WHERE assignment_id={}".format(assn_id))
        assn_part_index = 0
        for assn_part in assn_parts:
            assn_part_original_id = "assn_part_" + str(assn_part['id'])
            item = {
                'original_item_type': 'assignment_part',
                'problem_original_id': assn_part_original_id,
                'problem_name': assn_part_original_id,
                'problem_type_id': problem_type_map['assignment_part'],
                'problem_parent_original_id': test_original_id,
                'problem_child_number': assn_part_index,
                'problem_release_timestamp': assn_metadata['open_time'],
                'problem_soft_deadline': assn_metadata['soft_close_time'],
                'problem_hard_deadline': assn_metadata['hard_close_time'],
                'problem_max_submission': assn_metadata['maximum_submissions'],
                'problem_weight': None,
                'resource_original_id': test_original_id,
            }
            output_items.append(item)
            assn_part_index += 1
    
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} problems from source".format(len(output_items)))
    
    return output_items
