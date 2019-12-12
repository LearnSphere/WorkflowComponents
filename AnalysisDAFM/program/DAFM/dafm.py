from sklearn.metrics import mean_squared_error, log_loss
from keras.models import Model
from keras.models import load_model
from keras.layers import Input, Dense
from keras.layers.recurrent import SimpleRNN
from keras.layers.merge import multiply, concatenate, add
from keras import backend as K
from keras import initializers
from keras.callbacks import EarlyStopping
from keras.layers.wrappers import TimeDistributed
from keras.callbacks import Callback
from keras import optimizers
import pandas as pd
import numpy as np
from keras.constraints import max_norm, non_neg, unit_norm
np.random.seed(42)
from math import sqrt
import os
import sys

from collections import defaultdict

class DeepAFM:

    def __init__(self):
        pass

    def custom_bce(self, y_true, y_pred):
        b = K.not_equal(y_true, -K.ones_like(y_true))
        b = K.cast(b, dtype='float32')
        ans = K.mean(K.binary_crossentropy(y_true, y_pred), axis=-1) * K.mean(b, axis=-1)
        ans = K.cast(ans, dtype='float32')
        return K.sum(ans)

    def custom_activation(self, x):
        if self.activation.split('-')[0] == "custom":
            a = float(self.activation.split('-')[1])
            return 1.0 / ( 1 + K.exp(-a*x) )
        elif self.activation.split('-')[0] == "rounded":
            K.minimum(K.maximum(K.round(K.sigmoid(x)), 0), 1)

    def custom_init(self, shape, dtype=None):
        return K.cast_to_floatx(self.Q_jk_initialize)

    def custom_random(self, shape, dtype=None):
        if self.random_init == "normal":
            return K.random_normal(shape, 0.5, 0.05, dtype=dtype, seed=22)
        else:
            return K.random_uniform(shape, 0, 1, dtype=dtype, seed=22)

    def f(self, x):
        def custom_init(shape, dtype=None):
            return K.cast_to_floatx(np.reshape(x, shape))
        return custom_init

    def build(self, dafm_type="dafm-afm", optimizer="rmsprop", learning_rate=0.01, activation="linear", Q_jk_initialize=0, section="", section_count=0, model1="", stateful=False, theta_student="False", student_count=0, binary="False"):

        skills = np.shape(Q_jk_initialize)[1]
        steps = np.shape(Q_jk_initialize)[0]
        self.activation = activation
        if '-' in self.activation:
            activation = self.custom_activation

        if dafm_type.split("_")[-1] == "different":
            skills = int( float(dafm_type.split("_")[-2])*skills )
            dafm_type = dafm_type.split('_')[0]

        if dafm_type.split("_")[0] == "round-fine-tuned":
            try:
                self.round_threshold = float(dafm_type.split("_")[-1])
                dafm_type = dafm_type.split("_")[0]
            except:
                pass

        q_jk_size = skills
        if '^' in dafm_type:
            q_jk_size = skills
            skills = int (float(dafm_type.split('^')[-1]) * skills)
            dafm_type = dafm_type.split('^')[0]

        self.dafm_type = dafm_type
        if dafm_type == "random-uniform" or dafm_type == "random-normal":
            qtrainable, finetuning, randomize  = True, False, True
            self.random_init = dafm_type.split('-')[-1]
        elif dafm_type == "dafm-afm":
            qtrainable, finetuning, randomize = False, False, False
        elif dafm_type == "fine-tuned":
            qtrainable, finetuning, randomize = True, True, False
        elif dafm_type == "kcinitialize":
            qtrainable, finetuning, randomize = True, False, False
        elif dafm_type== "round-fine-tuned":
            # if not self.round_threshold == -1:
                # rounded_Qjk = np.abs(Q_jk1 - Q_jk_initialize)
                # Q_jk1[rounded_Qjk <= self.round_threshold] = Q_jk_initialize[rounded_Qjk <= self.round_threshold]
                # Q_jk1[rounded_Qjk > self.round_threshold] = np.ones(np.shape(Q_jk_initialize[rounded_Qjk > self.round_threshold])) - Q_jk_initialize[rounded_Qjk > self.round_threshold]
            # else:
            Q_jk1 = model1.get_layer("Q_jk").get_weights()[0]
            Q_jk1 = np.minimum(np.ones(np.shape(Q_jk1)), np.maximum(np.round(Q_jk1), np.zeros(np.shape(Q_jk1))))
            model1.get_layer("Q_jk").set_weights([Q_jk1])
            return model1
        elif dafm_type == "qjk-dense":
            qtrainable, finetuning, randomize = False, False, False
            activation_dense = activation
        elif dafm_type == "random-qjk-dense-normal" or dafm_type == "random-qjk-dense-uniform":
            qtrainable, finetuning, randomize = False, False, True
            self.random_init = dafm_type.split('-')[-1]
            activation_dense = activation
        else:
            print ("No Valid Model Found")
            sys.exit()

        if section == "onehot":
            section_input = Input(batch_shape=(None, None, section_count), name='section_input')
        if not theta_student=="False":
            student_input = Input(batch_shape=(None, None, student_count), name='student_input')

        virtual_input1 = Input(batch_shape=(None, None, 1), name='virtual_input1')
        if finetuning:
            B_k = TimeDistributed(Dense(skills, activation='linear', kernel_initializer=self.f(model1.get_layer("B_k").get_weights()[0]), use_bias=False), name="B_k")(virtual_input1)
            T_k = TimeDistributed(Dense(skills, activation='linear', kernel_initializer=self.f(model1.get_layer("T_k").get_weights()[0]), use_bias=False), name="T_k")(virtual_input1)
            bias_layer = TimeDistributed(Dense(1, activation='linear', use_bias=False, kernel_initializer=self.f(model1.get_layer("bias").get_weights()[0]), trainable=True), name="bias")(virtual_input1)
        else:
            B_k = TimeDistributed(Dense(skills, activation='linear', use_bias=False, trainable=True), name="B_k")(virtual_input1)
            T_k = TimeDistributed(Dense(skills, activation='linear', use_bias=False, trainable=True), name="T_k")(virtual_input1)
            bias_layer = TimeDistributed(Dense(1, activation='linear', use_bias=False, kernel_initializer=initializers.Zeros(), trainable=True), name="bias")(virtual_input1)

        step_input = Input(batch_shape=(None, None, steps), name='step_input')
        if randomize:
            if binary=="False":
                Q_jk = TimeDistributed(Dense(q_jk_size, use_bias=False, activation=activation, kernel_initializer=self.custom_random), trainable=qtrainable ,name="Q_jk")(step_input)
            else:
                Q_jk = TimeDistributed(BinaryDense(q_jk_size, use_bias=False,  activation=activation, kernel_initializer=self.custom_random),trainable=qtrainable, name="Q_jk")(step_input)
        else:
            if binary=="False":
                Q_jk = TimeDistributed(Dense(skills, activation=activation, kernel_initializer=self.f(Q_jk_initialize), use_bias=False,trainable=qtrainable), trainable=qtrainable, name="Q_jk")(step_input)
            else:
                Q_jk = TimeDistributed(BinaryDense(skills, activation=activation, kernel_initializer=self.f(Q_jk_initialize),trainable=qtrainable,
                                use_bias=False), name="Q_jk", trainable=qtrainable)(step_input)

        if dafm_type == "random-qjk-dense-normal" or dafm_type == "random-qjk-dense-uniform":
            if binary =="False":
                Q_jk = TimeDistributed(Dense(skills, activation=activation_dense, use_bias=False, kernel_initializer=self.custom_random, trainable=True), name="Q_jk_dense")(Q_jk)
            else:
                Q_jk = TimeDistributed(BinaryDense(skills, activation=activation_dense, use_bias=False, kernel_initializer=self.custom_random, trainable=True), name="Q_jk_dense")(Q_jk)

        elif dafm_type == "qjk-dense":
            if binary =='False':
                Q_jk = TimeDistributed(Dense(skills, activation=activation_dense, use_bias=False, kernel_initializer=initializers.Identity(), trainable=True), name="Q_jk_dense")(Q_jk)
            else:
                Q_jk = TimeDistributed(BinaryDense(skills, activation=activation_dense, use_bias=False, kernel_initializer=initializers.Identity(), trainable=True), name="Q_jk_dense")(Q_jk)
        else:
            pass

        Qjk_mul_Bk = multiply([Q_jk, B_k])
        sum_Qjk_Bk = TimeDistributed(Dense(1, activation='linear', trainable=False, kernel_initializer=initializers.Ones(), use_bias=False), trainable=False,name="sum_Qjk_Bk")(Qjk_mul_Bk)

        P_k = SimpleRNN(skills, kernel_initializer=initializers.Identity(), recurrent_initializer=initializers.Identity() , use_bias=False, trainable=False, activation='linear', return_sequences=True, name="P_k")(Q_jk)

        Qjk_mul_Pk_mul_Tk = multiply([Q_jk, P_k, T_k])
        sum_Qjk_Pk_Tk = TimeDistributed(Dense(1, activation='linear', trainable=False, kernel_initializer=initializers.Ones(), use_bias=False),trainable=False, name="sum_Qjk_Pk_Tk")(Qjk_mul_Pk_mul_Tk)
        Concatenate = concatenate([bias_layer, sum_Qjk_Bk, sum_Qjk_Pk_Tk])

        if not (theta_student=="False"):
            if finetuning:
                theta = TimeDistributed(Dense(1, activation="linear", use_bias=False, kernel_initializer=self.f(model1.get_layer("theta").get_weights()[0])), name='theta')(student_input)
            else:
                theta = TimeDistributed(Dense(1, activation="linear", use_bias=False), name='theta')(student_input)
            Concatenate = concatenate([Concatenate, theta])

        if section == "onehot":
            if finetuning:
                S_k = TimeDistributed(Dense(1, activation="linear", use_bias=False, kernel_initializer=self.f(model1.get_layer("S_k").get_weights()[0])), name='S_k')(section_input)
            else:
                S_k = TimeDistributed(Dense(1, activation="linear", use_bias=False), name='S_k')(section_input)
            Concatenate = concatenate([Concatenate, S_k])

        output = TimeDistributed(Dense(1, activation="sigmoid", trainable=False, kernel_initializer=initializers.Ones(), use_bias=False), trainable=False, name="output")(Concatenate)
        if section == "onehot" and not (theta_student=="False"):
            model = Model(inputs=[virtual_input1, step_input, section_input, student_input], outputs=output)
        elif section == "onehot" and theta_student=="False":
            model = Model(inputs=[virtual_input1, step_input, section_input], outputs=output)
        elif not (section == "onehot") and not (theta_student=="False"):
            model = Model(inputs=[virtual_input1, step_input, student_input], outputs=output)
        else:
            model = Model(inputs=[virtual_input1, step_input], outputs=output)

        d_optimizer = {"rmsprop":optimizers.RMSprop(lr=learning_rate), "adam":optimizers.Adam(lr=learning_rate), "adagrad":optimizers.Adagrad(lr=learning_rate) }
        model.compile( optimizer = d_optimizer[optimizer],
                       loss = self.custom_bce)
        return model

    def fit(self, x_train, y_train, x_train_section, x_train_student, x_test, y_test, x_test_section, x_test_student, model, epochs=5, batch_size=32, loaded=False, validation=True):

        loss_epoch = {"epoch":[], "loss":[], "val_loss":[], 'patience':[]}
        print ("Max Epochs", epochs)
        if self.dafm_type == "round-fine-tuned" or loaded:
            best_model = model

        patience, epoch = 0 , 1
        prev_best_val_loss = np.inf
        counter = 0

        virtual_input1 = np.ones([np.shape(x_train)[0], np.shape(x_train)[1], 1])
        virtual_input1_test = np.ones([np.shape(x_test)[0], np.shape(x_test)[1], 1])
        if not validation:
            earlyStopping = EarlyStopping(monitor='loss', patience=2)
            if len(x_train_student) == 0:
                if len(x_train_section) == 0:
                    history_callback = model.fit([virtual_input1, x_train], y_train, batch_size=batch_size, epochs=epochs, callbacks=[earlyStopping], verbose=1, shuffle=True)
                else:
                    history_callback = model.fit([virtual_input1, x_train, x_train_section], y_train, batch_size=batch_size, epochs=epochs, callbacks=[earlyStopping], verbose=1, shuffle=True)
            else:
                if len(x_train_section) == 0:
                    history_callback = model.fit([virtual_input1, x_train, x_train_student], y_train, batch_size=batch_size, epochs=epochs , callbacks=[earlyStopping], verbose=1, shuffle=True)
                else:
                    history_callback = model.fit([virtual_input1, x_train, x_train_section, x_train_student], y_train, batch_size=batch_size, epochs=epochs, callbacks=[earlyStopping], verbose=1, shuffle=True)
            # print ("Epoch Number:", counter, "Patience:", 0, "val loss:", current_val_loss)
            loss_epoch["loss"].extend(history_callback.history["loss"])
            loss_epoch["val_loss"].extend(history_callback.history["loss"])
            loss_epoch["epoch"].extend(list(range(epochs)))
            loss_epoch["patience"].extend(list(range(epochs)))
            best_model = model
            epoch = epochs
        else:
            while (patience <=5 and epoch <= epochs and (not self.dafm_type == "round-fine-tuned") and (loaded == False)):
                permutation = np.random.permutation(x_train.shape[0])
                x_train = x_train[permutation]
                y_train = y_train[permutation]
                counter += 1
                if len(x_train_student) == 0:
                    if len(x_train_section) == 0:
                        history_callback = model.fit([virtual_input1, x_train], y_train, batch_size=batch_size, epochs=1, validation_data=([virtual_input1_test, x_test], y_test), verbose=0, shuffle=True)
                    else:
                        x_train_section = x_train_section[permutation]
                        history_callback = model.fit([virtual_input1, x_train, x_train_section], y_train, batch_size=batch_size, epochs=1, validation_data=([virtual_input1_test, x_test, x_test_section], y_test), verbose=0, shuffle=True)
                else:
                    x_train_student = x_train_student[permutation]
                    if len(x_train_section) == 0:
                        history_callback = model.fit([virtual_input1, x_train, x_train_student], y_train, batch_size=batch_size, epochs=1, validation_data=([virtual_input1_test, x_test, x_test_student], y_test), verbose=0, shuffle=True)
                    else:
                        x_train_section = x_train_section[permutation]
                        history_callback = model.fit([virtual_input1, x_train, x_train_section, x_train_student], y_train, batch_size=batch_size, epochs=1, validation_data=([virtual_input1_test, x_test, x_test_section, x_test_student], y_test), verbose=0, shuffle=True)
                current_val_loss = history_callback.history["val_loss"][0]
                print ("Epoch Number:", counter, "Patience:", patience, "val loss:", current_val_loss)
                loss_epoch["val_loss"].append(history_callback.history["val_loss"][0])
                loss_epoch["loss"].append(history_callback.history["loss"][0])
                loss_epoch["epoch"].append(counter)
                loss_epoch["patience"].append(patience)
                if (prev_best_val_loss - current_val_loss) > 0:
                    best_model = model
                    epoch += patience + 1
                    patience = 0
                    prev_best_val_loss = current_val_loss
                else:
                    patience += 1
        if len(x_train_student)==0:
            if len(x_train_section)==0:
                x = self.bce_loss(y_train, best_model.predict([virtual_input1, x_train]), x_train)
            else:
                x = self.bce_loss(y_train, best_model.predict([virtual_input1, x_train, x_train_section]), x_train)
        else:
            if len(x_train_section)==0:
                x = self.bce_loss(y_train, best_model.predict([virtual_input1, x_train, x_train_student]), x_train)
            else:
                x = self.bce_loss(y_train, best_model.predict([virtual_input1, x_train, x_train_section, x_train_student]), x_train)

        L, N = -np.sum(x), len(x)
        model_param = best_model.count_params()
        print ("PARAM", model_param)
        AIC = 2 * model_param  - 2 * L
        BIC = model_param * np.log(N) - 2 * L
        B_k = best_model.get_layer("B_k").get_weights()[0]
        T_k = best_model.get_layer("T_k").get_weights()[0]
        return best_model, AIC, BIC, epoch, loss_epoch

    def fit_batches(self, dafmdata_obj, model, max_epochs=30, earlyStop="val_loss", loaded=False):

        print ("Max Epochs", max_epochs)
        loss_epoch = {"epoch":[], "loss":[], earlyStop:[], 'patience':[]}
        patience, epoch = 0, 1
        prev_best_val_loss = np.inf
        counter = 0
        if self.dafm_type == "round-fine-tuned" or loaded:
            best_model = model

        while (patience <= 2 and epoch <= max_epochs and loaded==False and (not self.dafm_type == "round-fine-tuned")):
            counter += 1
            current_val_loss = 0
            total_loss, total_train_samples = 0, 0
            train = dafmdata_obj.data_generator1("train")
            test = dafmdata_obj.data_generator1("test")
            bc = 0
            for x_train, y_train, x_train_section, x_train_student, batch_size in train:
                permutation = np.random.permutation(x_train.shape[0])
                x_train = x_train[permutation]
                y_train = y_train[permutation]
                virtual_input1 = np.ones([np.shape(x_train)[0], np.shape(x_train)[1], 1])
                print ("Batch Number:", bc, np.shape(x_train))
                if len(x_train_student)==0:
                    if len(x_train_section) == 0:
                        history_callback = model.fit([virtual_input1, x_train], y_train, batch_size=batch_size, epochs=1, verbose=0)
                    else:
                        x_train_section = x_train_section[permutation]
                        history_callback = model.fit([virtual_input1, x_train, x_train_section], y_train, batch_size=batch_size, epochs=1, verbose=1)
                else:
                    x_train_student = x_train_student[permutation]
                    if len(x_train_section) == 0:
                        history_callback = model.fit([virtual_input1, x_train, x_train_student], y_train, batch_size=batch_size, epochs=1, verbose=0)
                    else:
                        x_train_section = x_train_section[permutation]
                        history_callback = model.fit([virtual_input1, x_train, x_train_section, x_train_student], y_train, batch_size=batch_size, epochs=1, verbose=1)
                total_loss += history_callback.history["loss"][0] * len(x_train)
                total_train_samples += len(x_train)
                bc += 1

            if earlyStop == "rmse":
                current_avg_rmse = self.predict_batches(dafmdata_obj, model)
                loss_epoch["rmse"].append(current_avg_rmse)
            else:
                current_avg_rmse = np.mean(self.bce_loss_batches(dafmdata_obj, model, utype="test"))
                loss_epoch["val_loss"].append(current_avg_rmse)
            loss_epoch["loss"].append(float(total_loss)/float(total_train_samples))
            loss_epoch["epoch"].append(counter)
            loss_epoch["patience"].append(patience)

            print ("Epoch Number:", counter, "Patience:", patience, earlyStop, current_avg_rmse, "Loss:", loss_epoch["loss"][-1])
            if (prev_best_val_loss - current_avg_rmse) > 0:
                best_model = model
                epoch += patience + 1
                patience = 0
                prev_best_val_loss = current_avg_rmse
            else:
                patience += 1

        x = self.bce_loss_batches(dafmdata_obj, best_model, utype="train")
        L, N = -np.sum(x), len(x)
        model_param = best_model.count_params()
        AIC = 2 * model_param  - 2 * L
        BIC = model_param * np.log(N) - 2 * L
        return best_model, AIC, BIC, epoch, loss_epoch

    def L(self, y_true, y_pred, x_train):

        mask_matrix = np.sum(x_train, axis=2).flatten()
        num_users, max_responses = np.shape(x_train)[0], np.shape(x_train)[1]
        y_pred = y_pred.flatten()
        y_true = y_true.flatten()
        rmse = []
        SSR = 0
        response = 0
        L = 0
        N = 0
        c = 0
        for user in range(num_users):
            for i in range(user * max_responses, (user + 1) * max_responses):
                if mask_matrix[i] == 0 or y_true[i] == -1:
                    break
                if y_pred[i] < 1 and y_pred[i] > 0:
                    L += ( y_true[i] * np.log(y_pred[i]) + (1 - y_true[i]) * np.log(1 - y_pred[i]) )
                else:
                    c += 1
                    eps = 1e-4
                    if y_pred[i] == y_true[i]:
                        pass
                    else:
                        y_pred[i] = max(eps, min(1 - eps, y_pred[i]))
                        L += ( y_true[i] * np.log(y_pred[i]) + (1 - y_true[i]) * np.log(1 - y_pred[i]) )
                response += 1
                N += 1
        return L, N

    def L_batches(self, dafmdata_obj, model):

        L = 0
        N = 0
        train_generator = dafmdata_obj.data_generator1("train")
        for x_train, y_train, x_train_section, x_train_student, batch_size in train_generator:
            virtual_input1 = np.ones([np.shape(x_train)[0], np.shape(x_train)[1], 1])
            if len(x_train_student)==0:
                if len(x_train_section) == 0:
                    l, x= self.L(y_train, model.predict([virtual_input1, x_train]), x_train)
                    L += l
                else:
                    l, x= self.L(y_train, model.predict([virtual_input1, x_train, x_train_section]), x_train)
                    L += l
            else:
                if len(x_train_section) == 0:
                    l, x= self.L(y_train, model.predict([virtual_input1, x_train, x_train_student]), x_train)
                    L += l
                else:
                    l, x= self.L(y_train, model.predict([virtual_input1, x_train, x_train_section, x_train_student]), x_train)
                    L += l
            N += len(x_train)
        return L, N

    def predict(self, x_test, y_test, x_test_section, x_test_student, model, batch_size=32):

        virtual_input_test = np.ones([np.shape(x_test)[0], np.shape(x_test)[1], 1])
        if len(x_test_student)==0:
            if len(x_test_section)==0:
                y_pred = model.predict([virtual_input_test, x_test], batch_size=batch_size)
            else:
                y_pred = model.predict([virtual_input_test, x_test, x_test_section] , batch_size=batch_size)
        else:
            if len(x_test_section)==0:
                y_pred = model.predict([virtual_input_test, x_test, x_test_student], batch_size=batch_size)
            else:
                y_pred = model.predict([virtual_input_test, x_test, x_test_section, x_test_student] , batch_size=batch_size)
        rmse =  self.rmse_masking(y_test, y_pred, x_test)
        return rmse

    def prediction(self, x_test, x_test_section, x_test_student, model, batch_size=32):

        virtual_input_test = np.ones([np.shape(x_test)[0], np.shape(x_test)[1], 1])
        if len(x_test_student)==0:
            if len(x_test_section)==0:
                y_pred = model.predict([virtual_input_test, x_test], batch_size=batch_size)
            else:
                y_pred = model.predict([virtual_input_test, x_test, x_test_section] , batch_size=batch_size)
        else:
            if len(x_test_section)==0:
                y_pred = model.predict([virtual_input_test, x_test, x_test_student], batch_size=batch_size)
            else:
                y_pred = model.predict([virtual_input_test, x_test, x_test_section, x_test_student], batch_size=batch_size)
        return y_pred

    def predict_batches(self, dafmdata_obj, model):

        test_generator = dafmdata_obj.data_generator1("test")
        avg_rmse = 0
        t_users = 0
        for x_test, y_test, x_test_section, x_test_student, batch_size in test_generator:
            avg_rmse =  avg_rmse + len(x_test)*self.predict(x_test, y_test, x_test_section, x_test_student, model, batch_size)
            t_users = t_users + len(x_test)
        return avg_rmse/float(t_users)

    def bce_loss_batches(self, dafmdata_obj, model, utype="train"):

        ll = []
        test_generator = dafmdata_obj.data_generator1(utype)
        for x_test, y_test, x_test_section, x_test_student, batch_size in test_generator:
            virtual_input_test = np.ones([np.shape(x_test)[0], np.shape(x_test)[1], 1])
            if len(x_test_student) == 0:
                if len(x_test_section) == 0:
                    ll.extend(self.bce_loss(y_test, model.predict([virtual_input_test, x_test], batch_size=batch_size), x_test))
                else:
                    ll.extend(self.bce_loss(y_test, model.predict([virtual_input_test, x_test, x_test_section], batch_size=batch_size), x_test))
            else:
                if len(x_test_section) == 0:
                    ll.extend(self.bce_loss(y_test, model.predict([virtual_input_test, x_test, x_test_student], batch_size=batch_size), x_test))
                else:
                    ll.extend(self.bce_loss(y_test, model.predict([virtual_input_test, x_test, x_test_section, x_test_student], batch_size=batch_size), x_test))
        return ll

    def bce_loss(self, y_true, y_pred, x_test):

        mask_matrix = np.sum(x_test, axis=2).flatten()
        num_users, max_responses = np.shape(x_test)[0], np.shape(x_test)[1]
        y_pred = y_pred.flatten()
        y_true = y_true.flatten()
        ll = []
        response = 0
        for user in range(num_users):
            log_loss = []
            for i in range(user * max_responses, (user + 1) * max_responses):
                if mask_matrix[i] == 0 or y_true[i] == -1:
                    break
                response += 1
                eps = 1e-7
                y_pred[i] = max(eps, min(1 - eps, y_pred[i]))
                log_loss.append( -( y_true[i] * np.log(y_pred[i]) + (1 - y_true[i]) * np.log(1 - y_pred[i]) ) )
            ll.extend(log_loss)
        return ll

    def rmse_masking(self, y_true, y_pred, x_test):

        mask_matrix = np.sum(x_test, axis=2).flatten()
        num_users, max_responses = np.shape(x_test)[0], np.shape(x_test)[1]
        y_pred = y_pred.flatten()
        y_true = y_true.flatten()
        rmse = []
        for user in range(num_users):
            diff_sq, response = 0, 0
            for i in range(user * max_responses, (user + 1) * max_responses):
                if mask_matrix[i] == 0 or y_true[i] == -1:
                    continue
                    # continue for response level evaluation
                diff_sq += (y_true[i] - y_pred[i]) ** 2
                response += 1
            rmse.append(sqrt(diff_sq/float(response)))
        return np.mean(rmse)

if __name__ == "__main__":

    x_train = [ [ [0, 0, 1], [0, 0, 1], [1, 0, 0], [0, 0, 0] ],
                [ [1, 0, 0], [0, 1, 0], [1, 0, 0], [0, 0, 0] ],
                [ [0, 0, 1], [1, 0, 0], [0, 1, 0], [0, 0, 1] ],
                [ [1, 0, 0], [1, 0, 0], [0, 0, 1], [1, 0, 0] ] ]
    x_test = [ [ [ 1, 0, 0], [0, 1, 0], [0, 1, 0], [0, 0, 1] ] ]
    y_test = [ [ [-1], [-1], [-1], [-1] ] ]

    y_train = [ [ [0], [0], [1], [-1] ],
                [ [1], [0], [1], [-1] ],
                [ [0], [0], [0], [0] ],
                [ [0], [1], [0], [0] ] ]
    Q_jk_initialize = np.random.rand(3,2)
    Q_jk_initialize = np.array([[1, 0], [0, 1], [1, 1]])
    obj = DAFM(np.array(x_train), np.array(y_train), np.array(x_test), np.array(y_test), Q_jk_initialize, skills=2, steps=3)
    model = obj.build(qtrainable=False, finetuning=False, loaded=False, dftype="")
    obj.predict(np.array(x_test), np.array(y_test), model)
