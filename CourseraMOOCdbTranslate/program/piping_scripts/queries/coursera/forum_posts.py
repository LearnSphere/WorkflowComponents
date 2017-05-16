from utilities import db, moocdb_utils
from common import *
from datetime import datetime

def GetForumPosts(vars):
    # DB connections
    # --------------
    s = vars['source']
    forum_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['forum_db'])
    
    output_items = []
    post_ctid = moocdb_utils.GetCollaborationTypeMap(vars)['forum_post']
    comment_ctid = moocdb_utils.GetCollaborationTypeMap(vars)['forum_comment']
    
    q_threads = "SELECT * FROM forum_threads JOIN `{0}`.hash_mapping USING ({1})".format(vars['source']['hash_mapping_db'], vars['forum_anon_col_name'])
    if vars['options']['debug']:
        in_list = ",".join(vars['hash_map']['qls_forum'])
        q_threads += " WHERE {} IN ({})".format(vars['forum_anon_col_name'], in_list)
    forum_threads_rows = forum_db_selector.query(q_threads)
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} forum posts from source".format(len(forum_threads_rows)))
    
    q_posts = "SELECT forum_posts.id AS post_id, forum_threads.id AS thread_id, forum_forums.id AS forum_id, forum_posts.{0} AS poster_id, post_text, post_time FROM forum_posts JOIN `{1}`.hash_mapping USING ({0}) JOIN forum_threads ON forum_posts.thread_id=forum_threads.id JOIN forum_forums ON forum_threads.forum_id=forum_forums.id".format(vars['forum_anon_col_name'], vars['source']['hash_mapping_db'])
    
    if vars['options']['debug']:
        in_list = ",".join(vars['hash_map']['qls_forum'])
        q_posts += " WHERE forum_posts.{} IN ({})".format(vars['forum_anon_col_name'], in_list)
    forum_posts_rows = forum_db_selector.query(q_posts)
    
    thread_forum_ids = {x['id']: x['forum_id'] for x in forum_threads_rows}
    
    thread_first_post_id = {}
    forum_num_posts = {}
    posts_num_comments = {}
    for p in forum_posts_rows:
        if p['thread_id'] not in thread_forum_ids.keys(): continue
        forum_id = thread_forum_ids[p['thread_id']]
        thread_id = p['thread_id']
        is_root_post = thread_id not in thread_first_post_id.keys()
        if is_root_post: thread_first_post_id[thread_id] = p['post_id']
        parent_id = None if is_root_post else "post_" + str(thread_first_post_id[thread_id])
        x = {
            'original_id': 'post_' + str(p['post_id']),
            'resource_original_id': p['forum_id'],
            'collaboration_type_id': post_ctid if is_root_post else comment_ctid,
            'collaboration_parent_original_id': parent_id,
            'user_original_id': vars['hash_map']['map_forum'][p['poster_id']],
            'collaboration_content': p['post_text'],
            'collaboration_timestamp': datetime.fromtimestamp(p['post_time']),
        }
        
        if is_root_post:
           if parent_id not in forum_num_posts.keys(): forum_num_posts[parent_id] = 1
           x['collaboration_child_number'] = forum_num_posts[parent_id]
           forum_num_posts[parent_id] += 1
        else:
           if parent_id not in posts_num_comments.keys(): posts_num_comments[parent_id] = 1
           x['collaboration_child_number'] = posts_num_comments[parent_id]
           posts_num_comments[parent_id] += 1
        
        output_items.append(x)
    
    q_comments = "SELECT forum_comments.id AS comment_id, forum_comments.post_id AS post_id, forum_forums.id AS forum_id, forum_comments.{0} AS poster_id, comment_text, forum_comments.post_time AS post_time FROM forum_comments JOIN `{1}`.hash_mapping USING ({0}) JOIN forum_posts ON forum_comments.post_id=forum_posts.id JOIN forum_threads ON forum_posts.thread_id=forum_threads.id JOIN forum_forums ON forum_threads.forum_id=forum_forums.id".format(vars['forum_anon_col_name'], vars['source']['hash_mapping_db'])
    if vars['options']['debug']:
        in_list = ",".join(vars['hash_map']['qls_forum'])
        q_comments += " WHERE forum_comments.{} IN ({})".format(vars['forum_anon_col_name'], in_list)
    forum_comments_rows = forum_db_selector.query(q_comments)
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} forum_comments from source".format(len(forum_posts_rows) + len(forum_comments_rows) - len(forum_threads_rows)))
    
    for c in forum_comments_rows:
        parent_id = "post_" + str(c['post_id'])
        
        if parent_id not in posts_num_comments.keys(): posts_num_comments[parent_id] = 1
        child_number = posts_num_comments[parent_id]
        posts_num_comments[parent_id] += 1
           
        x = {
            'original_id': 'comment_' + str(c['comment_id']),
            'resource_original_id': c['forum_id'],
            'collaboration_type_id': comment_ctid,
            'collaboration_parent_original_id': parent_id,
            'collaboration_child_number': child_number,
            'collaboration_timestamp': datetime.fromtimestamp(c['post_time']),
            'collaboration_content': c['comment_text'],
            'user_original_id': vars['hash_map']['map_forum'][c['poster_id']],
        }
        
        output_items.append(x)
        
    return output_items
    
