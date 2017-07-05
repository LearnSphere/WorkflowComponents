from ....utilities import db
import phpserialize
from datetime import datetime
from ..common import *
from ....utilities import moocdb_utils

def GetAdditionalCollaborationEvents(vars):
    oe_type_map = moocdb_utils.GetObservedEventTypeMap(vars)
    events = []
    
    return events