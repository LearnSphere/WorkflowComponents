"""
Datasets :-
Geometry
1) Geometry96-97.txt (5,105)
CognitiveTutor: cognitive_tutor_2007_2009_kddcup
1)  algebra_2006_2007_train.txt (2,270,385) (1,808,571) (856, 549821, 6592)
2)  algebra_2008_2009_train.txt (8,918,055) (4,419,705) (2105, 288169, 6650)
3)  bridge_to_algebra_2006_2007_train.txt (3,679,199) (1817476) (732, 129263, 9766)
4)  bridge_to_algebra_2008_2009_train.txt (20,012,499)

ASSISTments:
1)  assistments_2012_2013/2012-2013-data-with-predictions-4-final_with_free_lunch.csv (6,123,270)
2)  assistments_2009_2010/assistments_2009_2010.csv (1,011,080)

ITEMWISE TRUE and By Batch would not work

"""

import argparse
import sys
import os
import numpy as np
import pandas as pd
from math import sqrt
import shutil

workingDir = ""
programDir = ""

## find the source file path on linux and appended to system path
execution_path = sys.argv[0].split('/')
##if len(execution_path) == 1:
##    os.chdir(os.getcwd()+"/../")
##elif len(execution_path) == 2:
##    pass
##else:
##    os.chdir("/".join(execution_path[:-2]))

print("CWD: " + str(os.getcwd()))

#sys.path.append(os.getcwd())


parser = argparse.ArgumentParser(description='Process inputs.')

# Tigris parameters


parser.add_argument('-workingDir', nargs=1, type=str,
                   help='the component instance working directory')
parser.add_argument('-programDir', nargs=1, type=str,
                   help='the component program directory')
parser.add_argument("-node", nargs=1, action='append')
parser.add_argument("-fileIndex", nargs=2, action='append')

parser.add_argument('-userId', type=str,
                   help='placeholder for Tigris system variable', default='')

# DAFM's previous parameters
parser.add_argument('-dataset', nargs=1, type=str, default=["dataset"])
####parser.add_argument('-dataset_path', nargs=1, type=str, default=None)
parser.add_argument('-user_id', nargs=1, type=str, default=["user_id"])
parser.add_argument('-problem_id', nargs=1, type=str, default=["problem_id"])
parser.add_argument('-skill_name', nargs=1, type=str, default=[None])
parser.add_argument('-correctness', nargs=1, type=str, default=["correctness"])
parser.add_argument('-section', nargs=1, type=str, default=["no", None])
parser.add_argument('-unit', nargs=1, type=str, default=['all', None])
parser.add_argument('-unit_users', nargs=1, type=str, default=["No"])

parser.add_argument('-item_wise', nargs=1, type=str, default=["False"])
parser.add_argument('-puser', nargs=1, type=str, default=["sub"])

parser.add_argument('-representation', nargs=1, type=str, default=[None])
parser.add_argument('-w2v_params', nargs=2, type=str, default=[100, 20])
parser.add_argument('-rnn_params', nargs=1, type=str, default=[100])
parser.add_argument('-clustering_params', nargs=2, type=str, default=['same', 'euclidean'])

parser.add_argument('-afm', nargs=1, type=str, default=[None])
parser.add_argument('-dafm', nargs=2, type=str, default=[None, "No"])
parser.add_argument('-dafm_params', nargs=3, type=str, default=["linear", "rmsprop", 0.1])
parser.add_argument('-dense_size', nargs=1, type=str, default=["False"])
parser.add_argument('-dkt', nargs=1, type=str, default=[None])
parser.add_argument('-theta', nargs=1, type=str, default=["False"])

parser.add_argument('-skill_wise', nargs=1, type=str, default=["False"])
parser.add_argument('-save_model', nargs=1, type=str, default=["False"])
parser.add_argument('-load_model', nargs=2, type=str, default=["False", "sub"])

parser.add_argument('-binary',nargs=1,type=str, default=["False"])

args, unknown = parser.parse_known_args()
#print("ARG" + str(args) + "\n")
#print("UNK" + str(unknown) + "\n")
print("Program dir: " + str(args.programDir[0]))
print("Working dir: " + str(args.workingDir[0]))

sys.path.append(str(args.programDir[0]) + "/program/")
os.chdir(str(args.programDir[0]) + "/program/")
from Representation.rnn import DKTnet
from load_data import afm_data_generator
from AFM.afm_keras import AFMK
from AFM.afm_liblinear import AFML
from DAFM.dafm import DeepAFM
os.chdir(str(args.workingDir[0]))

class KCModel:

    def __init__(self):
        global args
        global parser
        args.dafm_params[2] = float(args.dafm_params[2])
        user_path = "False"

        workingDir = args.workingDir

        # With DataShop data (student-step), we need to split up Problem Hierarchy, e.g. "Unit u2, Section nc" etc.
        data_path = None
        # Hijack dataset_path with the tigris arguments (node 0, file 0). This component accepts a single input file.
        for x in range(len(args.node)):
            if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
                data_path = args.fileIndex[x][1]
                print("Analyzing " + str(data_path))

        # if not os.path.exists(data_path):
        #    print ("Error not valid dataset")
        #    sys.exit()
        #source_path = args.programDir + '/program/'
        print("Working dir: " + str(workingDir))
        ####accuracy_path = str(workingDir) + '/'

        ## adding paths to args variable
        # parser.add_argument('-user_path', nargs=1, type=str, default=[user_path])
        # not needed only using for previous code (ignore)
        parser.add_argument('-data_path', nargs=1, type=str, default=data_path)

        ####parser.add_argument('-source_path', nargs=1, type=str, default=source_path)
        ####parser.add_argument('-accuracy_path', nargs=1, type=str, default=accuracy_path)
        args, unknown = parser.parse_known_args()
        self.args = args


        ## generating unique file name
        self.fname ='$'.join([self.args.section[0], self.args.dafm_params[0], self.args.dafm_params[1], str(self.args.dafm_params[2])])
        self.fname += self.args.dense_size[0] if not (self.args.dense_size[0]=="False") else ""
        if not self.args.item_wise[0] == "False":
            self.fname += self.args.theta[0]
        if not self.args.theta[0] == "False":
            self.fname += self.args.theta[0] + self.args.item_wise[0]
        self.fname += self.args.section[0] if not (self.args.section[0]=="no") else ""
        if not (self.args.representation[0] == None) :
            if self.args.representation[0][:3] == 'rnn':
                self.fname += '_'+self.args.representation[0] + '_' + str(self.args.rnn_params[0]) + '_' + '_'.join(self.args.clustering_params)
            elif  self.args.representation[0][:3] == 'w2v':
                self.fname += '_'+self.args.representation[0] + '_' + str(self.args.w2v_params[0]) + '_' + str(self.args.w2v_params[1]) + '_' + '_'.join(self.args.clustering_params)
        parser.add_argument('-fname', nargs=1, type=str, default=self.fname)
        args, unknown = parser.parse_known_args()
        self.args = args

        try:
            self.args.clustering_params[0] = int(self.args.clustering_params[0])
        except:
            pass

        ## accuracy directory using unit and user (?? not clear)
        #print ("./Accuracy/"+ "sub"+"@"+self.args.unit[0]+"/")
        self.adir_name= "./Accuracy/"+ "sub"+"@"+self.args.unit[0]+"/"
        self.apdir_name= "./Accuracy/"+  self.args.puser[0]+"@"+self.args.unit[0]+"/"
        self.dir_name= "./Accuracy/"+ "sub"+"@"+self.args.unit[0]+"/"
        self.pdir_name= "./Accuracy/"+  self.args.puser[0]+"@"+self.args.unit[0]+"/"
        if not os.path.exists(self.dir_name):
            os.makedirs(self.dir_name)
        if not os.path.exists(self.pdir_name):
            os.makedirs(self.pdir_name)
        if not os.path.exists(self.adir_name):
            os.makedirs(self.adir_name)
        self.validation = True
        # validation must not be done when using complete training set
        if args.puser[0]=="orig":
            self.validation = False
        #print (args)

    def save_model(self, model, dafm_type=""):

        if self.args.puser[0] == "orig":
            if not os.path.exists(self.apdir_name+"Model/"):
                os.makedirs(self.apdir_name+"Model/")
            model.save_weights(self.apdir_name +"Model/" + dafm_type+"$"+self.fname+".h5")
        else:
            if not os.path.exists(self.adir_name+"Model/"):
                os.makedirs(self.adir_name+"Model/")
            model.save_weights(self.adir_name +"Model/" + dafm_type+"$"+self.fname+".h5")

    def load_model(self, model, dafm_type=""):

        if self.args.puser[0]=="orig" and self.args.load_model[1]=="orig":
            #print ("Here")
            dir_name = self.apdir_name
        else:
            dir_name = self.adir_name
        if not os.path.exists(dir_name+"Model/" + dafm_type + "$"+self.fname+".h5"):
            print ("No Model Found At", dir_name+"Model/" + dafm_type + "$"+self.fname+".h5", file=sys.stderr)
            sys.exit()
        model.load_weights(dir_name + "Model/"+ dafm_type + "$" + self.fname+".h5")
        return model

    def save(self, rmse, AIC, BIC, best_epoch, loss_epoch, dafm_type):
        global workingDir
        print(dafm_type, rmse)
        outfile = open(workingDir + "output.txt", 'a')
        outfile.write("DAFM_type\tRMSE\tpredict_afm\tpredictions\n")
        outfile.write (dafm_type + "\t")
        outfile.write(str(rmse) + "\t")
        outfile.close()
        fname = dafm_type+"$"+self.fname
        if self.args.puser[0] == "orig":
            dir_name = self.apdir_name
        else:
            dir_name = self.adir_name
        if (not os.path.exists(dir_name+"Losses/")) or (not os.path.exists(dir_name+"Prediction/")):
            os.makedirs(dir_name+"Losses/")
            os.makedirs(dir_name+"Prediction/")

        d = {"RMSE":[rmse], "AIC":[AIC], "BIC":[BIC], "best epoch":[best_epoch], "dafm_type":[dafm_type], "section":[self.args.section[0]], "activation":[self.args.dafm_params[0]], "optimizer":[self.args.dafm_params[1]], "learning_rate":[self.args.dafm_params[2]], "theta":self.args.theta[0]}
        if not (self.args.dense_size[0] == "False"):
            d["dense_size"] = self.args.dense_size[0]
        if self.args.skill_wise[0] == "False":
            if self.args.puser[0] == "orig":
                if self.args.save_model[0]=="True":
                    pd.DataFrame(loss_epoch).to_csv(dir_name+"Losses/"+fname+".csv", sep=",", index=False)
                    pd.DataFrame(d).to_csv(self.apdir_name+"Prediction/"+fname+".acc", sep=",", index=False)
                else:
                    pd.DataFrame(d).to_csv(self.apdir_name+fname+".acc", sep=",", index=False)
                return
            else:
                pd.DataFrame(loss_epoch).to_csv(dir_name+"Losses/"+fname+".csv", sep=",", index=False)
                pd.DataFrame(d).to_csv(dir_name+"Prediction/"+fname+".acc", sep=",", index=False)
                pass
        #else:
        #    print ("Not Saving")

    def fit_predict_afm(self, trainX, trainY, testX, testY, d_t):

        afm_obj =  AFMK() if self.args.afm[0]=="afm-keras" else AFML()
        afm_model, AIC, BIC = afm_obj.fit(trainX, trainY)
        if self.args.representation[0][:3] == 'rnn':
            fname = self.args.afm[0] + '_' + self.args.representation[0] + '_' + self.args.rnn_params[0] + '_' + '_'.join(self.args.clustering_params)
        else:
            fname = self.args.afm[0] + '_' + self.args.representation[0] + '_' + self.args.w2v_params[0] + '_' + self.args.w2v_params[1] + '_' + '_'.join(self.args.clustering_params)
        fpath = "./Accuracy/"
        if not os.exists(fpath):
            os.makedirs(fpath)
        predict_afm = afm_obj.predict(testX, testY, afm_model, d_t)
        f = open(fpath+fname, 'w')
        f.write(predict_afm + "\t")
        return [predict_afm, AIC, BIC]

    def fit_predict_dkt(self, trainX, trainY, trainY_order, testX, testY, testY_order, input_dim, input_dim_order):
        global workingDir
        dkt_obj = DKTnet(input_dim, input_dim_order, 200, 'linear', trainX, trainY, trainY_order, testX, testY, testY_order)
        dkt_model = dkt_obj.build()
        predict_dkt = dkt_obj.predict_rmse(testX, testY, testY_order, dkt_model)
        print(predict_dkt)
        outfile = open(workingDir + "output.txt", 'a')
        outfile.write (predict_dkt + "\t")
        outfile.close()
        sys.exit()
        return [predict_afm, AIC, BIC]

    def fit_predict_dafm(self, trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, initialize):

        loss_epoch = {"epoch":[0], "loss":[0], "val_loss":[0], 'patience':[0]}
        AIC, BIC, best_epoch = 0, 0, 0
        initialize["section"] = self.args.section[0]
        initialize["theta_student"] = self.args.theta[0]
        initialize["activation"] = self.args.dafm_params[0]
        initialize["optimizer"] = self.args.dafm_params[1]
        initialize["learning_rate"] = self.args.dafm_params[2]
        initialize["binary"] = self.args.binary[0]
        Q_jk_initialize = np.copy(initialize["Q_jk_initialize"])
        # if self.args.dafm[0].split('_')[-1] == "double":
        #     Q_jk_initialize = np.concatenate([Q_jk_initialize, Q_jk_initialize], axis=1)

        if initialize["activation"] == "sigmoid" or '-' in initialize["activation"]:
            np.place(initialize["Q_jk_initialize"], initialize["Q_jk_initialize"]==0, [-99])
            np.place(initialize["Q_jk_initialize"], initialize["Q_jk_initialize"]==1, [99])

        if self.args.dafm[0].split('_')[0] == "fine-tuned" or self.args.dafm[0].split('_')[0] == "round-fine-tuned":
            initialize["dafm_type"] = "dafm-afm"
            dafm_obj = DeepAFM()
            dafm_model = dafm_obj.build(**initialize)
            if self.args.load_model[0] == "True":
                dafm_model = self.load_model(dafm_model, initialize["dafm_type"])
                if self.args.skill_wise[0] == "True":
                    pass
                else:
                    dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit(trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, dafm_model, loaded=True, validation=self.validation)
            else:

                dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit(trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, dafm_model, validation=self.validation)
                if self.args.save_model[0] == "True":
                    self.save_model(dafm_model, initialize["dafm_type"])

            if self.args.skill_wise[0] == "True":
                y_pred = dafm_obj.prediction(testX, testS, testStudent,  dafm_model)
                self.rmse_masking_skillwise(testY, y_pred, testX, Q_jk_initialize, initialize["dafm_type"])
                self.save_qjk(dafm_model.get_layer("Q_jk").get_weights()[0], initialize["dafm_type"])

            predict_dafm = dafm_obj.predict(testX, testY, testS, testStudent, dafm_model)
            self.save(predict_dafm, AIC, BIC, best_epoch, loss_epoch, initialize["dafm_type"])
            initialize["model1"] = dafm_model

            if self.args.dafm[0].split('_')[0] == "round-fine-tuned":
                # if self.args.dafm[0].split('_')[-1] == "double":
                #    initialize["dafm_type"] = "fine-tuned_double"
                # else:
                initialize["dafm_type"] = "fine-tuned"
                dafm_obj = DeepAFM()
                dafm_model = dafm_obj.build(**initialize)

                if self.args.load_model[0] == "True":
                    dafm_model = self.load_model(dafm_model, initialize["dafm_type"])
                    if self.args.skill_wise[0] == "True":
                        pass
                    else:
                        dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit(trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, dafm_model, loaded=True, validation=self.validation)
                else:
                    dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit(trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, dafm_model, validation=self.validation)
                    if self.args.save_model[0] == "True":
                        self.save_model(dafm_model, initialize["dafm_type"])

                if self.args.skill_wise[0] == "True":
                    y_pred = dafm_obj.prediction(testX, testS, testStudent, dafm_model)
                    self.rmse_masking_skillwise(testY, y_pred, testX, Q_jk_initialize,initialize["dafm_type"])
                    self.save_qjk(dafm_model.get_layer("Q_jk").get_weights()[0], initialize["dafm_type"])

                predict_dafm = dafm_obj.predict(testX, testY, testS, testStudent, dafm_model)
                self.save(predict_dafm, AIC, BIC, best_epoch, loss_epoch, initialize["dafm_type"])
                initialize["model1"] = dafm_model

        initialize["dafm_type"] = self.args.dafm[0]
        dafm_obj = DeepAFM()
        dafm_model = dafm_obj.build(**initialize)

        if self.args.load_model[0] == "True":
            dafm_model = self.load_model(dafm_model, initialize["dafm_type"])
            if self.args.skill_wise[0] == "True":
                pass
            else:
                dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit(trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, dafm_model, loaded=True, validation=self.validation)
        else:
            dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit(trainX, trainY, trainS, trainStudent, testX, testY, testS, testStudent, dafm_model, validation=self.validation)
            if self.args.save_model[0] == "True":
                self.save_model(dafm_model, initialize["dafm_type"])

        if self.args.skill_wise[0]=="True":
                y_pred = dafm_obj.prediction(testX, testS, testStudent, dafm_model)
                self.rmse_masking_skillwise(testY, y_pred, testX, Q_jk_initialize, initialize["dafm_type"])
                self.save_qjk(dafm_model.get_layer("Q_jk").get_weights()[0], initialize["dafm_type"])
                sys.exit()
        predict_dafm = dafm_obj.predict(testX, testY, testS, testStudent, dafm_model)
        # print ("s")
        # print ('SavingStarts')
        # pd.DataFrame(dafm_model.get_layer('Q_jk_dense').get_weights()[0]).to_csv('CogQjkDense.csv', sep=',', index=False)
        # sys.exit()
        self.save(predict_dafm, AIC, BIC, best_epoch, loss_epoch, initialize["dafm_type"])
        return [predict_dafm, AIC, BIC]

    def fit_predict_batch_dafm(self, dafmdata_obj, initialize):

        loss_epoch = {"epoch":[0], "loss":[0], "val_loss":[0], 'patience':[0]}
        AIC, BIC, best_epoch = 0, 0, 0
        initialize["theta_student"] = self.args.theta[0]
        initialize["section"] = self.args.section[0]
        initialize["activation"] = self.args.dafm_params[0]
        initialize["optimizer"] = self.args.dafm_params[1]
        initialize["learning_rate"] = self.args.dafm_params[2]
        initialize["binary"] = self.args.binary[0]

        Q_jk_initialize = np.copy(initialize["Q_jk_initialize"])
        # if self.args.dafm[0].split('_')[-1] == "double":
        #    Q_jk_initialize = np.concatenate([Q_jk_initialize, Q_jk_initialize], axis=1)

        if initialize["activation"] == "sigmoid" or '-' in initialize["activation"]:
            np.place(initialize["Q_jk_initialize"], initialize["Q_jk_initialize"]==0, [-99])
            np.place(initialize["Q_jk_initialize"], initialize["Q_jk_initialize"]==1, [99])

        if self.args.dafm[0].split('_')[0] == "fine-tuned" or self.args.dafm[0].split('_')[0] == "round-fine-tuned":

            # if self.args.dafm[0].split('_')[-1] == "double":
            #    initialize["dafm_type"] = "dafm-afm_double"
            # else:
            initialize["dafm_type"] = "dafm-afm"
            dafm_obj = DeepAFM()
            dafm_model = dafm_obj.build(**initialize)

            if self.args.load_model[0] == "True":
                dafm_model = self.load_model(dafm_model, initialize["dafm_type"])
                if self.args.skill_wise[0] == "False":
                    dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit_batches(dafmdata_obj, dafm_model, loaded=True)
            else:
                dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit_batches(dafmdata_obj, dafm_model)
                if self.args.save_model[0] == "True":
                    self.save_model(dafm_model, initialize["dafm_type"])

            if self.args.skill_wise[0] == "True":
                    self.rmse_masking_skillwise_batches(dafm_obj, dafmdata_obj, dafm_model, Q_jk_initialize, initialize["dafm_type"])
                    self.save_qjk(dafm_model.get_layer("Q_jk").get_weights()[0], initialize["dafm_type"])

            predict_dafm = dafm_obj.predict_batches(dafmdata_obj, dafm_model)
            self.save(predict_dafm, AIC, BIC, best_epoch, loss_epoch, initialize["dafm_type"])
            initialize["model1"] = dafm_model

            if self.args.dafm[0].split('_')[0] == "round-fine-tuned":

                # if self.args.dafm[0].split('_')[-1] == "double":
                #    initialize["dafm_type"] = "fine-tuned_double"
                # else:
                initialize["dafm_type"] = "fine-tuned"
                dafm_obj = DeepAFM()
                dafm_model = dafm_obj.build(**initialize)

                if self.args.load_model[0] == "True":
                    dafm_model = self.load_model(dafm_model, initialize["dafm_type"])
                    if self.args.skill_wise[0] == "False":
                        dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit_batches(dafmdata_obj, dafm_model, loaded=True)
                else:
                    dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit_batches(dafmdata_obj, dafm_model)
                    if self.args.save_model[0] == "True":
                        self.save_model(dafm_model, initialize["dafm_type"])

                if self.args.skill_wise[0] == "True":
                    self.rmse_masking_skillwise_batches(dafm_obj, dafmdata_obj, dafm_model, Q_jk_initialize, initialize["dafm_type"])
                    self.save_qjk(dafm_model.get_layer("Q_jk").get_weights()[0], initialize["dafm_type"])

                predict_dafm = dafm_obj.predict_batches(dafmdata_obj, dafm_model)
                self.save(predict_dafm, AIC, BIC, best_epoch, loss_epoch, initialize["dafm_type"])
                initialize["model1"] = dafm_model

        initialize["dafm_type"] = self.args.dafm[0]
        dafm_obj = DeepAFM()
        dafm_model = dafm_obj.build(**initialize)

        if self.args.load_model[0] == "True":
            dafm_model = self.load_model(dafm_model, initialize["dafm_type"])
            if self.args.skill_wise[0] == "False":
                dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit_batches(dafmdata_obj, dafm_model, loaded=True)
        else:
            dafm_model, AIC, BIC, best_epoch, loss_epoch = dafm_obj.fit_batches(dafmdata_obj, dafm_model)
            if self.args.save_model[0] == "True":
                self.save_model(dafm_model, initialize["dafm_type"])

        if self.args.skill_wise[0]=="True":
                self.rmse_masking_skillwise_batches(dafm_obj, dafmdata_obj, dafm_model, Q_jk_initialize, initialize["dafm_type"])
                self.save_qjk(dafm_model.get_layer("Q_jk").get_weights()[0], initialize["dafm_type"])

        # print ('SavingStarts')
        # pd.DataFrame(dafm_model.get_layer('Q_jk_dense').get_weights()[0]).to_csv('CogQjkDense.csv', sep=',', index=False)
        predict_dafm = dafm_obj.predict_batches(dafmdata_obj, dafm_model)
        self.save(predict_dafm, AIC, BIC, best_epoch, loss_epoch, initialize["dafm_type"])
        return [predict_dafm, AIC, BIC]

    def rmse_masking_skillwise_batches(self, dafm_obj, dafmdata_obj, dafm_model, Q_jk_initialize, dafm_type=""):

        fname = dafm_type + "$" + self.fname + '$' + self.args.puser[0]
        test_generator = dafmdata_obj.data_generator1("test")
        skill_error, problem_error = {}, {}
        counter = 0
        for x_test, y_test, x_test_section, x_test_student, batch_size in test_generator:
            y_pred = dafm_obj.prediction(x_test, x_test_section, x_test_student, dafm_model, batch_size)
            skill_rmse, problem_rmse = self.rmse_masking_skillwise(y_test, y_pred, x_test, Q_jk_initialize, dafm_type=dafm_type, save=False)
            if counter==0:
                skill_error, problem_error = skill_rmse, problem_rmse
            else:
                for i, j in skill_rmse.items():
                    if type(j) == list:
                        skill_error[i].extend(j)
                    else:
                        skill_error[i].append([j])
                for i, j in problem_rmse.items():
                    if type(j) == list:
                        problem_error[i].extend(j)
                    else:
                        problem_error[i].append([j])
            counter += 1
        ###print ("Skill VS RMSE started batches")
        skills, rmse_s = [], []
        for i, j in skill_error.items():
            skills.append(i)
            rmse_s.append(sqrt(np.mean(j)))
        problems, rmse_p = [], []

        for i, j in problem_error.items():
            problems.append(i)
            rmse_p.append(sqrt(np.mean(j)))
        if not os.path.exists("./SkillWise/"):
            os.makedirs("./SkillWise/")
        pd.DataFrame({"skill":skills, "RMSE":rmse_s}).to_csv("./SkillWise/"+fname+".srmse", sep=",", index=False)
        pd.DataFrame({"problem":problems, "RMSE":rmse_p}).to_csv("./SkillWise/"+fname+".prmse", sep=",", index=False)


    def rmse_masking_skillwise(self, y_true, y_pred, x_test, Q_jk_initialize, dafm_type="", save=True):

        fname = dafm_type + "$" + self.fname + '$' + self.args.puser[0]
        mask_matrix = np.sum(x_test, axis=2).flatten()
        num_users, max_responses = np.shape(x_test)[0], np.shape(x_test)[1]
        x_test = np.reshape(x_test, (x_test.shape[0]*x_test.shape[1], x_test.shape[2]))
        responses_skills = []
        for i in range(x_test.shape[0]):
            if not (np.sum(x_test[i]) == 0) :
                responses_skills.append(np.nonzero(x_test[i])[0][0])
            else:
                responses_skills.append('$')
        y_pred = y_pred.flatten()
        y_true = y_true.flatten()
        rmse = []

        # Why does this code have Geometry dataset cases? it used to be the -dataset variable (removed as superfluous)
        GeometrySpecificVariable = ""
        skill_index = pd.read_csv("./SkillWise/"+"skill_index.csv", sep=",")
        problem_index = pd.read_csv("./SkillWise/"+"problem_index.csv", sep=",")
        skill_index["skills"] = skill_index["skills"].map(str)
        problem_index["problem"] = problem_index["problem"].map(str)
        d_problem = dict(zip(problem_index["index"].map(int), problem_index["problem"].map(str)))
        d_skill = dict(zip(skill_index["index"].map(int), skill_index["skills"].map(str)))
        if GeometrySpecificVariable == 'Geometry':
            problem_name = {i.split('_(')[0]:[] for i in sorted(list(problem_index["problem"]))}
        problem_rmse = {i:[] for i in sorted(list(problem_index["problem"]))}
        skill_error = {i:[] for i in sorted(list(skill_index["skills"]))}
        for user in range(num_users):
            diff_sq, response = 0, 0
            for i in range(user * max_responses, (user + 1) * max_responses):
                if mask_matrix[i] == 0:
                    break
                problem_step1 = np.nonzero(x_test[i])[0][0]
                diff_sq = (y_true[i] - y_pred[i]) ** 2
                problem_rmse[d_problem[problem_step1]].append(diff_sq)
                skill_error[d_skill[np.nonzero(Q_jk_initialize[responses_skills[i]][:])[0][0]]].append(diff_sq)
                if GeometrySpecificVariable == 'Geometry':
                    problem_name[d_problem[problem_step1].split('_(')[0]].append(diff_sq)

        ###print ("Skill VS RMSE started")
        if GeometrySpecificVariable == 'Geometry' and save:
            problem_names, rmse_ps = [], []
            for i, j in problem_name.items():
                problem_names.append(i)
                rmse_ps.append(sqrt(np.mean(j)))
            if not os.path.exists("./SkillWise/"):
                os.makedirs("./SkillWise/")
            pd.DataFrame({"problem_name":problem_names, "RMSE":rmse_ps}).to_csv("./SkillWise/"+fname+".psrmse", sep=",", index=False)

        skills, rmse_s = [], []
        for i, j in skill_error.items():
            skills.append(i)
            rmse_s.append(sqrt(np.mean(j)))
        problems, rmse_p = [], []
        for i, j in problem_rmse.items():
            problems.append(i)
            rmse_p.append(sqrt(np.mean(j)))
        if save:
            if not os.path.exists("./SkillWise/"):
                os.makedirs("./SkillWise/")
            pd.DataFrame({"skill":skills, "RMSE":rmse_s}).to_csv("./SkillWise/"+fname+".srmse", sep=",", index=False)
            pd.DataFrame({"problem":problems, "RMSE":rmse_p}).to_csv("./SkillWise/"+fname+".prmse", sep=",", index=False)
        else:
            return skill_error, problem_rmse

    def save_qjk(self, w_Q_jk, dafm_type):

        fname = dafm_type + "$" + self.fname + '$' + self.args.puser[0]
        skill_index = pd.read_csv("./SkillWise/"+"skill_index.csv", sep=",")
        problem_index = pd.read_csv("./SkillWise/"+"problem_index.csv", sep=",")
        d_problem = dict(zip(problem_index["index"].map(int), problem_index["problem"].map(str)))
        d_skill = dict(zip(skill_index["index"].map(int), skill_index["skills"].map(str)))
        w_Q_jk_dataframe = pd.DataFrame(w_Q_jk)
        w_Q_jk_dataframe.index = [d_problem[i] for i in range(len(d_problem))]
        w_Q_jk_dataframe.columns = [d_skill[i] for i in range(len(d_skill))]
        w_Q_jk_dataframe.to_csv("./SkillWise/"+fname+".qjk", sep=",")

    def main(self):


        shutil.rmtree("./log/", ignore_errors=True)

        obj_for_afm_dafm = afm_data_generator(self.args)
        data_for_afm_dafm = obj_for_afm_dafm.main()
        predictions = []

        if (not self.args.afm[0] == None):
            for index, data in enumerate(data_for_afm_dafm):
                ###print ("testing", index)
                if index == 0:
                    predictions.append(self.fit_predict_afm(data[0], data[1], data[2], data[3], data[4]))
                else:
                    if self.args.dafm[1] == "Yes":
                        predictions.append(self.fit_predict_batch_dafm(data[0], data[1]))
                    else:
                        predictions.append(self.fit_predict_dafm(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8]))

        elif (not self.args.dafm[0] == None):
            for data in data_for_afm_dafm:
                if self.args.dafm[1] == "Yes":
                    predictions.append(self.fit_predict_batch_dafm(data[0], data[1]))
                else:
                    predictions.append(self.fit_predict_dafm(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8]))

        else:
            for data in data_for_afm_dafm:
                predictions.append(self.fit_predict_dkt(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]))
        print (predictions)
        outfile = open(workingDir + "output.txt", 'a')
        outfile.write (str(predictions) + "\n")
        outfile.close()

obj = KCModel()
obj.main()
