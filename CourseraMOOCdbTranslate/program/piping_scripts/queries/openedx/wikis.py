from ...utilities import db, moocdb_utils
from common import *

def GetWikis(vars):
    resource_type_id = moocdb_utils.GetResourceTypeMap(vars)['wiki']
    
    output_items = []
        
    return output_items