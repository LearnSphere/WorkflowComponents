try:
    from AFM.liblinear.python.liblinearutil import *
except:
    pass

import numpy as np
from sklearn.metrics import mean_squared_error
from math import sqrt
"""
X_train: 2-D array
Y_train: 1-D array
"""
class AFML:
    
    def fit(self, X_train, Y_train):
    
        m = train(problem(Y_train, X_train), '-s 6')
        labels, p_acc, y_val = predict(Y_train, X_train, m, '-b 1')
        index_one = list(m.get_labels()).index(1)
        acc_y = np.array([float(y_val[i][index_one]) for i in range(len((y_val)))])
        SSR = mean_squared_error(Y_train, acc_y)
        N = len(Y_train)
        s2 = SSR / float(N)
        L =  ( N * np.log(1.0/np.sqrt(2*np.pi*s2)) - (1.0/(2*s2) )*SSR )
        AIC = 2*(1+len(X_train[0]))  - 2 * L
        BIC = (1+len(X_train[0])) * np.log(N) - 2 * L         
        return m, AIC, BIC
        
    def predict(self, X_test, Y_test, m, d_t):

        labels, p_acc, y_val = predict(Y_test, X_test, m, '-b 1')
        index_one = list(m.get_labels()).index(1)
        acc_y = np.array([float(y_val[i][index_one]) for i in range(len((y_val)))])
        rmse_avg = self.rmse_avg(m, acc_y, d_t, Y_test)
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
        print ("Model Trained")
