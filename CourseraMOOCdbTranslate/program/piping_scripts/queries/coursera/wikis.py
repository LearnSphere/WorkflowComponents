from utilities import db, moocdb_utils
from common import *

def GetWikis(vars):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['wiki']
    
    output_items = []
    src_items = general_db_selector.query("SELECT * FROM wiki_pages")
    vars["logger"].Log(vars, "\t\tCounts: Read {} wikis from source".format(len(src_items)))
    
    for wiki in src_items:
        output_items.append({
            'original_id': wiki['id'],
            'resource_name': wiki['title'],
            'resource_uri': "www.coursera.org/{}/wiki/view?page={}".format(vars['source']['course_url_id'], wiki['canonical_name']),
            'resource_parent_original_id': None,
            'resource_child_number': None,
            'resource_type_id': resource_type_id
        })
        
    return output_items
