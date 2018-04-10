from ....utilities import db, moocdb_utils
from datetime import datetime
import json

def GetTestEvents(vars, original_item_id):
    oe_type_map = moocdb_utils.GetObservedEventTypeMap(vars)
    output_items = []
    
    return output_items