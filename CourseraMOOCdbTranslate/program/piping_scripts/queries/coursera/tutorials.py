from utilities import db, moocdb_utils
from common import *

def GetTutorials(vars):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    output_items = []
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['tutorial']
    
    src_videos = general_db_selector.query("SELECT * FROM lecture_metadata")
    vars["logger"].Log(vars, "\t\tCounts: Read {} videos from source".format(len(src_videos)))
    
    items_sections = general_db_selector.query("SELECT * FROM items_sections WHERE item_type='lecture'")
    items_sections_lookup = {x['item_id']: {'resource_parent_id': x['section_id'], 'resource_child_number': x['order']} for x in items_sections if x['item_type'] == 'lecture'}
    for video in src_videos:
        item = {
            'original_id': video['id'],
            'resource_name': video['title'],
            'resource_uri': "www.coursera.org/{}/lecture/view?lecture_id={}".format(vars['source']['course_url_id'], video['id']),
            'resource_parent_original_id': None,
            'resource_child_number': None,
            'resource_type_id': resource_type_id,
        }
        
        if video['id'] in items_sections_lookup.keys():
            item['resource_parent_original_id'] = items_sections_lookup[video['id']]['resource_parent_id']
            item['resource_child_number'] = items_sections_lookup[video['id']]['resource_child_number']
            
        output_items.append(item)
    
    return output_items
