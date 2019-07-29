
# Author: Steven C. Dang

# Classes for using TA2 for modeling tasks

import grpc
import logging
import json
import sys
from os import path
import argparse
import pprint
import csv

import pandas as pd

from google.protobuf import json_format

# Workflow component specific imports
from ls_utilities.ls_wf_settings import SettingsFactory
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.d3m_prediction import D3MPrediction
from ls_problem_desc.ls_problem import ProblemDesc
from ls_problem_desc.d3m_problem import DefaultProblemDesc
from d3m_ta2.ta2_client import TA2Client
from d3m_eval.summer_2018.prob_discovery import ProblemDiscoveryWriter
# from ls_workflow.workflow import Workflow as Solution
from modeling.models import *
from modeling.component_out import *

logger = logging.getLogger(__name__)

class ModelSearch(object):
    """
    Search for a set of models using ta2 and fit the models with the dataset

    """

    def __init__(self, db, sess, serv):
        self.db = db
        self.sess = sess
        self.serv = serv

    def run(self, ds, prob, out_path=None):

        logger.info("Running Pipeline Search on TA2")

    
        logger.debug("********************************")
        logger.debug("********************************")
        # Parse the problem and dataset to get the prediction data
        # Assume the problem has 1 input with 1 target
        if len(prob.inputs) != 1:
            logger.warning("Assumed problem has 1 input, but actually has %i inputs" % len(prob.inputs))
        pinput = prob.inputs[0]
        if len(pinput.targets) != 1:
            logger.warning("Assumed problem input has 1 target, but actually has %i inputs" % len(pinput.targets))
        ptarget = pinput.targets[0]

        # Use problem target info to retreive dataset and prediction column
        data_resource = None
        for dr in ds.dataResources:
            if dr.resID == ptarget.resource_id:
                data_resource = dr
                logger.debug("Got Data resourse\n%s" % str(data_resource))
        if data_resource is None:
            logger.error("No matching data resouce was found for info from problem\n%s" % str(ptarget))
        data_path = path.join(ds.get_ds_path(), data_resource.resPath)
        logger.debug("Got data path from dataset with ds root path: %s\t total path: %s" % (ds.get_ds_path(), data_path))
        data = pd.read_csv(data_path, sep=',')
        logger.debug(data.head())
        predictions = data.loc[:,['d3mIndex', ptarget.column_name]]
        predictions.index = predictions['d3mIndex']
        predictions.drop(labels=['d3mIndex'], axis=1, inplace=True)
        logger.debug("Just predictions data: \n%s" % str(predictions.head()))

        logger.debug("********************************")
        logger.debug("********************************")




        # Search for solutions
        # Get training dataset
        train_ds = ds.get_training_dataset()
        # if config.get_mode() == 'D3M':
        # if out_path is not None:
            # # Write search and problem to file for problem discovery task
            # logger.debug("Writing to out_dir: %s" % out_path)
            # prob_list = ProblemDiscoveryWriter(out_path)
            # search_id, request = self.serv.search_solutions(prob, train_ds, get_request=True)
            # prob_list.add_problem(prob, request)
        # else:
            # search_id, request = self.serv.search_solutions(prob, train_ds, get_request=True)
        search_id, request = self.serv.search_solutions(prob, train_ds, get_request=True)
        self.sess.search_id = search_id
        # Update session in db
        self.db.update_data_fields('wf_sessions', self.sess, ['search_id'])
        # Get search results
        soln_ids = self.serv.get_search_solutions_results(search_id)
        if soln_ids is None:
            raise Exception("No solution returned")

        
        # Get Model for each solution returned
        solns = {}
        for soln_id in soln_ids:
            solns[soln_id] = self.serv.describe_solution(soln_id)
            logger.debug("Got pipeline descripton for solution id %s: \n%s" % (soln_id, str(solns[soln_id])))

        # Add solutions to db
        for soln_id, soln in solns.items():
            solns[soln_id]._id = self.db.insert_data('solutions', soln)
        # Rewrite solns dict with db _id as key
        solns = {soln._id: soln for sid, soln in solns.items()}



        # Add soln ids to session
        soln_ids = [soln._id for soln_id, soln in solns.items()]
        self.sess.add_soln_ids(soln_ids)
        # Update session in db
        self.db.update_data_fields('wf_sessions', self.sess, ['soln_ids'])


        logger.debug("********************************")
        logger.debug("Done searching for solutions")
        logger.debug("********************************")

        # Test update to wf session
        sess = self.db.get_workflow_session(self.sess._id)
        logger.debug("recovered session: %s" % sess.to_json())

        # Testing writes of solutions to db
        for soln_id in self.sess.soln_ids:
            logger.debug("Retrieving solution from db with id: %s" % soln_id)
            obj = self.db.get_object('solutions', soln_id)
            logger.debug("Got json from db: %s" % str(obj))
            test_soln = Model.from_json(obj)
            logger.debug("got Solution instance: %s" % str(test_soln))


        ### Temp patch of writing to file and reading back in to simulate passing between components
        # out_file_name = "model_data.tsv"

        # out_file_path = path.join(out_path, out_file_name)
        # ModelSetIO.to_file(out_file_path, solns)
        # logger.info(out_file_path)
        # with open(out_file_path, 'r') as ofile:
            # m_index, models = ModelSetIO.from_file(ofile)

        # Get fitted solution
        fit_req_ids = {}
        fitted_models = {}
        fitted_results = {}
        mdl_predictions = {}
        # models = [Model.from_json(self.db.get_object('solutions', sid) for sid in self.sess.soln_ids)]
        for mid, model in solns.items():
            logger.debug("Fitting model: %s" % str(model))
            fit_req_ids[mid] = self.serv.fit_solution(model, train_ds)
        for mid, rid in fit_req_ids.items():
            logger.debug("Model id: %s\tfit model request id: %s" % (mid, rid))
            try:
                fitted_id, fitted_results = self.serv.get_fit_solution_results(rid)
            except Exception as e:
                logger.warning("Got null result for fit solution request, %s, removing from solution set" % e)
                # Removing soln from set of solns 
                logger.debug("Number of solns before deleting soln that was unable to fit: %i" % len(solns))
                del solns[mid]
                logger.debug("Number of solns after deleting soln that was unable to fit: %i" % len(solns))

            # Adding fitted model and results to db
            fitted_mdl = FittedModel(solution_id= mid, fitted_id=fitted_id, dataset_id=ds._id)
            fitted_mdl._id = self.db.insert_data('fitted_solutions', fitted_mdl)
            fitted_models[fitted_mdl._id] = fitted_mdl
            logger.debug("Added fitted model to db: %s" % str(fitted_mdl))
            # for inpt in prob.inputs:
                # for target in inpt.targets:
                    # logger.debug("Input datasetID: %s\ttarget: %s " % (inpt.dataset_id, str(target)))
            # logger.debug("Predictions to json: %s" % str(fitted_results.to_dict(orient='records')))
            predictions = ModelPredictions(dataset_id=ds._id, problem_id=prob._id, fitted_model_id=fitted_mdl._id, data=fitted_results.to_dict('list'))
            predictions._id = self.db.insert_data('predictions', predictions)
            mdl_predictions[fitted_mdl._id] = predictions
            logger.debug("Added predictions to db: %s" % str(predictions))


        valid_slns = [mdl.solution_id for mid, mdl in fitted_models.items()]
        for valid_sln in valid_slns:
            logger.debug("Got valid solution from fitted model search from soln id: %s" % valid_sln)

        # Updating workflow session with fitted model ids and prediction ids
        fitted_mdl_ids = [mdl._id for mid, mdl in fitted_models.items()]
        pred_ids = [pred._id for mid, pred in mdl_predictions.items()]
        self.sess.fitted_ids = fitted_mdl_ids
        self.sess.fitted_pred_ids = pred_ids
        # Update session in db
        self.db.update_data_fields('wf_sessions', self.sess, ['fitted_ids', 'fitted_pred_ids'])

        # Test update to wf session
        sess = self.db.get_workflow_session(self.sess._id)
        logger.debug("recovered session: %s" % sess.to_json())

        # Testing writes of fitted Models and predictions to db
        for fid in self.sess.fitted_ids:
            logger.debug("Retrieving fitted model from db with id: %s" % fid)
            obj = self.db.get_object('fitted_solutions', fid)
            logger.debug("Got json from db: %s" % str(obj))
            test_soln = FittedModel.from_json(obj)
            logger.debug("got fitted model instance from db: %s" % str(test_soln))

        # Get Model Predictions on test data
        req_ids = {}
        test_predictions = {}
        test_ds = ds.get_test_dataset()
        for fmid, fmdl in fitted_models.items():
            soln = solns[fmdl.solution_id]
            req_ids[fmid] = self.serv.produce_solution(fmdl.fitted_id, soln, test_ds)
        logger.debug("Created predict solution requests with ids: %s" % str(req_ids))
        for fmid, rid in req_ids.items():
            logger.info("Requesting predictions with request id: %s" % rid)
            try:
                fmdl_predictions = self.serv.get_produce_solution_results(rid)
                predictions = ModelPredictions(dataset_id=ds._id, problem_id=prob._id, fitted_model_id=fmid, data=fmdl_predictions.to_dict('list'))
                predictions._id = self.db.insert_data('predictions', predictions)
                test_predictions[predictions._id] = predictions
                logger.debug("Added predictions to db: %s" % str(predictions))
            except Exception as e:
                logger.warning("Encountered error while producing predictions for fitted model, %s.\n Error: %s" %
                               (fmid, str(e)))
            # solution_predictions[fsid] = self.serv.get_produce_solution_results(rid)

        tpred_ids = [pred._id for pid, pred in test_predictions.items()]
        self.sess.test_pred_ids = tpred_ids
        # Update session in db
        self.db.update_data_fields('wf_sessions', self.sess, ['test_pred_ids'])

        # Test update to wf session
        sess = self.db.get_workflow_session(self.sess._id)
        logger.debug("recovered session: %s" % sess.to_json())

        # Get Score for each solution
        score_ds = ds
        req_ids = {}
        for mid, soln in solns.items():
            req_ids[mid] = self.serv.score_solution(soln, score_ds, metrics=prob.metrics)
        model_scores = {}
        for mid in req_ids:
            results = self.serv.get_score_solution_results(req_ids[mid])
            scores = ModelScores(solns[mid]._id, [score_ds.get_schema_uri()], [Score.from_protobuf(result) for result in results])
            scores._id = self.db.insert_data('model_scores', scores)
            model_scores[scores.mid] = scores
            logger.debug("Added ModelScores to db: %s" % str(scores))

        score_ids = [score._id for sid, score in model_scores.items()]
        self.sess.score_ids = score_ids
        # Update session in db
        self.db.update_data_fields('wf_sessions', self.sess, ['score_ids', 'state'])

        # Test update to wf session
        sess = self.db.get_workflow_session(self.sess._id)
        logger.debug("recovered session: %s" % sess.to_json())


        ### Parse through model scores to get dataframe of scores
        # Determine number of scores to  plot:
        mid = list(solns)[0]
        sample_scores = len(model_scores[mid].scores)
        score_data = {score.metric.type: [] for score in model_scores[mid].scores}
        score_data['model_id'] = []
        # score_data['model_num'] = []
        metrics = [score.metric.type for score in model_scores[mid].scores]
        # score_data['index'] = [ms.mid for , mid, ms in model_scores]

        soln_ids = list(solns)
        for mid in model_scores:
            score_set = model_scores[mid]
            logger.debug("Adding score data for model with id: %s" % score_set.mid)
            score_data['model_id'].append(mid)
            # score_data['model_num'].append(soln_ids.index(mid))
            for score in score_set.scores:
                metric_val = list(score.value.value.values())[0]
                logger.debug("appending score for metric: %s\tvalue: %s" % 
                        (score.metric.type, metric_val))
                logger.debug("Score value tyep: %s" % type(metric_val))
                score_data[score.metric.type].append(metric_val)
                
        # logger.debug("###############################################")
        # logger.debug("Score_data keys: %s" % str([key for key in score_data.keys()]))
        data = pd.DataFrame(score_data)
        logger.debug("Score data with solution ids: %s" % str(data))

        # Sort models by metricA
        ordering = Metric.get_metric_order(metrics[0])
        if ordering.lower() == 'higher_is_better':
            logger.info("Sorting models in descending order")
            sorted_data = data.sort_values(by=[metrics[0]], ascending=False)
        elif ordering.lower() == "lower_is_better":
            logger.info("Sorting models in ascending order")
            sorted_data = data.sort_values(by=[metrics[0]], ascending=True)
        else:
            logger.warning("'%s' ordering given. Using ascending order by default." % ordering)
            sorted_data = data.sort_values(by=[metrics[0]], ascending=True)
        sorted_data['rank'] = range(1, sorted_data.shape[0] + 1)

        logger.debug("###############################################")
        logger.debug(sorted_data.columns)
        logger.debug("#############")
        logger.debug(sorted_data.shape)
        logger.debug("#############")
        logger.debug(sorted_data.head())
        logger.debug("#############")
        logger.debug(sorted_data[metrics[0]])
        logger.debug("#############")
        logger.debug(sorted_data['model_id'])
        logger.debug("#############")
        logger.debug(sorted_data['rank'])
        logger.debug("###############################################")
        # create ranked model list 
        # Write ranked models to db
        rm_ids = []
        for row in sorted_data.iterrows():
            rm = RankedModel(
                    mdl=solns[row[1]['model_id']],
                    rank=row[1]['rank']
            )
            rm._id = self.db.insert_data('ranked_models', rm)
            logger.debug("Added ranked model to db: %s" % str(rm))
            rm_ids.append(rm._id)

       # Update session in db
        self.sess.ranked_mdl_ids = rm_ids
        self.sess.set_state_complete()
        self.db.update_data_fields('wf_sessions', self.sess, ['ranked_mdl_ids', 'state'])
        
        # Test update to wf session
        sess = self.db.get_workflow_session(self.sess._id)
        logger.debug("recovered session: %s" % sess.to_json())

        
        
        # return m_index, models, result_df, score_data, ranked_models
    
class ModelRanker(object):
    """
    Rank a set of models based on scores along a particular metric

    """
    
    def run(self, models, m_index, ds, metric, ordering, serv):
        ### Begin Script ###
        logger.info("Scoring models with selected metric")

        # Get Score for each solution
        req_ids = {}
        for mid in models:
            model = models[mid]
            req_ids[mid] = serv.score_solution(model, ds, metrics=[metric])
        scores = {}
        for mid in req_ids:
            results = serv.get_score_solution_results(req_ids[mid])
            scores[mid] = ModelScores(models[mid].id, [ds.get_schema_uri()], [Score.from_protobuf(result) for result in results])


        ### Parse through model scores to get dataframe of scores
        # Determine number of scores to  plot:
        sample_scores = len(scores[m_index[0]].scores)
        score_data = {score.metric.type: [] for score in scores[m_index[0]].scores}
        score_data['model_id'] = []
        score_data['model_num'] = []
        metrics = [score.metric.type for score in scores[m_index[0]].scores]
        score_data['index'] = range(len(m_index))

        for mid in scores:
            score_set = scores[mid]
            logger.debug("Adding score data for model with id: %s" % score_set.mid)
            score_data['model_id'].append(mid)
            score_data['model_num'].append(m_index.index(mid))
            for score in score_set.scores:
                metric_val = list(score.value.value.values())[0]
                logger.debug("appending score for metric: %s\tvalue: %s" % 
                        (score.metric.type, metric_val))
                logger.debug("Score value tyep: %s" % type(metric_val))
                score_data[score.metric.type].append(metric_val)
                
        logger.debug("###############################################")
        logger.debug("Score_data keys: %s" % str([key for key in score_data.keys()]))
        logger.debug("Score_data model_id: %s" % str(score_data['model_id']))
        logger.debug("Score_data model_num: %s" % str(score_data['model_num']))
        logger.debug("Score_data model_index: %s" % str(score_data['index']))
        logger.debug("Score_data metric: %s" % str(score_data[metrics[0]]))
        data = pd.DataFrame(score_data)
        logger.debug("Converted Score data to dataframe: %s" % str(data.head(20)))
        logger.debug("###############################################")
        logger.debug("###############################################")
        logger.debug("###############################################")
        logger.debug(data.columns)
        logger.debug("#############")
        logger.debug(data.shape)
        logger.debug("#############")
        logger.debug(data.head())
        logger.debug("###############################################")

        # Sort models by metric
        if ordering.lower() == 'higher_is_better':
            logger.info("Sorting models in descending order")
            sorted_data = data.sort_values(by=[metrics[0]], ascending=False)
        elif ordering.lower() == "lower_is_better":
            logger.info("Sorting models in ascending order")
            sorted_data = data.sort_values(by=[metrics[0]], ascending=True)
        else:
            logger.warning("'%s' ordering given. Using ascending order by default." % ordering)
            sorted_data = data.sort_values(by=[metrics[0]], ascending=True)
        sorted_data['rank'] = range(1, sorted_data.shape[0] + 1)

        logger.debug("###############################################")
        logger.debug(sorted_data.columns)
        logger.debug("#############")
        logger.debug(sorted_data.shape)
        logger.debug("#############")
        logger.debug(sorted_data.head())
        logger.debug("#############")
        logger.debug(sorted_data[metrics[0]])
        logger.debug("#############")
        logger.debug(sorted_data['model_id'])
        logger.debug("#############")
        logger.debug(sorted_data['model_num'])
        logger.debug("#############")
        logger.debug(sorted_data['index'])
        logger.debug("#############")
        logger.debug(sorted_data['rank'])
        logger.debug("###############################################")
        logger.debug("###############################################")
        logger.debug("###############################################")
        logger.debug(sorted_data[['rank', metrics[0], 'index', 'model_num']])
        logger.debug("###############################################")

        # create ranked model list 
        ranked_models = {
                row[1]['model_id']: RankedModel(
                    mdl=models[row[1]['model_id']],
                    rank=row[1]['rank']
                ) for row in sorted_data.iterrows()
        }
        
        return ranked_models

class ModelExporter(object):

    def run(self, out_path, ranked_models, serv):
        #Create model writer 
        logger.debug("Writing Ranked models to out_dir: %s" % out_path)
        # model_writer = RankedPipelineWriter(out_path)
        # model_writer.write_ranked_models(ranked_models)

        for rmodel in ranked_models:
            logger.info("Exporting model via TA2 with id: %s\t and rank: %s" % (rmodel.mdl.id, rmodel.rank))
            serv.export_solution(rmodel.mdl, rmodel.mdl.id, rmodel.rank)


