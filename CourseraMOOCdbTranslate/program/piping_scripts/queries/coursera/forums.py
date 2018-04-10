from utilities import db, moocdb_utils
from common import *

def GetForums(vars):
    # DB connections
    # --------------
    s = vars['source']
    forum_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['forum_db'])
    
    output_items = []
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['forum']
    src_forums = forum_db_selector.query("SELECT * FROM forum_forums ORDER BY display_order, id")
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} forums from source".format(len(src_forums)))
    
    for forum in src_forums:
        output_items.append({
            'original_id': forum['id'],
            'resource_uri': "www.coursera.org/{}/forum/list?forum_id={}".format(vars['source']['course_url_id'], forum['id']),
            'resource_name': forum['name'],
            'resource_parent_original_id': None,
            'resource_child_number': None,
            'resource_type_id': resource_type_id,
        })
    
    return output_items
