from utilities import db
import phpserialize
from datetime import datetime
from ..common import *
from utilities import moocdb_utils
import json

def GetAdditionalCollaborationEvents(vars):
    # DB connections
    # --------------
    s = vars['source']
    forum_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['forum_db'])
    
    oe_type_map = moocdb_utils.GetObservedEventTypeMap(vars)
    events = []
    
    # Find the id of the first forum_post of each thread. This is the item_id under which the event will be registered
    q = "SELECT id, thread_id FROM forum_posts WHERE id IN (SELECT min(id) FROM forum_posts GROUP BY thread_id)"
    thread_id_to_first_post_id = {x['thread_id']: x['id'] for x in forum_db_selector.query(q)}        
    
    table_name = "kvs_course.{}.forum_readrecord".format(vars['source']['course_id']) if vars['source']['platform_format'] == 'coursera_1' else "kvs_course.forum_readrecord"
    q = "SELECT * FROM `{}`".format(table_name)
    rows = forum_db_selector.query(q)
    
    for row in rows:
        key_parts = row['key'].split(".")
        uoid_str = key_parts[1]
        try:
            int(uoid_str)
        except:
            continue
            
        user_original_id = int(uoid_str)
        if user_original_id not in vars['hash_map']['list_raw']:
            continue
            
        forum_id = int(key_parts[0].replace('forum_', ''))
        value = phpserialize.loads(row['value'])
        if "_all" in value.keys():
            events.append({
                'user_original_id': user_original_id,
                'observed_event_type_id': oe_type_map['forum_visit'],
                'item_original_id': forum_id,
                'item_type': 'forums',
                'observed_event_timestamp': datetime.fromtimestamp(value["_all"]),
                'observed_event_data': json.dumps({}),
            })
        else:
            for k in value.keys():
                if k in thread_id_to_first_post_id.keys():
                    events.append({
                        'user_original_id': user_original_id,
                        'observed_event_type_id': oe_type_map['forum_post_read'],
                        'item_original_id': 'post_' + str(thread_id_to_first_post_id[k]),
                        'item_type': 'forum_posts',
                        'observed_event_timestamp': datetime.fromtimestamp(value[k]),
                        'observed_event_data': json.dumps({}),
                    })

    return events
