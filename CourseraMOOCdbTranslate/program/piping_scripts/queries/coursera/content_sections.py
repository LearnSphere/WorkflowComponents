from utilities import db, moocdb_utils
from common import *

def GetContentSections(vars):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['content_section']
    output_items = []
    src_sections = general_db_selector.query("SELECT * FROM sections ORDER BY display_order, id")
    vars["logger"].Log(vars, "\t\tCounts: Read {} content sections from source".format(len(src_sections)))
    
    section_index = 1
    
    for section in src_sections:
        output_items.append({
            'original_id': section['id'],
            'resource_name': section['title'],
            'resource_uri': '',
            'resource_parent_original_id': None,
            'resource_child_number': section_index,
            'resource_type_id': resource_type_id,
        })

        section_index += 1
    
    return output_items
