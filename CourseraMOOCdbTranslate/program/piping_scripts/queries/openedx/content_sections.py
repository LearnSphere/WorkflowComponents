from ...utilities import db, moocdb_utils
from common import *

def GetContentSections(vars):
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['content_section']
    
    src_sections = [x for x in vars["resource_list"] if x["_id"]["category"] == 'chapter']
    
    section_index = 1
    output_items = []
    for section in src_sections:
        fqid = GetFullyQualifiedID(section)
        output_items.append({
            'original_id': fqid,
            'resource_name': section['metadata']['display_name'],
            'resource_uri': '',
            'resource_parent_original_id': None,
            'resource_child_number': section_index,
            'resource_type_id': resource_type_id,
        })

        section_index += 1
    
    return output_items