from utilities import db

def TransformObservationData(vars):
    # DB connections
    # --------------
    t = vars['target']
    target_db_selector = db.Selector(t['host'], t['user'], t['password'], t['port'], t['db'])
    
    # Videos
    i = 1
    n = len(vars['id_maps']['tutorials'].keys())
    for original_item_id in vars['id_maps']['tutorials'].keys():
        vars['logger'].Log(vars, "\tProcessing events for tutorial {} out of {}: original ID {}".format(i, n, original_item_id))
        events = vars['queries'].observations.GetVideoEvents(vars, original_item_id)
        for event in events:
            event['user_id'] = vars['id_maps']['users'][event['user_original_id']]
            event['item_id'] = vars['id_maps'][event['item_type']][event['item_original_id']]
        InsertObservedEvents(vars, events)
        i += 1
    
    # Tests
    i = 1
    n = len(vars['id_maps']['tests'].keys())
    for original_item_id in vars['id_maps']['tests'].keys():
        vars['logger'].Log(vars, "\tProcessing events for test: original ID {}".format(i, n, original_item_id))
        events = vars['queries'].observations.GetTestEvents(vars, original_item_id)
        for event in events:
            event['user_id'] = vars['id_maps']['users'][event['user_original_id']]
            event['item_id'] = vars['id_maps'][event['item_type']][event['item_original_id']]
        InsertObservedEvents(vars, events)
        i += 1
        
    # Wikis
    i = 1
    n = len(vars['id_maps']['wikis'].keys())
    for original_item_id in vars['id_maps']['wikis'].keys():
        vars['logger'].Log(vars, "\tProcessing events for wiki: original ID {}".format(i, n, original_item_id))
        events = vars['queries'].observations.GetWikiVisits(vars, original_item_id)
        for event in events:
            event['user_id'] = vars['id_maps']['users'][event['user_original_id']]
            event['item_id'] = vars['id_maps'][event['item_type']][event['item_original_id']]
        InsertObservedEvents(vars, events)
        i += 1
        
    # Indices
    i = 1
    n = len(vars['id_maps']['indices'].keys())
    for original_item_id in vars['id_maps']['indices'].keys():
        vars['logger'].Log(vars, "\tProcessing events for index: original ID {}".format(i, n, original_item_id))
        events = vars['queries'].observations.GetIndexVisits(vars, original_item_id)
        for event in events:
            event['user_id'] = vars['id_maps']['users'][event['user_original_id']]
            event['item_id'] = vars['id_maps'][event['item_type']][event['item_original_id']]
        InsertObservedEvents(vars, events)
        i += 1
        
    # Collaborations
    # ---------------
    # Events from what already exists in the collaborations table (so we don't have to redo it for the platforms separately)
    vars['logger'].Log(vars, "\tProcessing collaboration events")
    events = vars['common_queries'].GetTransformedCollaborationEvents(vars)
    InsertObservedEvents(vars, events)
    
    # If a platform has additional collaboration data aside from what was fetched above
    vars['logger'].Log(vars, "\tProcessing collaboration events not transformed previously")
    events = vars['queries'].observations.GetAdditionalCollaborationEvents(vars)
    for event in events:
        event['user_id'] = vars['id_maps']['users'][event['user_original_id']]
        if event['item_original_id'] in vars['id_maps'][event['item_type']].keys(): 
            event['item_id'] = vars['id_maps'][event['item_type']][event['item_original_id']]
        else:
            event['item_id'] = -1
        
    InsertObservedEvents(vars, events)
    
    
def InsertObservedEvents(vars, events):
    fields = {
        'observed_event_type_id': 'num',
        'user_id': 'num',
        'item_id': 'num',
        'observed_event_timestamp': 'datetime',
        'observed_event_data': 'string',
    }

    t = vars['target']
    target_db_selector = db.Selector(t['host'], t['user'], t['password'], t['port'], t['db'])
    oe_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'observed_events', fields)
    for event in events:
        oe_inserter.addRow({k: event[k] for k in fields})

    oe_inserter.insertPendingRows()