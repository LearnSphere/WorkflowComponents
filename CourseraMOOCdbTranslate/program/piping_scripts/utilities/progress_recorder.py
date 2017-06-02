import os, json

class ProgressRecorder():

    def __init__(self, vars):
        progress_file = vars['log_path'] + "/progress_" + vars["task_static_id"] + ".json"
        self.progress_file = progress_file
        
        if not vars['options']['resume_using_progress_file'] or not os.path.exists(progress_file):
            self.progress_state = {}
            f = open(progress_file, "wb")
            f.write(json.dumps(self.progress_state))
            f.close()
            
        else:
            f = open(progress_file, "rb")
            self.progress_state = json.loads(f.read())
            f.close()
            
        