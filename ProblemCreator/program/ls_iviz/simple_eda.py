
# Author: Steven C. Dang

# Classes to generate Simple EDA

import logging
import requests
import json
from abc import ABC, abstractmethod

from bokeh.client import pull_session
from bokeh.plotting import figure
from bokeh.embed import server_session

logger = logging.getLogger(__name__)

class SimpleEDAViz(ABC):

    def __init__(self, session, dataset, resource, data_attr):
        self.workflow_session = session
        self.dataset = dataset
        self.resource = resource
        self.data_attr = data_attr
        
        self._id = None
        self.viz_doc = None
        self.viz_type = None

    @abstractmethod
    def generate(self, viz_server):
        logger.info("Generating simple eda viz")

    def as_dict(self):
        out = {'workflow_session_id': self.workflow_session._id,
                'dataset_id': self.dataset._id,
                'resource_id': self.resource.resID,
                'data_attr_id': self.data_attr.colIndex,
                'viz_doc': str(self.viz_doc)
        }
        if self._id is not None:
            out['_id'] = str(self._id),
        if self.viz_type is not None:
            out['viz_type']  = self.viz_type
        logger.debug(out)
        return out

    @staticmethod
    def from_json(data, db):
        if data['viz_type'] == "Simple Numeric EDA":
            cls = globals()['SimpleNumericEDAViz']
        elif data['viz_type'] == "Simple Categorical EDA":
            cls = globals()['SimpleCategoricalEDAViz']
        wfs = db.get_workflow_session(data['workflow_session_id'])
        ds = db.get_dataset_metadata(data['dataset_id'])
        resource = ds.get_resource(data['resource_id'])
        dattr = resource.get_column(data['data_attr_id'])

        out = cls(wfs, ds, resource, dattr)
        out.viz_type = data['viz_type']
        out.viz_doc = data['viz_doc']
        out._id = data['_id']

        return out

    def __str__(self):
        return str(self.as_dict())


class SimpleNumericEDAViz(SimpleEDAViz):

    def __init__(self, session, dataset, resource, data_attr):
        super().__init__(session, dataset, resource, data_attr)
        self.viz_type = "Simple Numeric EDA"

    def generate(self, viz_server):
        """
        viz_server - url to the viz server

        """
        logger.info("Generating Simple Numeric EDA Viz")
        logger.debug("Connecting to server at address: %s" % viz_server)

        with pull_session(url=viz_server) as session:
            # update or customize that session
            doc = session.document

            p = figure(plot_width=400, plot_height=400)

            # add a circle renderer with a size, color, and alpha
            p.circle([1, 2, 3, 4, 5], [6, 7, 2, 4, 5], size=20, color="navy", alpha=0.5)

            doc.add_root(p)
            # generate a script to load the customized session
            embed_script = server_session(session_id=session.id, url=viz_server)
            logger.debug("Got embed script for viz:\n%s" % str(embed_script))

            self.viz_doc = embed_script


class SimpleCategoricalEDAViz(SimpleEDAViz):

    def __init__(self, session, dataset, resource, data_attr):
        super().__init__(session, dataset, resource, data_attr)
        self.viz_type = "Simple Categorical EDA"

    def generate(self, viz_server):
        logger.info("Generating Simple Categorical EDA Viz")
        viz_server = viz_server.get_address()
        logger.debug("Connecting to server at address: %s" % viz_server)

        with pull_session(url=viz_server) as session:
            # update or customize that session
            doc = session.document

            p = figure(plot_width=400, plot_height=400)

            # add a square renderer with a size, color, and alpha
            p.square([5, 4, 3, 2, 1], [6, 7, 2, 4, 5], size=20, color="olive", alpha=0.5)
            
            doc.add_root(p)
            # generate a script to load the customized session
            embed_script = server_session(session_id=session.id, url=viz_server)
            logger.debug("Got embed script for viz:\n%s" % str(embed_script))

            self.viz_doc = embed_script

