from utilities import db, moocdb_utils
from common import *

def GetTests(vars):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    output_items = []
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['testing']
    
    src_quizzes = general_db_selector.query("SELECT * FROM quiz_metadata")
    vars["logger"].Log(vars, "\t\tCounts: Read {} quizzes from source".format(len(src_quizzes)))
    
    items_sections = general_db_selector.query("SELECT * FROM items_sections WHERE item_type='quiz'")
    items_sections_lookup = {x['item_id']: {'resource_parent_id': x['section_id'], 'resource_child_number': x['order']} for x in items_sections if x['item_type'] == 'quiz'}
    for quiz in src_quizzes:
        item = {
            'original_id': 'quiz_' + str(quiz['id']),
            'resource_name': quiz['title'],
            'resource_uri': "www.coursera.org/{}/quiz/start?quiz_id={}".format(vars['source']['course_url_id'], quiz['id']),
            'resource_parent_original_id': None,
            'resource_child_number': None,
            'resource_type_id': resource_type_id,
        }
        
        if quiz['id'] in items_sections_lookup.keys():
            item['resource_parent_original_id'] = items_sections_lookup[quiz['id']]['resource_parent_id']
            item['resource_child_number'] = items_sections_lookup[quiz['id']]['resource_child_number']
        
        output_items.append(item)
        
    src_assignments = general_db_selector.query("SELECT * FROM assignment_metadata")
    vars["logger"].Log(vars, "\t\tCounts: Read {} assignments from source".format(len(src_assignments)))
    
    items_sections = general_db_selector.query("SELECT * FROM items_sections WHERE item_type='assignment'")
    items_sections_lookup = {x['item_id']: {'resource_parent_id': x['section_id'], 'resource_child_number': x['order']} for x in items_sections if x['item_type'] == 'assignment'}
    for assn in src_assignments:
        assn_id = assn['id']
        item = {
            'original_id': 'assignment_' + str(assn_id),
            'resource_name': assn['title'],
            'resource_uri': "www.coursera.org/{}/assignment/view?assignment_id={}".format(vars['source']['course_url_id'], assn_id),
            'resource_parent_original_id': None,
            'resource_child_number': None,
            'resource_type_id': resource_type_id,
        }
        
        if assn['id'] in items_sections_lookup.keys():
            item['resource_parent_original_id'] = items_sections_lookup[assn['id']]['resource_parent_id']
            item['resource_child_number'] = items_sections_lookup[assn['id']]['resource_child_number']
            
        output_items.append(item)
    
    return output_items
