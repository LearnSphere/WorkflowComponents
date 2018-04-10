from ...utilities import db, moocdb_utils
from common import *

def GetForums(vars):
    output_items = []
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['forum']
    
    # course_doc = vars['resource_list'][0]
    # discussion_topics = course_doc['metadata']['discussion_topics']
    # src_forums = [{'id': discussion_topics[name]['id'], 'name': name} for name in discussion_topics.keys()]
    
    # for forum in src_forums:
        # output_items.append({
            # 'original_id': forum['id'],
            # 'resource_uri': forum['id'],
            # 'resource_name': forum['name'],
            # 'resource_parent_original_id': None,
            # 'resource_child_number': None,
            # 'resource_type_id': resource_type_id,
        # })
    
    output_items.append({
        'original_id': 'openedx_forum',
        'resource_uri': None,
        'resource_name': 'Forum',
        'resource_parent_original_id': None,
        'resource_child_number': None,
        'resource_type_id': resource_type_id,
    })
    
    return output_items