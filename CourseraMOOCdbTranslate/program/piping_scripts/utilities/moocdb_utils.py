import db

def GetResourceTypeMap(vars):
    # DB connections
    # --------------
    c = vars['core']
    core_db_selector = db.Selector(c['host'], c['user'], c['password'], c['port'], c['db'])
    
    return {x['resource_type_content']: x['resource_type_id'] for x in core_db_selector.query("SELECT * FROM resource_types")}
    
def GetCollaborationTypeMap(vars):
    # DB connections
    # --------------
    c = vars['core']
    core_db_selector = db.Selector(c['host'], c['user'], c['password'], c['port'], c['db'])
    
    return {x['name']: x['id'] for x in core_db_selector.query("SELECT * FROM collaboration_types")}
    
def GetProblemTypeMap(vars):
    # DB connections
    # --------------
    c = vars['core']
    core_db_selector = db.Selector(c['host'], c['user'], c['password'], c['port'], c['db'])
    
    return {x['name']: x['id'] for x in core_db_selector.query("SELECT * FROM problem_types")}
    
def GetObservedEventTypeMap(vars):
    # DB connections
    # --------------
    c = vars['core']
    core_db_selector = db.Selector(c['host'], c['user'], c['password'], c['port'], c['db'])
    
    return {x['name']: x['id'] for x in core_db_selector.query("SELECT * FROM observed_event_types")}
    
def GetUserTypeMap(vars):
    # DB connections
    # --------------
    c = vars['core']
    core_db_selector = db.Selector(c['host'], c['user'], c['password'], c['port'], c['db'])
    
    return {x['name']: x['id'] for x in core_db_selector.query("SELECT * FROM user_types")}