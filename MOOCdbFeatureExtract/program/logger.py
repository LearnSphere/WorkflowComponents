from datetime import datetime

class Logger:
    """
    attributes:
        logToConsole    whether to log to console
        logFilePath     lof file path and name
    """

    def __init__(self, logToConsole=True, logFilePath=None):
        self.logToConsole = logToConsole
        self.logFilePath = logFilePath
    
    
    def log(self, message):
        if self.logFilePath != None:
            f = open(self.logFilePath, "ab")
            f.write(str(datetime.now()) + ": " + message + "\n")
            f.close()
        
        if self.logToConsole:
            print message

