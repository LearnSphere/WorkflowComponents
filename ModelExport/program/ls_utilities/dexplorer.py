
# Author: Steven C. Dang

# Interface for Data Explorer Backend Services

import logging
import requests
from dxdb.workflow_session import WorkflowSession, SimpleEDASession
from ls_iviz.simple_eda import *

logger = logging.getLogger(__name__)

class Dexplorer(object):

    def __init__(self, url):
        self.url = url


    def get_eda_url(self, sid):
        return self.url + "/testbokeh1/" + str(sid)

        
class DexplorerUIServer(object):

    def __init__(self, url):
        # url # no longer aboslute because services are hidden behind proxy
        if url.startswith("/"):
            self.url = url
        else:
            self.url = "/" + url

    def get_simple_eda_ui_url(self, wfs):
        return self.url + "/eda" + "/%s" % (wfs._id)

    def get_dataset_importer_ui_url(self, wfs):
        return self.url + "/datasetimporter" + "/%s" % (wfs._id)

    def get_problem_creator_ui_url(self, wfs):
        return self.url + "/problemcreator" + "/%s" % (wfs._id)

    def get_model_search_ui_url(self, wfs):
        return self.url + "/model/quicksearch" + "/%s" % (wfs._id)


class VizServer(object):

    def __init__(self, url):
        self.url = url

    def get_address(self):
        # return "http://" + self.url
        return self.url


class VizFactory(object):

    def __init__(self, viz_server, wf_session):
        self.viz_server = viz_server
        self.workflow_session = wf_session

    def generate_simple_eda_viz(self, dataset, resource, data_attr):
        viz = None
        if not isinstance(self.workflow_session, SimpleEDASession):
            logger.warning("Generating simple eda viz without simple eda workflow session")
        else:
            if data_attr.colType == 'boolean':
                logger.info("Generating SimpleBooleanEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))

            elif data_attr.colType == 'integer':
                logger.info("Generating SimpleIntEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))
                viz = SimpleNumericEDAViz(self.workflow_session, dataset, resource, data_attr)

            elif data_attr.colType == 'real':
                logger.info("Generating SimpleRealEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))

            elif data_attr.colType == 'string':
                logger.info("Generating SimpleStringEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))

            elif data_attr.colType == 'categorical':
                logger.info("Generating SimpleCategoryEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))
                viz = SimpleNumericEDAViz(self.workflow_session, dataset, resource, data_attr)

            elif data_attr.colType == 'dateTime':
                logger.info("Generating SimpleDateTimeEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))

            elif data_attr.colType == 'realVector':
                logger.info("Generating SimpleRealVecEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))

            elif data_attr.colType == 'json':
                logger.info("Generating SimpleJsonEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))

            elif data_attr.colType == 'geojson':
                logger.info("Generating SimpleGeoJsonEDAViz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))
            else:
                logger.warning("Unsupported Data type for EDA Viz for dataset, %s, resource id: %s, data attribute: %s" % (dataset.name, resource.resID, data_attr.colName))
                return None
            if viz is not None:
                viz.generate(self.viz_server.get_address())
            return viz




