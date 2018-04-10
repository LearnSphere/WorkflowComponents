from ...utilities import db, moocdb_utils
from common import *

def GetIndices(vars):
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['index']
    
    output_items = [
        {'original_id': 'class_index', 'resource_name': 'Class Index'},
        {'original_id': 'wiki_index', 'resource_name': 'Wiki Index'},
        {'original_id': 'forum_index', 'resource_name': 'Forum Index'},
        {'original_id': 'tutorial_index', 'resource_name': 'Tutorial Index'},
        {'original_id': 'forum_index', 'resource_name': 'Forum Index'},
    ]
    
    for item in output_items:
        item['resource_uri'] = ''
        item['resource_parent_original_id'] = None
        item['resource_child_number'] = None
        item['resource_type_id'] = resource_type_id
    
    return output_items