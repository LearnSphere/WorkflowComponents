from ...utilities import db, moocdb_utils
from common import *

def GetTests(vars):
    output_items = []
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['test']
    
    last_section_id = None
    for node in vars['resource_list']:
        if node["_id"]["category"] == "chapter":
            last_section_id = GetFullyQualifiedID(node)
            num_children = 0
        
        if node["_id"]["category"] in ["problem_set", "problem", "video", "videoalpha"]:
            num_children += 1
        
        if node["_id"]["category"] in ["problem_set", "problem"]:
            num_children += 1
            fqid = GetFullyQualifiedID(node)
            output_items.append({
                "original_id": fqid,
                "resource_name": node["metadata"]["display_name"],
                "resource_uri": fqid,
                "resource_parent_original_id": last_section_id,
                "resource_child_number": num_children,
                "resource_type_id": resource_type_id,
            })
    
    return output_items