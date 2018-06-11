
# Author: Steven C. Dang

# Convenience class and functions for supporting reading in configuration files

import logging
import configparser

# logging.basicConfig()
# logger = logging.getLogger('ls_wf_settings')

class Settings(object):
    """
    A generic settings class including a few operators for reading in configuration files

    """
    def __init__(self, cfg_path=None):
        self.cfg_file = cfg_path
        self.cfg = configparser.ConfigParser()

        if cfg_path is None:
            self.cfg.read('settings.cfg')
        else:
            self.cfg.read(cfg_path)

    def parse_config(self):
        """
        Parse a given config file

        """
        
        cfg = {
            'dataset_dir': self.cfg.get('Dataset', 'dataset_dir'),
            'dataset_json': self.cfg.get('Dataset', 'dataset_json'),
            'out_file': self.cfg.get('Dataset', 'out_file'),
            'log_level': logging.getLevelName(self.cfg.get('Logging', 'log_level')),
            'enable_syslog': self.cfg.getboolean('Logging', 'enable_syslog'),
            'enable_file_log': self.cfg.getboolean('Logging', 'enable_file_log'),
            'file_log_path': self.cfg.get('Logging', 'file_log_path') 
        }
        return cfg

    def parse_logging(self):
        """
        Parse only settings from the logging part of the config

        """
        cfg = {
            'log_level': logging.getLevelName(self.cfg.get('Logging', 'log_level')),
            'enable_syslog': self.cfg.getboolean('Logging', 'enable_syslog'),
            'enable_file_log': self.cfg.getboolean('Logging', 'enable_file_log'),
            'file_log_path': self.cfg.get('Logging', 'file_log_path') 
        }
        return cfg


    def get(self, sect, key):
        """
        Manually retrieve specific configuration from settings. Basically a wrapper around
        configparser.get()

        """
        return self.cfg.get(sect, key)


