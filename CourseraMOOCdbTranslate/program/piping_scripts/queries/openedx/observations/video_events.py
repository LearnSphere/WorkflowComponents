from ....utilities import db, moocdb_utils
from datetime import datetime
import json
    
def GetVideoEvents(vars, original_item_id):
    oe_type_map = moocdb_utils.GetObservedEventTypeMap(vars)
    events = []
    
    event_video_id = original_item_id.replace("://", "-")
    event_video_id = event_video_id.replace("/", "-")
    q = "SELECT e.event_type AS et, e.time AS time, e.video_current_time AS vct, e.video_old_time AS vot, e.video_new_time AS vnt, e.video_old_speed AS vos, e.video_new_speed AS vns, ep.email AS email FROM Edx.EdxTrackEvent AS e JOIN EdxPrivate.Account AS ep USING(anon_screen_name) WHERE e.course_id LIKE '%/{}/%' AND ep.screen_name != '' AND ep.email != '' AND e.video_id='{}'".format(vars['source']['course_id'], event_video_id)
    if vars['options']['debug']:
        q += " AND ep.email IN ({})".format(",".join(["'" + x + "'" for x in vars['id_maps']['users'].keys()]))
    q += " ORDER BY ep.email, time"
    rows = db.Select(vars['curs']['tracklog'], q)
    
    last_student_id = None
    last_event_time = None
    last_event_type = None
    for row in rows:
        email = row['email'].lower()
        
        if email in vars['id_maps']['users'].keys():
            new_visit = email != last_student_id or (row['time'] - last_event_time).seconds/(3600) > 24
            if new_visit and row['et'] != 'load_video':
                events.append({
                    'user_original_id': email,
                    'item_type': 'tutorials',
                    'item_original_id': original_item_id,
                    'observed_event_data': json.dumps({}),
                    'observed_event_timestamp': row['time'],
                    'observed_event_type_id': oe_type_map['tutorial_visit'],
                })
            
            etype = row['et']
            event_data = {}
            oetid = None
            
            if etype == 'load_video':
                oetid = oe_type_map['tutorial_visit']
            elif etype in ['play_video', 'pause_video']:
                oetid = oe_type_map['video_play' if etype == 'play_video' else 'video_pause']
                event_data["current_time"] = row["vct"]
            elif etype == 'seek_video':
                if row['vnt'] > row['vot']:
                    oetid = 'video_seek_forward'
                elif row['vnt'] < row['vot']:
                    oetid = 'video_seek_back'
                    
                event_data = {"from": row["vot"], "to": row["vnt"]}
            
            if oetid != None:
                events.append({
                    'user_original_id': email,
                    'item_type': 'tutorials',
                    'item_original_id': original_item_id,
                    'observed_event_timestamp': row['time'],
                    'observed_event_data': json.dumps(event_data),
                    'observed_event_type_id': oe_type_map['tutorial_visit'],
                })
        
        last_student_id = email
        last_event_time = row['time']
        last_event_type = row['et']
    
    print "\tNum events:", len(events)
    return events