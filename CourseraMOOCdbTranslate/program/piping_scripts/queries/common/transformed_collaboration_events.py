from utilities import db, moocdb_utils
import json

def GetTransformedCollaborationEvents(vars):
    # DB connections
    # --------------
    t = vars['target']
    target_db_selector = db.Selector(t['host'], t['user'], t['password'], t['port'], t['db'])
    
    oe_type_map = moocdb_utils.GetObservedEventTypeMap(vars)
    coll_type_map = moocdb_utils.GetCollaborationTypeMap(vars)
    coll_type_map_id_to_name = {coll_type_map[k]: k for k in coll_type_map.keys()}
    
    events = []
    
    rows = target_db_selector.query("SELECT * FROM collaborations JOIN collaboration_types ON collaborations.collaboration_type_id=collaboration_types.collaboration_type_id")
    for row in rows:
        coll_type_id = row['collaboration_type_id']
        coll_type_name = coll_type_map_id_to_name[coll_type_id]
        oe_type_name = coll_type_name
        oe_type_id = oe_type_map[oe_type_name]
        
        events.append({
            'user_id': row['user_id'],
            'item_id': row['collaboration_parent_id'] if row['collaboration_parent_id'] != None else row['resource_id'],
            'observed_event_type_id': oe_type_id,
            'observed_event_timestamp': row['collaboration_timestamp'],
            'observed_event_data': json.dumps({}),
        })
        
    return events
