from utilities import db, moocdb_utils
from common import *

def GetUsers (vars):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    user_type_map = moocdb_utils.GetUserTypeMap(vars)
    
    # Stuff for mapping coursera access group to moocdb user type
    access_group_id_to_name = {x['id']: x['name'] for x in general_db_selector.query("SELECT * FROM access_groups")}
    access_group_name_to_user_type_name = {
        'Student': 'Student',
        'Administrator': 'Administrator',
        'Instructor': 'Instructor',
        'Teaching Staff': 'Teaching Staff',
        'Blocked': 'Blocked',
        'Student Access': 'Student Access',
        'External Viewer': 'Student Access',
        'Community TA': 'Community TA',
        'School Administrator': 'School Administrator',
        'Data Coordinator': 'School Administrator',
        'Coursera Tech Support': 'Administrator',
        'Student (Forum Banned)': 'Student (Forum Banned)',
    }
    
    # Fetch the users data
    # ---------------------
    q = "SELECT {} AS uid, normal_grade FROM course_grades".format(vars['general_anon_col_name'])
    if vars['options']['debug']:
        q += " WHERE {} IN ({})".format(vars['general_anon_col_name'], ",".join(vars['hash_map']['qls_general']))
    
    rows = general_db_selector.query(q)
    max_grade = max([row['normal_grade'] for row in rows]) if len(rows) > 0 else 1
    course_grade_dict = {vars['hash_map']['map_general'][row['uid']]: 1.0*row['normal_grade']/max_grade for row in rows if row['uid'] in vars['hash_map']['map_general'].keys()}
    
    user_items = {x: {'original_id': x, 'user_ip': None, 'user_country': None, 'user_final_grade': None, 'user_join_timestamp': None, 'user_type_id': user_type_map['Student']} for x in vars['hash_map']['list_raw']}
    
    # The join below is to ensure that we only fetch users who have corresponding hash_mapping entries
    q = "SELECT * FROM users JOIN `{0}`.hash_mapping USING (`{1}`)".format(vars['source']['hash_mapping_db'], vars['general_anon_col_name'])
    if vars['options']['debug']:
        q += " WHERE users.{} IN ({})".format(vars['general_anon_col_name'], ",".join(vars['hash_map']['qls_general']))
    user_metadata_rows = general_db_selector.query(q)

    for row in user_metadata_rows:
        user_id = vars['hash_map']['map_general'][row[vars['general_anon_col_name']]]
        
        user_items[user_id]['user_ip'] = row['last_access_ip'] if 'last_access_ip' in row.keys() else None
        
        user_items[user_id]['user_join_timestamp'] = row['registration_time']
        
        user_items[user_id]['user_final_grade'] = course_grade_dict[user_id] if user_id in course_grade_dict.keys() else None
        
        user_coursera_access_group_name = access_group_id_to_name[row['access_group_id']]
        if user_coursera_access_group_name in access_group_name_to_user_type_name:
            user_moocdb_user_type_name = access_group_name_to_user_type_name[user_coursera_access_group_name]
        else:
            user_moocdb_user_type_name = access_group_name_to_user_type_name['External Viewer']
        user_moocdb_user_type_id = user_type_map[user_moocdb_user_type_name]
        user_items[user_id]['user_type_id'] = user_moocdb_user_type_id
  
    output_items = sorted(user_items.values(), key=lambda x: x['original_id'])
    
    return output_items
