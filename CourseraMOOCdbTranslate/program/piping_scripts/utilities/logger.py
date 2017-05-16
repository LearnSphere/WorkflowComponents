def Log(vars, message):
    if vars['log_file'] != None:
        f = open(vars['log_file'], "ab")
        f.write(message + "\n-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n")
        f.close()
        
    if vars['options']['log_to_console']:
        print message