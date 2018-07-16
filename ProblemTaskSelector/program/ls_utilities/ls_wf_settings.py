
# Author: Steven C. Dang

# Convenience class and functions for supporting reading in configuration files

import logging
import configparser
import os

# logging.basicConfig()
logger = logging.getLogger(__name__)

class SettingsFactory(object):

    @staticmethod
    def get_settings(cfg_path=None, program_dir=None, working_dir=None, is_test=False):
        # Force this for now until I figure out how to pass env vars or config to module
        if True:
        # if os.environ.get(D3MSettings.__config_path_var__) is not None:
            return D3MSettings(cfg_path, program_dir, working_dir, is_test)
        else:
            return Settings(cfg_path, program_dir, working_dir, is_test)

class Settings(object):
    """
    A generic settings class including a few operators for reading in configuration files

    """
    def __init__(self, cfg_path=None, program_dir=None, working_dir=None, is_test=False):
        self.cfg_file = cfg_path
        self.cfg = configparser.ConfigParser()
        self.program_dir = program_dir
        self.working_dir = working_dir
        self.is_test = is_test

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

    def get_log_level(self):
        if self.is_test:
            return logging.DEBUG
        else:
            return logging.getLevelName(self.cfg.get('Logging', 'log_level'))

    def is_syslog_enabled(self):
        return self.cfg.getboolean('Logging', 'enable_syslog')

    def is_file_log_enabled(self):
        return self.cfg.getboolean('Logging', 'enable_file_log')

    def get_file_log_path(self):
        return os.path.join(self.working_dir)

    def get_working_dir(self):
        return self.working_dir

    def get_program_dir(self):
        return self.program_dir

    def parse_logging(self):
        """
        Parse only settings from the logging part of the config

        """
        cfg = {
            'log_level': logging.getLevelName(self.cfg.get('Logging', 'log_level')),
            'enable_syslog': self.cfg.getboolean('Logging', 'enable_syslog'),
            'enable_file_log': self.cfg.getboolean('Logging', 'enable_file_log'),
            # 'file_log_path': self.cfg.get('Logging', 'file_log_path') 
            'file_log_path': os.path.join(self.working_dir)
        }

        return cfg

    def get_dataset_path(self):
        return self.cfg.get('Dataset', 'dataset_dir')

    def get_out_path(self):
        return self.working_dir

    def get_ta2_url(self):
        return self.cfg.get('TA2', 'ta2_url')

    def get_ta2_name(self):
        return "TA2"

    def get(self, sect, key):
        """
        Manually retrieve specific configuration from settings. Basically a wrapper around
        configparser.get()

        """
        return self.cfg.get(sect, key)

    def get_mode(self):
        return "normal"


class D3MSettings(Settings):

    __config_path_var__="D3MCONFIG"

    def __init__(self, cfg_path=None, program_dir=None, working_dir=None, is_test=False):
        super().__init__(cfg_path, program_dir, working_dir, is_test=is_test)
        if 'D3MCONFIG' not in os.environ:
            self.d3m_config_file = "/datashop/workflow_components/D3M/d3m.cfg"
        else:
            self.d3m_config_file = os.environ['D3MCONFIG']
        self.d3m_cfg = configparser.ConfigParser()
        self.d3m_cfg.read(self.d3m_config_file)
    
    def parse_logging(self):
        """
        Parse only settings from the logging part of the config

        """
        cfg = super().parse_logging()
        # Override logging level settings for deployment to d3m environments
        # cfg['log_level'] =logging.INFO
        cfg['log_level'] =logging.DEBUG
        return cfg

    def get_dataset_path(self):
        return self.d3m_cfg.get('Data', 'dataset_root')

    def get_out_path(self):
        return self.d3m_cfg.get('Data', 'out_dir_root')

    def get_ta2_url(self):
        return self.d3m_cfg.get('TA2', 'ta2_url')
    
    def get_ta2_name(self):
        return self.d3m_cfg.get('TA2', 'ta2_name')
    
    def get_mode(self):
        return 'D3M'
