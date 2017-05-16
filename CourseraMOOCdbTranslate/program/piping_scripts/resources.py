from utilities import db

def TransformResourceData(vars):
    
    fields = {
        'resource_id': 'num',
        'resource_type_id': 'num',
        'resource_name': 'string',
        'resource_uri': 'string',
        'resource_parent_id': 'num',
        'resource_child_number': 'num',
    }
    
    t = vars['target']
    resource_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'resources', fields)
    
    resource_id_map = {}
    resources = [
        {'type': 'indices', 'parent_type': None, 'items': vars['queries'].GetIndices(vars)},
        {'type': 'content_sections', 'parent_type': None, 'items': vars['queries'].GetContentSections(vars)},
        {'type': 'tutorials', 'parent_type': 'content_sections', 'items': vars['queries'].GetTutorials(vars)},
        {'type': 'tests', 'parent_type': 'content_sections', 'items': vars['queries'].GetTests(vars)},
        {'type': 'books', 'parent_type': None, 'items': vars['queries'].GetBooks(vars)},
        {'type': 'wikis', 'parent_type': None, 'items': vars['queries'].GetWikis(vars)},
        {'type': 'forums', 'parent_type': None, 'items': vars['queries'].GetForums(vars)},
    ]
    
    resource_moocdb_id = 1
    
    for resource_subset in resources:
        if resource_subset['type'] not in resource_id_map.keys(): resource_id_map[resource_subset['type']] = {}
        for item in resource_subset['items']:
            item['resource_id'] = resource_moocdb_id
            resource_id_map[resource_subset['type']][item['original_id']] = resource_moocdb_id
            resource_moocdb_id += 1
            
            rpt = resource_subset['parent_type']
            rpoid = item['resource_parent_original_id']
            
            if rpt != None and rpoid != None and rpoid in resource_id_map[rpt].keys():
                rpmid = resource_id_map[rpt][rpoid]
                item['resource_parent_id'] = rpmid
            else:
                item['resource_parent_id'] = None
            
            resource_inserter.addRow({k: item[k] for k in fields})
            
    resource_inserter.insertPendingRows()
    
    vars["logger"].Log(vars, "Counts: Inserted {} resources to target".format(resource_inserter.num_inserted_rows))
    
    return resource_id_map