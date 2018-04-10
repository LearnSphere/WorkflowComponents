from ...utilities import db, moocdb_utils
from common import *
from datetime import datetime

def GetForumPosts(vars):
    output_items = []
    
    post_ctid = moocdb_utils.GetCollaborationTypeMap(vars)['forum_post']
    comment_ctid = moocdb_utils.GetCollaborationTypeMap(vars)['forum_comment']
    
    forum_num_posts = 0
    posts_num_children = {}
    
    fc_coll = vars['cons']['forum_contents']['forum_contents']
    fu_coll = vars['cons']['forum_users']['forum_users']
    posts = fc_coll.find({"course_id": vars['source']['course_id']})
    
    for p in posts:
        ctid = post_ctid if p["_type"] == "CommentThread" else comment_ctid
        
        parent_id = None
        if 'parent_ids' in p.keys() and len(p['parent_ids']) >0: parent_id = str(p['parent_ids'][0])
        elif p['_type'] == 'Comment': parent_id = str(p['comment_thread_id'])
        
        if parent_id not in posts_num_children.keys(): posts_num_children[parent_id] = 0
        posts_num_children[parent_id] += 1
        
        r = fu_coll.find({'_id': p['author_id']}).limit(1)
        if r.count() == 0: continue # Ex: user was somehow deleted from the forum_users database
        
        email = r[0]['email'].lower()
        if email not in vars['id_maps']['users'].keys(): continue # No user with an openedx email matching the forum_users email was found
            
        output_items.append({
            'original_id': str(p['_id']),
            'resource_original_id': None,
            'collaboration_type_id': ctid,
            'collaboration_parent_original_id': parent_id,
            'collaboration_child_number': posts_num_children[parent_id],
            'user_original_id': email,
            'collaboration_content': p['body'].encode('utf-8'),
            'collaboration_timestamp': p['created_at'],
        })
    
    return output_items
    