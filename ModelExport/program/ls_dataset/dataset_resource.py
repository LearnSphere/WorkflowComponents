
# Author: Steven C. Dang

# An abstract type for resources types that are encapsulated by a d3m remote dataset

import logging
import json

logger = logging.getLogger(__name__)

class DatasetResource(object):
    """
    Abstract type for resources types that are encapsulated by a d3m remote dataset

    """
    __resource_types__ = ['image',
                          'video',
                          'audio',
                          'speech',
                          'text',
                          'graph',
                          'table',
                          'timeseries',
                          'edgeList',
                          'raw',
                          ]

    def __init__(self, metadata):
        """
        inputs: 
            metadata - json object describing the d3m dataset resource

        """
        logger.debug("Initializing DatasetResource with metadata: %s" % str(metadata))
        self.resID = metadata['resID']
        self.resPath = metadata['resPath']
        # placeholder logic for more custom type handling
        if metadata['resType'] in self.__resource_types__:
            self.resType = metadata['resType']
        else:
            logger.warning("Invalid resource type encountered: %s" % str(metadata['resType']))
            self.resType = metadata['resType']
        self.resFormat = metadata['resFormat']
        self.isCollection = metadata['isCollection']


    def to_json(self):
        out = {
            'resID': self.resID,
            'resPath': self.resPath,
            'resType': self.resType,
            'resFormat': self.resFormat,
            'isCollection': self.isCollection
        }

        return json.dumps(out)
        

    def __str__(self):
        return self.to_json()
