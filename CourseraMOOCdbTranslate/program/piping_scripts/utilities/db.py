import MySQLdb as mdb
import sys
from datetime import datetime
import time

def connect(host, user, password, port, db):
    con = None
    try:
        con = mdb.connect(host, user, password, db, port=port)
        con.autocommit(True)
    except mdb.Error, e:
        print "Error %d: %s" % (e.args[0], e.args[1])
        sys.exit(1)

    cur = con.cursor(mdb.cursors.DictCursor)
    
    return (con, cur)
    
def Select(cur, query):
    x = Execute(cur, query)
    return list(x) if x != None else []
    
def Execute(cur, query):
    #print query
    cur.execute(query)
    return cur.fetchall()
    
class StaggeredInsert():
    # Convenience class for performing staggered inserts
    
    insert_at = 1000 # When this number of rows is reached, an insert statement will be generated.
    
    def __init__(self, host, user, password, port, db_name, table_name, fields):
        self.host = host
        self.user = user
        self.password = password
        self.port = port
        self.db_name = db_name
        (self.con, self.cur) = connect(host, user, password, port, db_name)
        
        self.table = table_name
        self.fields = fields
        self.field_names = fields.keys()
        self.last_query = None
        self.rows = []
        self.num_inserted_rows = 0
        
    def addRow(self, row):
        # Add a new row to the row list, and execute insert if # of rows reached self.insert_at
        self.rows.append(row)
        self.num_inserted_rows += 1
        if len(self.rows) == self.insert_at:
            self.insertPendingRows()
        
    def insertPendingRows(self):
        # Insert everything currently in self.rows
        if len(self.rows) == 0: return
        
        query = "INSERT INTO {} ({}) VALUES ".format(self.table, ",".join(self.fields))
        row_value_strings = []
        for row in self.rows:
            x = []
            for field_name in self.field_names:
                if self.fields[field_name] == 'string':
                    if field_name not in row.keys() or row[field_name] == None:
                        x.append("null")
                    else:
                        s = str(row[field_name])
                        s = mdb.escape_string(s)
                        x.append("'" + s + "'")
                elif self.fields[field_name] == 'datetime':
                    if field_name not in row.keys() or row[field_name] == None:
                        x.append("null")
                    else:
                        if not isinstance(row[field_name], datetime):
                            row[field_name] = datetime.fromtimestamp(row[field_name])
                        x.append("'" + row[field_name].isoformat(' ') + "'")
                elif self.fields[field_name] == 'ip':
                    if field_name not in row.keys() or row[field_name] == None:
                        x.append("null")
                    else:
                        ip_num = 'null'
                        if row[field_name] != None:
                            if isinstance(row[field_name], basestring) and '.' in row[field_name]:
                                ip_num = str(ip_aton(row[field_name]))
                            else:
                                ip_num = str(row[field_name])
                        x.append(ip_num)
                else:
                    if field_name not in row.keys() or row[field_name] == None:
                        x.append("null")
                    else:
                        x.append(str(row[field_name]))
                    
            row_value_strings.append("(" + ",".join(x) + ")")
            
        query += ",".join(row_value_strings) + ";"
        cur = self.con.cursor(mdb.cursors.DictCursor)
        cur.execute(query)
        cur.close()
        
        self.last_query = query
        self.rows = []
        
class Selector():

    def __init__(self, host, user, password, port, db_name):
        self.host = host
        self.user = user
        self.password = password
        self.port = port
        self.db_name = db_name
        
        self.connectToServer()
    
    def resetCursor(self):
        self.cur.close()
        self.cur = self.con.cursor(mdb.cursors.DictCursor)
        
    def connectToServer(self):
        (self.con, self.cur) = connect(self.host, self.user, self.password, self.port, self.db_name)
        
    def query(self, q):
        try:
            return Select(self.cur, q)
        except mdb.Error, e:
            if e.args[0] in [2003, 2013]:
                # Connection problem try reconnecting in 5 seconds and resending the query
                i = 1
                while 1:
                    print "Failed to query '{}'. Reattempt {} in 15 seconds".format(self.host, i)
                    time.sleep(15)
                    self.connectToServer()
                    self.query(q) 
                
        
def CloneDB(cur, source_db, target_db):
    tables = Select(cur, "SHOW TABLES IN `{}`".format(source_db))
    for table in tables:
        table_name = table.values()[0]
        Execute(cur, "CREATE TABLE `{}`.`{}` LIKE `{}`.`{}`".format(target_db, table_name, source_db, table_name))
    
def CloneTableContents(cur, source_db, target_db, tables):
    for table in tables:
        Execute(cur, "INSERT INTO `{}`.`{}` SELECT * FROM `{}`.`{}`".format(target_db, table, source_db, table))
    
def ip_aton(ip_str):
    if ip_str == '' or not ip_str:
        return 'null'
    else:
        if ',' in ip_str:
            parts = ip_str.split(",")
            ip_str = parts[0].strip()
            
        ip_str_parts = ip_str.split(".")
        try:
            octet1 = int(ip_str_parts[0])
            octet2 = int(ip_str_parts[1])
            octet3 = int(ip_str_parts[2])
            octet4 = int(ip_str_parts[3])
            ip_num = 256*256*256*octet1 + 256*256*octet2 + 256*octet3 + octet4
        except:
            print "\t\t\tMalformed ip-address:", ip_str
            return 'null'
            
        return ip_num
        