from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import
from __future__ import division
from numbers import Number

import numpy as np
import math
from sklearn.base import BaseEstimator
from sklearn.utils.validation import check_X_y
from sklearn.utils.validation import check_array
from scipy.optimize import minimize

from util import invlogit_vect

class BoundedLogistic(BaseEstimator):

    def __init__(self, fit_first_intercept=True, fit_second_intercept=True,
                 first_bounds=None, second_bounds=None, first_l2=1.0,
                 second_l2=1.0, method="TNC", max_iter=1000):
        """
        Bounded Logistic Regression with box constraints (i.e., bounds on
        estimates) and l2 regularization.

        If box constraints are being used, then the only supported methods are:
            'l-bfgs-b', 'TNC', or 'SLSQP'
        """
        self.fit_first_intercept = fit_first_intercept
        self.fit_second_intercept = fit_second_intercept
        self.method = method
        self.first_bounds = first_bounds
        self.second_bounds = second_bounds
        self.first_l2 = first_l2
        self.second_l2 = second_l2
        self.max_iter = max_iter

    def fit(self, X, X2, y):
        """
        Train the Logistic model, X and y are numpy arrays.
        """
        X, y = check_X_y(X, y) 
        X2, y = check_X_y(X2, y) 
        self.classes_, y = np.unique(y, return_inverse=True)

        if self.fit_first_intercept:
            X = np.insert(X, 0, 1, axis=1)
        if self.fit_second_intercept:
            X2 = np.insert(X2, 0, 1, axis=1)

        w0 = np.zeros(X.shape[1] + X2.shape[1])

        if self.first_bounds is None:
            self.first_bounds_ = [(None, None) for i in range(X.shape[1])]
        elif isinstance(self.first_bounds, tuple) and len(self.first_bounds) == 2:
            self.first_bounds_ = [self.first_bounds for i in range(X.shape[1])]
        elif self.fit_first_intercept and len(self.first_bounds) == X.shape[1] - 1:
            self.first_bounds_ = np.concatenate(([(None, None)],
                                                self.first_bounds))
        else:
            self.first_bounds_ = self.first_bounds

        if self.second_bounds is None:
            self.second_bounds_ = [(None, None) for i in range(X2.shape[1])]
        elif isinstance(self.first_bounds, tuple) and len(self.second_bounds) == 2:
            self.second_bounds_ = [self.second_bounds for i in range(X2.shape[1])]
        elif self.fit_second_intercept and len(self.second_bounds) == X2.shape[1] - 1:
            self.second_bounds_ = np.concatenate(([(None, None)],
                                                 self.second_bounds))
        else:
            self.second_bounds_ = self.second_bounds

        self.bounds_ = np.concatenate((self.first_bounds_,
                                       self.second_bounds_))
        if len(self.bounds_) != len(w0):
            raise ValueError("Bounds must be the same length as the coef")

        if isinstance(self.first_l2, Number):
            self.first_l2_ = [self.first_l2 for i in range(X.shape[1])]
        elif self.fit_first_intercept and len(self.first_l2) == X.shape[1] - 1:
            self.first_l2_ = np.insert(self.first_l2, 0, 0)
        else:
            self.first_l2_ = self.first_l2

        if isinstance(self.second_l2, Number):
            self.second_l2_ = [self.second_l2 for i in range(X2.shape[1])]
        elif self.fit_second_intercept and len(self.second_l2) == X2.shape[1] - 1:
            self.second_l2_ = np.insert(self.second_l2, 0, 0)
        else:
            self.second_l2_ = self.second_l2

        # the intercept should never be regularized.
        if self.fit_first_intercept:
            self.first_l2_[0] = 0.0
        if self.fit_second_intercept:
            self.second_l2_[0] = 0.0

        self.l2_ = np.concatenate((self.first_l2_, self.second_l2_), axis=0)

        if len(self.l2_) != len(w0):
            raise ValueError("L2 penalty must be the same length as coef, be sure the intercept is accounted for.")

        #start of modification and addition
        w = minimize(_ll, w0, args=(X, X2, y, self.l2_),
                               jac=_ll_grad, 
                               method=self.method, bounds=self.bounds_,
                               options={'maxiter': self.max_iter, 
                                        #'disp': True
                               })
        negOfLl = w.fun
        w = w['x']
        self.ll = (-1)*negOfLl
        self.nDataPoints = len(y)
        self.nPars = len(w0)
        #formula for AIC: -2 * loglikelihood + 2 * nPars;
        #formula for BIC: -2 * loglikelihood + nPars * log(nDataPoints);
        
        self.aic = (-2)*self.ll + 2*self.nPars
        self.bic = (-2)*self.ll + self.nPars*math.log(self.nDataPoints)
        #end of modification and addition
        
        w1 = w[:X.shape[1]]
        w2 = w[X.shape[1]:]

        if self.fit_first_intercept:
            self.intercept1_ = w1[0:1]
            self.coef1_ = w1[1:]
        else:
            self.intercept1_ = np.array([])
            self.coef1_ = w1

        if self.fit_second_intercept:
            self.intercept2_ = w2[0:1]
            self.coef2_ = w2[1:]
        else:
            self.intercept2_ = np.array([])
            self.coef2_ = w2

        return self

    def predict(self, X, X2):
        """
        Returns the predicted class for each x in X, predicts 1 if probability
        is greater than or equal to 1.
        """
        y = np.array(self.predict_proba(X, X2))
        y[y >= 0.5] = self.classes_[1] 
        y[y < 0.5] = self.classes_[0] 
        return y 

    def predict_proba(self, X, X2):
        """
        Returns the probability of class 1 for each x in X.
        """
        try:
            getattr(self, "intercept1_")
            getattr(self, "intercept2_")
            getattr(self, "coef1_")
            getattr(self, "coef2_")
        except AttributeError:
            raise RuntimeError("You must train classifer before predicting data!")

        X = check_array(X)
        X2 = check_array(X2)

        if self.fit_first_intercept:
            X = np.insert(X, 0, 1, axis=1)
        if self.fit_second_intercept:
            X2 = np.insert(X2, 0, 1, axis=1)

        w = np.insert(self.coef1_, 0, self.intercept1_)
        w2 = np.insert(self.coef2_, 0, self.intercept2_)
        return (invlogit_vect(np.dot(w, np.transpose(X))) *
                invlogit_vect(np.dot(w2, np.transpose(X2))))

    def mean_squared_error(self, X, X2, y):
        pred = self.predict_proba(X, X2)
        sq_err = [(v-pred[i]) * (v-pred[i]) for i,v in enumerate(y)]
        return np.average(sq_err)

def _ll(w, X, X2, y, l2):
    """
    Logistic Regression loglikelihood given, the weights w, the data X, and the
    labels y.
    """
    w1 = w[:X.shape[1]]
    w2 = w[X.shape[1]:]

    p1 = invlogit_vect(np.dot(w1, np.transpose(X)))
    p2 = invlogit_vect(np.dot(w2, np.transpose(X2)))
    p = np.multiply(p1, p2)
    
    ll = -sum(np.multiply(y, np.log(p)))
    ll -= sum(np.multiply(1-y, np.log(1-p)))
    ll += np.dot(np.divide(l2, 2), np.multiply(w, w))
    return ll

def _ll_grad(w, X, X2, y, l2):
    """
    Logistic Regression loglikelihood gradient given, the weights w, the data
    X, and the labels y.
    """
    w1 = w[:X.shape[1]]
    w2 = w[X.shape[1]:]
    z1 = np.dot(w1, np.transpose(X))
    z2 = np.dot(w2, np.transpose(X2))
    
    p1 = invlogit_vect(z1)
    p2 = invlogit_vect(z2)
    p = np.multiply(p1, p2)

    g1 = np.dot(np.transpose(X), 
                np.multiply(invlogit_vect(-z1), 
                            np.divide(np.subtract(y, p), 
                                      np.subtract(1, p))))
    g2 = np.dot(np.transpose(X2), 
                np.multiply(invlogit_vect(-z2), 
                            np.divide(np.subtract(y, p), 
                                      np.subtract(1, p))))
    
    g = np.concatenate((g1, g2))
    g -= np.multiply(l2, w)
    return -1 * g

