from utilities import db, moocdb_utils
from common import *
from datetime import datetime

def GetForumVotes(vars):
    # DB connections
    # --------------
    s = vars['source']
    forum_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['forum_db'])
    
    forum_anon = vars['forum_anon_col_name']
    forum_vote_ctid = moocdb_utils.GetCollaborationTypeMap(vars)['forum_vote']
    output_items = []
    
    q = "SELECT * FROM forum_reputation_record JOIN `{0}`.hash_mapping USING ({1})".format(vars['source']['hash_mapping_db'], forum_anon)
    if vars['options']['debug']:
        q += " WHERE {} IN ({})".format(forum_anon, ",".join(vars['hash_map']['qls_forum']))
    src_forum_voting_records = forum_db_selector.query(q)
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} forum_votes from source".format(len(src_forum_voting_records)))
    
    vote_index = 0
    for vote in src_forum_voting_records:
        output_items.append({
            'original_id': 'vote_' + str(vote_index),
            'user_original_id': vars['hash_map']['map_forum'][vote[forum_anon]],
            'resource_original_id': None,
            'collaboration_parent_original_id': vote['type'] + '_' + str(vote['pc_id']),
            'collaboration_child_number': None,
            'collaboration_content': vote['direction'],
            'collaboration_timestamp': vote['timestamp'],
            'collaboration_type_id': forum_vote_ctid,
        })
        vote_index += 1
        
    return output_items
