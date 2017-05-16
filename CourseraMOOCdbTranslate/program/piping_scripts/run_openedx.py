'''
To run:
cd <parent of the directory containing this file>
python -m piping_scripts.run_openedx
'''

from main import *

vars = {
    'source': {
        'platform_format': 'openedx',
        'course_org': '',
        'course_id_1': '',
        'course_id_2': '',
        'lms_host': '',
        'lms_user': '',
        'lms_password': '',
        'lms_port': 3306,
        'lms_db': '',
        'tracklog_host': '',
        'tracklog_user': '',
        'tracklog_password': '',
        'tracklog_db': '',
        'tracklog_port': 3306,
        'cms_host': '',
        'cms_user': '',
        'cms_password': '',
        'cms_port': 27017,
        'modulestore_db': '',
        'forum_contents_db': '',
        'forum_subscriptions_db': '',
        'forum_users_db': '',
    },
    
    'core': {
        'host': '',
        'user': '',
        'password': '',
        'port': 3306,
        'db': '',
    },
    
    'target': {
        'host': '',
        'user': '',
        'password': '',
        'port': 3306,
        'db': '',
        'clean_db': '',
    },
    
    'options': {
        'anonymize_output': False,
        'log_path': None,
        'log_to_console': True,
        'debug': False,
        'num_students_debug_mode': 100,
    },
}

main(vars)