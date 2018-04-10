from ...utilities import db, moocdb_utils
from common import *

def GetProblems(vars):
    problem_type_map = moocdb_utils.GetProblemTypeMap(vars)
    output_items = []
    
    last_section_id = None
    for node in vars['resource_list']:
        if node["_id"]["category"] == "problem":
            fqid = GetFullyQualifiedID(node)
            output_items.append({
                'problem_original_id': fqid,
                'problem_name': node["metadata"]["display_name"],
                'problem_type_id': problem_type_map["multipart"],
                'problem_parent_original_id': fqid,
                'resource_original_id': fqid,
                'problem_child_number': None,
                'problem_release_timestamp': None,
                'problem_soft_deadline': None,
                'problem_hard_deadline': None,
                'problem_max_submission': None,
            })
            
    return output_items