
# Author: Steven C. Dang

# An type for table resources types that are encapsulated by a d3m remote dataset

import logging
import json

logger = logging.getLogger(__name__)

class ResourceColumn(object):
    """
    Metadata for a column for a given tabular dataset resource

    """

    __col_types__ = ['boolean',
                      'integer',
                      'real',
                      'string',
                      'categorical',
                      'dateTime',
                      'realVector',
                      'json',
                      'geojson',
                          ]

    __col_roles__ = ['index',
                     'key',
                     'attribute',
                     'suggestedTarget',
                     'timeIndicator',
                     'locationIndicator',
                     'boundaryIndicator',
                     'instanceWeight',
                     'boundingBox'
                     ]


    def __init__(self, metadata):
        """
        Inputs:
            metadata - the metadata dictionary describing the resource column

        """
        logger = logging.getLogger(__name__)
        self.colIndex = metadata['colIndex']
        self.colName = metadata['colName']

        if 'colDescription' in metadata:
            self.colDescription = metadata['colDescription']
        else:
            self.colDescription = None

        if metadata['colType'] in self.__col_types__:
            self.colType = metadata['colType']
        else:
            self.colType = metadata['colType']
            logger.warning("ingesting unsupported dataset column type: %s" % str(metadata['colType']))
        
        # Raise a flag if unsupported roles are ingested, but dont error out
        for role in metadata['role']:
            if role not in self.__col_roles__:
                logger.warning("ingesting unsupported dataset column role: %s" % str(metadata['role']))
        self.role = metadata['role']
        
        if 'refersTo' in metadata:
            self.refersTo = metadata['refersTo']
        else:
            self.refersTo = None

    def to_json(self):
        result = {
            'colIndex': self.colIndex,
            'colName': self.colName,
            'colType': self.colType,
            'role': self.role
        }

        if self.colDescription is not None:
            result['colDescription'] = self.colDescription

        if self.refersTo is not None:
            result['refersTo'] = self.refersTo

        return json.dumps(result)


    def __str__(self):
        return str(self.to_json())
