
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

    @staticmethod
    def get_dx_settings():
        cfg_file_name = "docker_config.cfg"
        if 'D3MCONFIG' not in os.environ:
            config_file = os.path.join("/datashop/workflow_components/D3M", cfg_file_name)
        else:
            config_file = os.path.join(os.path.dirname(os.environ['D3MCONFIG']),
                    "docker_config.cfg")
        return AppServiceSettings(config_file)

    def get_env_settings():
        return EnvServiceSettings()

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
            return logging.getLevelName(self.cfg.get('Logging', 'log_level'))
        else:
            return logging.getLevelName(self.cfg.get('Logging', 'log_level'))

    def is_syslog_enabled(self):
        return self.cfg.getboolean('Logging', 'enable_syslog')

    def is_file_log_enabled(self):
        return self.cfg.getboolean('Logging', 'enable_file_log')

    def get_file_log_path(self):
        if self.working_dir is None:
            return os.getcwd()
        else:
            return os.path.join(self.working_dir)

    def get_working_dir(self):
        if self.working_dir is None:
            return os.getcwd()
        else:
            return self.working_dir

    def get_program_dir(self):
        if self.program_dir is None:
            return os.getcwd()
        else:
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
        try:
            name = self.d3m_cfg.get('TA2', 'ta2_name')
            return name
        except:
            logger.warning("No name provided, using name = ''")
            return ''
    
    def get_mode(self):
        return 'D3M'

class AppServiceSettings(object):
    """
    A Settings class for reading configuration files for the D3M Inquiry service

    """
    def __init__(self, cfg_path=None):
        self.cfg_file = cfg_path
        self.cfg = configparser.ConfigParser()
        if cfg_path is None:
            self.cfg.read('/datashop/workflow_components/D3M/docker_config.cfg')
        else:
            self.cfg.read(cfg_path)

    def get_service_url(self):
        host = self.cfg.get("backend", "HOST_URL")
        return "http://%s" % host

    def get_db_backend_url(self):
        return self.cfg.get("db", "HOST_URL")

    def get_dexplorer_url(self):
        return self.cfg.get("frontend", "EXTERNAL_URL")

    def get_viz_server_url(self):
        return "http://%s" % self.cfg.get("viz", "HOST_URL")

class EnvServiceSettings(object):
    """
    A Settings class for reading settings from environment variables

    """
    def __init__(self):
        self.settings = {}

    def get_db_addr(self):
        if "db_addr" in self.settings:
            return self.settings['db_addr']
        else:
            if 'DBADDR' not in os.environ:
                raise Exception("No DB Address found")
            else:
                addr = os.environ['DBADDR']
                if 'DBPORT' in os.environ:
                    port = os.environ['DBPORT']
                    addr = addr + ":" + port
                self.settings['db_addr'] = addr
                return addr
                
    def get_viz_addr(self):
        if "viz_addr" in self.settings:
            return self.settings['viz_addr']
        else:
            if 'VIZADDR' not in os.environ:
                raise Exception("No Viz Address found")
            else:
                addr = os.environ['VIZADDR']
                if 'VIZPORT' in os.environ:
                    port = os.environ['VIZPORT']
                    addr = addr + ":" + port
                self.settings['viz_addr'] = addr
                return addr

    def get_frontend_addr(self):
        if "frontend_addr" in self.settings:
            return self.settings['frontend_addr']
        else:
            if 'FRONTENDADDR' not in os.environ:
                raise Exception("No Frontend Address found")
            addr = os.environ['FRONTENDADDR']
            if 'FRONTENDPORT' in os.environ:
                port = os.environ['FRONTENDPORT']
                addr = addr + ":" + port
            self.settings['frontend_addr'] = addr
            return addr

                
    def get_backend_addr(self):
        if "backend_addr" in self.settings:
            return self.settings['backend_addr']
        else:
            if 'BACKENDADDR' not in os.environ:
                raise Exception("No Backend Address found")
            addr = os.environ['BACKENDADDR']
            if 'BACKENDPORT' in os.environ:
                port = os.environ['BACKENDPORT']
                addr = addr + ":" + port
            self.settings['backend_addr'] = addr
            return addr

    def get_ta2_addr(self):
        if "ta2_addr" in self.settings:
            return self.settings['ta2_addr']
        else:
            if 'TA2ADDR' not in os.environ:
                raise Exception("No TA2 Address found")
            addr = os.environ['TA2ADDR']
            if 'TA2PORT' in os.environ:
                port = os.environ['TA2PORT']
                addr = addr + ":" + port
            self.settings['ta2_addr'] = addr
            return addr

    def get_tigris_addr(self):
        if "tigris_addr" in self.settings:
            return self.settings['tigris_addr']
        else:
            if 'TIGRISADDR' not in os.environ:
                raise Exception("No Tigris Address found")
            addr = os.environ['TIGRISADDR']
            if 'TIGRISPORT' in os.environ:
                port = os.environ['TIGRISPORT']
                addr = addr + ":" + port
            self.settings['tigris_addr'] = addr
            return addr

           
    def get_ta2_name(self):
        if "ta2_name" in self.settings:
            return self.settings['ta2_name']
        else:
            if 'TA2NAME' not in os.environ:
                name = "Unknown TA2"
            else:
                name = os.environ['TA2NAME']
            self.settings['ta2_name'] = name
            return name
    
    def get_out_path(self):
        if 'D3MOUTPUTDIR' in os.environ:
            name = os.environ['D3MOUTPUTDIR']
        else:
            # Default path
            name = "/output"
        return name

    def get_dataset_path(self):
        if 'D3MOUTPUTDIR' in os.environ:
            name = os.environ['D3MINPUTDIR']
        else:
            # Default path
            name = "/input"
        return name
