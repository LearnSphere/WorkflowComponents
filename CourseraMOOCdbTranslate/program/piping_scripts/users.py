from utilities import db, sanity_checks

def TransformUserData(vars):
    # DB connections
    # --------------
    c = vars['core']
    core_db_selector = db.Selector(c['host'], c['user'], c['password'], c['port'], c['db'])
    
    # Populate the users table
    user_id_map = {}
    users = vars['queries'].GetUsers(vars)
    
    fields = {
        'user_id': 'num',
        'user_email': 'string',
        'user_type_id': 'num',
        'user_join_timestamp': 'datetime',
        'user_ip': 'ip',
        'user_country': 'string',
        'user_timezone_offset': 'num',
        'user_final_grade': 'num',
    }
    
    # IP-country lookup table
    ip_country_rows = [{'start': int(x['ip_numeric_start']), 'stop': int(x['ip_numeric_stop']), 'country_code': x['country_code']} for x in core_db_selector.query("SELECT ip_numeric_start,ip_numeric_stop,country_code FROM ip_country ORDER BY ip_numeric_start")]
    
    t = vars['target']
    user_inserter = db.StaggeredInsert(t['host'], t['user'], t['password'], t['port'], t['db'], 'users', fields)
    moocdb_user_id = 1
    for user in users:
        # User MOOCdb ID
        user['user_id'] = moocdb_user_id
        
        # User IP
        user['user_ip'] = db.ip_aton(user['user_ip'])
        
        # User email cannot be null
        if 'user_email' not in user.keys() or user['user_email'] == None: user['user_email'] = ''
        
        # User country
        if 'user_country' not in user.keys(): user['user_country'] = None
        if user['user_country'] == None and user['user_ip'] != 'null': # Note: Some platforms don't record IP, but do record country
            for ipc_row in ip_country_rows:
                if user['user_ip'] >= ipc_row['start'] and user['user_ip'] <= ipc_row['stop']:
                    user['user_country'] = ipc_row['country_code']
                    break
                
        # User timezone offset
        # We are computing it as the mean for the country since some platforms provide incorrect data for user timezone
        utzo = None
        if user['user_country'] != None:
            r = core_db_selector.query("SELECT * FROM timezone WHERE country_code='{}'".format(user['user_country']))
            if len(r) > 0:
                offsets = [x['gmt_offset'] for x in r]
                utzo = offsets[len(offsets)/2]
        user['user_timezone_offset'] = utzo
        
        user_inserter.addRow({k: user[k] if k in user.keys() else None for k in fields})
        
        user_id_map[user['original_id']] = moocdb_user_id
        moocdb_user_id += 1
        
    user_inserter.insertPendingRows()
    
    vars["logger"].Log(vars, "Counts: Inserted {} users to target".format(user_inserter.num_inserted_rows))
    
    return user_id_map
    