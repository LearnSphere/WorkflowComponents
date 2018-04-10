from utilities import db

def TransformCollaborationData(vars):
    collaboration_id_maps = {}
    
    fields = {
        'collaboration_id': 'num',
        'collaboration_type_id': 'num',
        'user_id': 'num',
        'resource_id': 'num',
        'collaboration_content': 'string',
        'collaboration_timestamp': 'datetime',
        'collaboration_parent_id': 'num',
        'collaboration_child_number': 'num',
    }
    
    t = vars['target']
    collaboration_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'collaborations', fields)
    
    # Forum Posts
    ################
    collaborations = [
        {'type': 'forum_posts', 'items': vars['queries'].GetForumPosts(vars), 'parent_type': 'forum_posts', 'resource_type': 'forums'},
        {'type': 'forum_votes', 'items': vars['queries'].GetForumVotes(vars), 'parent_type': 'forum_posts', 'resource_type': 'forums'},
        {'type': 'wiki_edits', 'items': vars['queries'].GetWikiEdits(vars), 'parent_type': None, 'resource_type': 'wikis'},
    ]
    #print collaborations
    coll_index = 1
    for coll_subset in collaborations:
        type = coll_subset['type']
        parent_type = coll_subset['parent_type']
        resource_type = coll_subset['resource_type']
        collaboration_id_maps[type] = {}
        
        for item in coll_subset['items']:
            item['collaboration_id'] = coll_index

            collaboration_id_maps[type][item['original_id']] = coll_index
            coll_index += 1
            
            add_item = True
            item['collaboration_parent_id'] = None
            cpoid = item['collaboration_parent_original_id']
            if parent_type != None and cpoid != None:
                if cpoid in collaboration_id_maps[parent_type].keys():
                    item['collaboration_parent_id'] = collaboration_id_maps[parent_type][cpoid]
                else:
                    add_item = False
            
            item['resource_id'] = None
            roid = item['resource_original_id']
            if resource_type != None and roid != None:
                if roid in vars['id_maps'][resource_type].keys():
                    item['resource_id'] = vars['id_maps'][resource_type][roid]
                else:
                    add_item = False
            
            uoid = item['user_original_id']
            if uoid in vars['id_maps']['users'].keys():
                item['user_id'] = vars['id_maps']['users'][uoid]
            else:
                add_item = False

            
            if add_item:
                collaboration_inserter.addRow({k: item[k] for k in fields})
                
        
    collaboration_inserter.insertPendingRows()
    
    vars["logger"].Log(vars, "Counts: Inserted {} collaborations to target".format(collaboration_inserter.num_inserted_rows))
    
    return collaboration_id_maps
