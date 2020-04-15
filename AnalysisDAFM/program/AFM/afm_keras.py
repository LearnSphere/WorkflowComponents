from tensorflow.python.keras.layers import Input, Dense
from tensorflow.python.keras.models import Model, Sequential
from tensorflow.python.keras.callbacks import EarlyStopping
from sklearn.metrics import mean_squared_error
from math import sqrt
import numpy as np
np.random.seed(29)
"""
Input: X_train 2-D array
       Y_train 1-D array
"""

class AFMK:

    def __init__(self):

        self.batch_size = 64
        self.epochs = 500
        self.validation_split = 0.2

    def fit(self, X_train, Y_train):

        model = Sequential()
        model.add(Dense(1, input_dim=len(X_train[0]), activation='sigmoid'))
        model.compile(optimizer='rmsprop',
                      loss='binary_crossentropy',
                      metrics=['accuracy'])

        earlyStopping = EarlyStopping(monitor='val_loss', patience=5, verbose=1, mode='auto')
        model.fit(X_train, Y_train, verbose=0, batch_size = self.batch_size, epochs=self.epochs, callbacks = [earlyStopping], validation_split = self.validation_split, shuffle = True)

        y_val = model.predict(X_train)
        index_one = 0
        acc_y = np.array([float(y_val[i][index_one]) for i in range(len((y_val)))])
        SSR = sum([(Y_train[i]-acc_y[i]) ** 2 for i in range(len(acc_y))])
        N = len(Y_train)
        s2 = SSR / float(N)
        L =  ( N * np.log(1.0/np.sqrt(2*np.pi*s2)) - (1.0/(2*s2) )*SSR )
        AIC = 2*(model.count_params())  - 2 * L
        BIC = (model.count_params()) * np.log(N) - 2 * L
        return model, AIC, BIC

    def predict(self, X_test, Y_test, model, d_t):

        y_val = model.predict(X_test)
        index_one = 0
        acc_y = np.array([float(y_val[i][index_one]) for i in range(len((y_val)))])
        rmse_avg = self.rmse_avg(model, acc_y, d_t, Y_test)
        return rmse_avg

    def rmse_avg(self, model, acc_y, d_t, Y_test):

        rmse = []
        for dummy, l in d_t.items():
            if len(l) == 0:
                    continue
            rmse.append(sqrt(mean_squared_error(Y_test[l], acc_y[l])))
        return np.mean(rmse)

if __name__ == "__main__":

    x = [[1,0,0,1], [1,0,0,0],[0,1,1,0], [1,0,1,0]]
    y = [[1, 0],[0, 1],[1, 0]]
    y = [0, 1, 0, 1]
    obj = AFMK()
    m = obj.fit(x,y)
    print ("Model Fitted")
