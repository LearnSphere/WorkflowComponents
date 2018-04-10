from utilities import db, moocdb_utils
from datetime import datetime
import json

def GetVideoEvents(vars, original_item_id):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    oe_type_map = moocdb_utils.GetObservedEventTypeMap(vars)
    events = []
    
    gen_anon = vars['general_anon_col_name']
    
    q = "SELECT * FROM lecture_submission_metadata JOIN `{0}`.hash_mapping USING ({1}) WHERE item_id={2}".format(vars['source']['hash_mapping_db'], gen_anon, original_item_id)
    if vars['options']['debug']:
        q += " AND {} IN ({})".format(gen_anon, ",".join(vars['hash_map']['qls_general']))
    rows = general_db_selector.query(q)
    for row in rows:
        events.append({
            'user_original_id': vars['hash_map']['map_general'][row[gen_anon]],
            'item_type': 'tutorials',
            'item_original_id': original_item_id,
            'observed_event_data': json.dumps({}),
            'observed_event_timestamp': datetime.fromtimestamp(row['submission_time']),
            'observed_event_type_id': oe_type_map['tutorial_visit'],
            'observed_event_data': json.dumps({}),
        })
        
    return events
