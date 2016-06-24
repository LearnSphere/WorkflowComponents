from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import
from __future__ import division
from math import log
from math import exp

import numpy as np

def log_one_plus_exp(z):
    """
    This function returns log(1 + exp(z)) where it rewrites the terms to reduce
    floating point errors.
    """
    if z > 0:
        return log(1 + exp(-z)) + z
    else:
        return log(1 + exp(z))

def invlogit(z):
    """
    This function return 1 / (1 + exp(-z)) where it rewrites the terms to
    reduce floating point errors.
    """
    if z > 0:
        return 1 / (1 + exp(-z))
    else:
        return exp(z) / (1 + exp(z))

invlogit_vect = np.vectorize(invlogit)
log_one_plus_exp_vect = np.vectorize(log_one_plus_exp)


