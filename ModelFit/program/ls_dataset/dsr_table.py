
# Author: Steven C. Dang

# An type for table resources types that are encapsulated by a d3m remote dataset

import logging
import json

from ls_dataset.dataset_resource import DatasetResource
from ls_dataset.resource_col import ResourceColumn

logger = logging.getLogger(__name__)

class DSRTable(DatasetResource):
    """
    A type for table resources types that are encapsulated by a d3m remote dataset

    """

    def __init__(self, metadata):
        """
        inputs: 
            metadata - json object describing the d3m dataset resource

        """
        super().__init__(metadata) 
        self.columns = [ResourceColumn(col) for col in metadata['columns']]

    def to_json(self):
        out = json.loads(super().to_json())
        out['columns'] = [json.loads(rc.to_json()) for rc in self.columns]
        return json.dumps(out)
        

    def __str__(self):
        return self.to_json()
