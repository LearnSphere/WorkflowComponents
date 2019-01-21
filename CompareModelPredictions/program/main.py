
# Author: Steven C. Dang

# Main script for generating a confusion matrix viz taking in a d3m dataset and a prediction


import logging
import sys
from os import path
import os
import argparse
import itertools
from shutil import copytree, rmtree, copyfile

import pandas as pd
import numpy as np
# import matplotlib
# matplotlib.use("agg")
# import matplotlib.pyplot as plt

from plotly import tools
import plotly as py
import plotly.graph_objs as go

from sklearn.metrics import confusion_matrix

from jinja2 import Environment, PackageLoader, select_autoescape

from pandas.api.types import is_string_dtype
from pandas.api.types import is_numeric_dtype

# Workflow component specific imports
from ls_utilities.ls_logging import setup_logging
from ls_utilities.cmd_parser import get_default_arg_parser
from ls_utilities.ls_wf_settings import SettingsFactory
from ls_utilities.ls_wf_settings import *
from ls_dataset.d3m_dataset import D3MDataset
from ls_dataset.d3m_prediction import D3MPrediction
from ls_problem_desc.ls_problem import ProblemDesc
from ls_problem_desc.d3m_problem import DefaultProblemDesc

from modeling.models import *
from modeling.component_out import *

__version__ = '0.1'


class LS_Path_Factory(object):

    def __init__(self, workingDir, programDir):
        self.workingDir = workingDir
        self.programDir = programDir

    def get_out_path(self, fpath):
        return path.join(self.workingDir, fpath)

    def get_hosted_path(self, fpath):
        return "LearnSphere?htmlPath=" + self.get_out_path(fpath)

# def plot_confusion_matrix(cm, classes,
                          # normalize=False,
                          # title='Confusion matrix',
                          # cmap=plt.cm.Blues):
    # """
    # This function prints and plots the confusion matrix.
    # Normalization can be applied by setting `normalize=True`.
    # """
    # if normalize:
        # cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
        # print("Normalized confusion matrix")
    # else:
        # print('Confusion matrix, without normalization')

    # print(cm)

    # plt.imshow(cm, interpolation='nearest', cmap=cmap)
    # plt.title(title)
    # plt.colorbar()
    # tick_marks = np.arange(len(classes))
    # plt.xticks(tick_marks, classes, rotation=45)
    # plt.yticks(tick_marks, classes)

    # fmt = '.2f' if normalize else 'd'
    # thresh = cm.max() / 2.
    # for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
            # plt.text(j, i, format(cm[i, j], fmt),
                    # horizontalalignment="center",
                    # color="white" if cm[i, j] > thresh else "black")

    # plt.tight_layout()
    # plt.ylabel('True label')
    # plt.xlabel('Predicted label')


if __name__ == '__main__':
    # Parse argumennts
    parser = get_default_arg_parser("D3M Compare Model Predictions")
    parser.add_argument('-file0', type=argparse.FileType('r'),
                       help='the dataset json provided for the search')
    parser.add_argument('-file1', type=argparse.FileType('r'),
                       help='the problem json provided for the search')
    parser.add_argument('-file2', type=argparse.FileType('r'),
                       help='at tab-delimited list of the fitted models')
    parser.add_argument('-file3', type=argparse.FileType('r'),
                       help='the csv of a data predictions dataframe')
    args = parser.parse_args()

    if args.is_test is not None:
        is_test = args.is_test == 1
    else:
        is_test = False

    # Get config file
    config = SettingsFactory.get_settings(path.join(args.programDir, 'program', 'settings.cfg'), 
                                          program_dir=args.programDir,
                                          working_dir=args.workingDir,
                                          is_test=is_test)
    # Setup Logging
    setup_logging(config)
    logger = logging.getLogger('d3m_vis_compare_predictions')

    ### Begin Script ###
    logger.info("Generating a series of confusion matrix or scatter plots to visualize model prediction errors")
    logger.debug("Running D3M Compare Model Predictions with arguments: %s" % str(args))

    # Open dataset json
    ds = D3MDataset.from_component_out_file(args.file0)
    logger.debug("Dataset json parse: %s" % str(ds))

    # Get the Problem Doc to forulate the Pipeline request
    logger.debug("Problem input: %s" % args.file1)
    prob = ProblemDesc.from_file(args.file1)
    # logger.debug("Got Problem Description: %s" % (prob.print()))

    # Import all the fitted models
    logger.debug("Fitted Model file input: %s" % args.file2)
    m_index, fitted_models, models = FittedModelSetIO.from_file(args.file2)

    # Import the predictions data
    logger.debug("Predicions data file input: %s" % args.file3)
    pred_data = pd.read_csv(args.file3, sep='\t', index_col=0)
    logger.debug("Prediction data: %s" % str(pred_data.head()))
    logger.debug("Prediction data: %s" % str(pred_data.head()))

    # Determine types of plots to generate
    # Get problem target
    if len(prob.inputs) != 1:
        logger.warning("Assumed problem has 1 input, but actually has %i inputs" % len(prob.inputs))
    pinput = prob.inputs[0]
    if len(pinput.targets) != 1:
        logger.warning("Assumed problem input has 1 target, but actually has %i inputs" % len(pinput.targets))
    ptarget = pinput.targets[0]
   
    # Get info about target from dataset json
 
    data_resource = None
    for dr in ds.dataResources:
        if dr.resID == ptarget.resource_id:
            data_resource = dr
            logger.debug("Got Data resourse\n%s" % str(data_resource))
    if data_resource is None:
        logger.error("No matching data resouce was found for info from problem\n%s" % str(ptarget))
    # Get column info
    target_col = None
    for col in dr.columns:
        logger.debug("Comparing to columns: %s" % str(col))
        if col.colIndex == ptarget.column_index:
            logger.debug("Found matching for %s column with %s column" % (ptarget.column_name, col.colName))
            target_col = col	

    logger.debug("Target col: %s" % str(target_col))
    coltype = target_col.colType
    plot_type = None
    if any([coltype.lower() == ctype for ctype in ['integer', 'real']]):
        logger.info("Data is numeric, using scatter plots")
        plot_type = "scatter"
    else:
        logger.info("Data is not numeric. Using confusion matric")
        plot_type = "confusion matrix"

    # Setup subplots
    num_plots = pred_data.shape[1] - 2
    fig = tools.make_subplots(rows=num_plots, cols=1)
    plots = []

    # Iterate over columns of predictions
    logger.debug("Columns: %s" % str(pred_data.columns))
    truth_col = pred_data.columns[1]
    np.set_printoptions(precision=2)
    for i, col in enumerate(pred_data.columns[2:]):
        logger.info("Generating plot for columns:\t %s" % col)
	
	# pdata = pred_data.loc[:,truth_col + [col]]
    if plot_type == "scatter":
        fig.append_trace(go.Scatter(
        x=pred_data.loc[:,truth_col],
        y=pred_data.loc[:,col],
        mode='markers'),
        i + 1, 1)
    else:
        # Compute confusion matrix
        data_labels = pred_data[truth_col].unique()
        cm = confusion_matrix(pred_data[truth_col], pred_data[col], 
                labels=data_labels)
        logger.debug("Confusion Matrix: %s" % str(cm))
        logger.debug("Data Labels: %s" % str(data_labels))
        fig.append_trace(go.Heatmap(z=cm),
            i + 1, 1)   
		




    # logger.debug("############################################")
    # # Import data csv into pandas dataframe
    # for dr in ds.dataResources:
        # if dr.resType == 'table':
            # dspath = path.join(ds.get_ds_path(), dr.resPath)
            # logger.debug("Getting data csv: %s" % dspath)
            # data = pd.read_csv(dspath, ',', index_col=0)
            # # logger.debug(data.head())
            # # logger.debug(data.shape)
            # break

    # # Import prediction result csv into pandas dataframe
    # ppath = path.join(ds.ppath, ds.pfiles[0])
    # logger.debug("Importing prediction data at: %s" % ppath)
    # pdata = pd.read_csv(ppath, ',', index_col=0)
    # pcol = pdata.columns[0]
    # pdata.rename(columns={pcol: pcol + "_pred"}, inplace=True)

    # # Merge data and prediciton csv
    # merged = data.merge(pdata, how='left', left_index=True, right_index=True)
    # logger.debug("Merged data: %s" % str(merged.shape))

    
    # Isolate prediction and predicted column with id
    # logger.debug(merged.loc[:, [pcol, pcol + "_pred"]].head())
    # out_data = merged.loc[:, [pcol, pcol + "_pred"]]

    ### Create Confusion Matrix ###
    # cm = confusion_matrix(out_data[pcol], out_data[pcol + '_pred'])
    # np.set_printoptions(precision=2)

    # Plot confusion Matrix
    # fig = plt.figure()
    # class_names = out_data[pcol].unique()
    # plot_confusion_matrix(cm, classes=class_names, normalize=True, title='Normalized confusion matrix')

    # Copy support library files to output directory
    # srcdir = path.join(args.programDir, 'program', 'html','lib')
    # outdir = path.join(args.workingDir, 'lib')
    # logger.debug("Copying files from %s" % srcdir)
    # if path.isdir(outdir):
	# rmtree(outdir)
    # copytree(srcdir, outdir)

    # Copy support html documents to output file
    # srcdir = path.join(args.programDir, 'program', 'html','resources')
    # outdir = path.join(args.workingDir, 'resources')
    # logger.debug("Copying files from %s" % srcdir)
    # if path.isdir(outdir):
	# rmtree(outdir)
    # copytree(srcdir, outdir)

    # Write data to output directory
    # out_file = path.join(args.workingDir, 'resources','data', 'data.csv')
    # out_data.to_csv(out_file,sep=',')

    # Write plot to file
    # fig_path = path.join(args.workingDir, 'resources', 'plot.png')
    # fig.savefig(fig_path)

    # Generate html from template and write to output file
    # path_factory = LS_Path_Factory(args.workingDir, args.programDir)
    # env = Environment(
	# loader=PackageLoader("ls_iviz", "templates"),
	# autoescape=select_autoescape(['html'])
    # )
    # template_info = {
	# 'resource_path': path_factory.get_hosted_path(
	    # path.join('resources')),
	# 'viz_img_path': path_factory.get_hosted_path(
	    # path.join('resources', 'plot.png')),
	# 'raw_data': cm.tolist(),
	# 'data_classes': str([str(cls) for cls in class_names]),
	# 'd3_dashboard_css': env.get_template('dashboard.css').render(),
	# 'component_css': env.get_template('confusion_matrix.css').render(),
	# 'component_js': env.get_template('confusion_matrix.js').render(),
    # }
    # viz_template = env.get_template("confusion_matrix.html")
    # out_file_path = path.join(args.workingDir, 
			      # config.get('Output', 'out_file')
			      # )
    # logger.info("Writing output html to: %s" % out_file_path)
    # with open(out_file_path, 'w') as out_file:
	# out_file.write(viz_template.render(template_info))
    

    # Get  html to output file path
    out_file_path = path.join(args.workingDir, 
                              config.get('Output', 'out_file')
                              )
    logger.info("Writing output html to: %s" % out_file_path)
    plot_url = py.offline.plot(fig, filename=out_file_path)
