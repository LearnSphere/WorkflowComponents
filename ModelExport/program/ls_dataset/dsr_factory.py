
# Author: Steven C. Dang

# An abstract type for resources types that are encapsulated by a d3m remote dataset

import logging

from ls_dataset.dataset_resource import DatasetResource
from ls_dataset.dsr_table import DSRTable

logger = logging.getLogger(__name__)

class DatasetResourceFactory(object):
    """
    Factory class for producing the appropriate DatasetResource type given
    some metadata

    """

    @staticmethod
    def get_resource(metadata):
        """
        Initializes and returns an instance of a dataset resource with proper
        child class given a metadata dictionary

        """
        if metadata['resType'] not in DatasetResource.__resource_types__:
            logger.warning("Invalid resource type encountered: %s" % str(metadata))
            raise Exception("Invalid resource type encountered: %s" % str(metadata))
        elif metadata['resType'] == 'table':
            return DSRTable(metadata)
        else:
            return DatasetResource(metadata)
