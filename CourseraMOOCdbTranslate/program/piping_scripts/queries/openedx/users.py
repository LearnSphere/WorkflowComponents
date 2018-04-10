from ...utilities import db, moocdb_utils
from common import *

def GetUsers (vars):
    user_type_map = moocdb_utils.GetUserTypeMap(vars)
    
    # Staff, instructor, and student users had to be fetched in vars.py to build the ID maps.
    
    output_items = []
    user_types_items = {'Student': vars['student_users'], 'Teaching Staff': vars['staff_users'], 'Instructor': vars['inst_users']}
    
    committed_emails = []
    for k in user_types_items.keys():
        for u in user_types_items[k]:
            email = u['email'].lower()
            if email not in committed_emails:
                output_items.append({
                    'original_id': email,
                    'user_email': email,
                    'user_ip': None,
                    'user_country': u['country'],
                    'user_type_id': user_type_map[k],
                })
                
                committed_emails.append(email)
        
    return output_items