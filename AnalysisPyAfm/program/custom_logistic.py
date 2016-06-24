from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import
from __future__ import division
from numbers import Number

import numpy as np
from sklearn.base import BaseEstimator
from sklearn.base import ClassifierMixin
from sklearn.utils.validation import check_X_y
from sklearn.utils.validation import check_array

from scipy.optimize import minimize

from util import log_one_plus_exp_vect
from util import invlogit_vect

class CustomLogistic(BaseEstimator, ClassifierMixin):

    def __init__(self, fit_intercept=True, method="TNC", bounds=None,
                 l2=1.0, max_iter=1000):
        """
        My own logistic regression that allows me to pass in box constraints
        (i.e., bounds on estimates) and use l2 regularization.

        If box constraints are being used, then the only supported methods are:
            'l-bfgs-b', 'TNC', or 'SLSQP'
        """
        self.fit_intercept = fit_intercept
        self.method = method
        self.bounds = bounds
        self.l2 = l2
        self.max_iter = max_iter

    def fit(self, X, y):
        """
        Train the Logistic model, X and y are numpy arrays.
        """
        X, y = check_X_y(X, y) 
        #, accept_sparse=['csr', 'csc']) # not sure how to handle sparse
        self.classes_, y = np.unique(y, return_inverse=True)

        if self.fit_intercept:
            X = np.insert(X, 0, 1, axis=1)

        w0 = np.zeros(X.shape[1])

        if self.bounds is None:
            self.bounds_ = [(None, None) for v in w0]
        elif isinstance(self.bounds, tuple) and len(self.bounds) == 2:
            self.bounds_ = [self.bounds for v in w0]
        elif self.fit_intercept and len(self.bounds) == len(w0) - 1:
            self.bounds_ = np.concatenate(([(None, None)], self.bounds))
        else:
            self.bounds_ = self.bounds
        if len(self.bounds_) != len(w0):
            raise ValueError("Bounds must be the same length as the coef")

        if isinstance(self.l2, Number):
            self.l2_ = [self.l2 for v in w0]
        elif self.fit_intercept and len(self.l2) == len(w0) - 1:
            self.l2_ = np.insert(self.l2, 0, 0)
        else:
            self.l2_ = self.l2
        if len(self.l2_) != len(w0):
            raise ValueError("L2 penalty must be the same length as the coef, be sure the intercept is accounted for.")

        # the intercept should never be regularized.
        if self.fit_intercept:
            self.l2_[0] = 0.0

        w = minimize(_ll, w0, args=(X, y, self.l2_),
                               jac=_ll_grad, 
                               method=self.method, bounds=self.bounds_,
                               options={'maxiter': self.max_iter, 
                                        #'disp': True
                               })['x']

        if self.fit_intercept:
            self.intercept_ = w[0:1]
            self.coef_ = w[1:]
        else:
            self.intercept_ = np.array([])
            self.coef_ = w
        return self

    def predict(self, X):
        """
        Returns the predicted class for each x in X, predicts 1 if probability
        is greater than or equal to 1.
        """
        y = np.array(self.predict_proba(X))
        y[y >= 0.5] = self.classes_[1] 
        y[y < 0.5] = self.classes_[0] 
        return y 

    def predict_proba(self, X):
        """
        Returns the probability of class 1 for each x in X.
        """
        try:
            getattr(self, "intercept_")
            getattr(self, "coef_")
        except AttributeError:
            raise RuntimeError("You must train classifer before predicting data!")

        X = check_array(X)
        if self.fit_intercept:
            X = np.insert(X, 0, 1, axis=1)

        w = np.insert(self.coef_, 0, self.intercept_)
        return invlogit_vect(np.dot(w, np.transpose(X)))

    def mean_squared_error(self, X, y):
        pred = self.predict_proba(X)
        sq_err = [(v-pred[i]) * (v-pred[i]) for i,v in enumerate(y)]
        return np.average(sq_err)

def _ll(w, X, y, l2):
    """
    Logistic Regression loglikelihood given, the weights w, the data X, and the
    labels y.
    """
    z = np.dot(w, np.transpose(X))
    ll = sum(np.subtract(log_one_plus_exp_vect(z), np.multiply(y, z)))
    ll += np.dot(np.divide(l2, 2), np.multiply(w, w))
    return ll

def _ll_grad(w, X, y, l2):
    """
    Logistic Regression loglikelihood gradient given, the weights w, the data
    X, and the labels y.
    """
    p = invlogit_vect(np.dot(w, np.transpose(X)))
    g = np.dot(np.transpose(X), np.subtract(y, p))
    g -= np.multiply(l2, w)
    return -1 * g 
