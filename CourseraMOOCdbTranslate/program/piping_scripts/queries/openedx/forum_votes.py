from ...utilities import db, moocdb_utils
from common import *
from datetime import datetime

def GetForumVotes(vars):
    forum_vote_ctid = moocdb_utils.GetCollaborationTypeMap(vars)['forum_vote']
    output_items = []
    
    return output_items