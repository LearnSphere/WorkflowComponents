# -*- coding: utf-8 -*-
import numpy as np

from keras import backend as K

from keras.layers import InputSpec, Layer, Dense, Conv2D
from keras import constraints
from keras import initializers
from keras import regularizers


from DAFM.binary_ops import binarize


class Clip(constraints.Constraint):
    def __init__(self, min_value, max_value=None):
        self.min_value = min_value
        self.max_value = max_value
        if not self.max_value:
            self.max_value = -self.min_value
        if self.min_value > self.max_value:
            self.min_value, self.max_value = self.max_value, self.min_value

    def __call__(self, p):
        return K.clip(p, self.min_value, self.max_value)

    def get_config(self):
        return {"min_value": self.min_value,
                "max_value": self.max_value}


class BinaryDense2(Dense):
    ''' Binarized Dense layer
    References:
    "BinaryNet: Training Deep Neural Networks with Weights and Activations Constrained to +1 or -1" [http://arxiv.org/abs/1602.02830]
    '''

    def __init__(self, units, H=1., w_lr_multiplier='Glorot', bias_lr_multiplier=None, **kwargs):
        super(BinaryDense2, self).__init__(units, **kwargs)
        self.H = H
        self.w_lr_multiplier = w_lr_multiplier
        self.bias_lr_multiplier = bias_lr_multiplier

        super(BinaryDense2, self).__init__(units, **kwargs)

    def build(self, input_shape):
        # input_shape (None,40)
        print(input_shape)

        input_dim = input_shape[1]
        if self.H == 'Glorot':
            self.H = np.float32(np.sqrt(1.5 / (int(input_dim) + self.units)))
            # print('Glorot H: {}'.format(self.H))
        if self.w_lr_multiplier == 'Glorot':
            self.w_lr_multiplier = np.float32(1. / np.sqrt(1.5 / (int(input_dim) + self.units)))
            # print('Glorot learning rate multiplier: {}'.format(self.kernel_lr_multiplier))

        self.w_constraint = Clip(-self.H, self.H)
        self.w_initializer = initializers.RandomUniform(-self.H, self.H)
        self.w_regularizer = regularizers.l2(0.01)
        self.w = self.add_weight(shape=(input_dim, self.units),
                                 initializer=self.w_initializer,
                                 name='w',
                                 regularizer=self.w_regularizer,
                                 constraint=self.w_constraint)

        if self.use_bias:
            self.lr_multipliers = [self.w_lr_multiplier, self.bias_lr_multiplier]
            self.bias = self.add_weight(shape=(self.units,),  # is this shape right??? # 假设这个weight每一个都不一样好了先！
                                        initializer=self.w_initializer,
                                        name='bias')
            # regularizer=self.bias_regularizer,
            # constraint=self.bias_constraint)

        else:
            self.lr_multipliers = [self.w_lr_multiplier]
            self.bias = None

        self.input_spec = InputSpec(min_ndim=2, axes={-1: input_dim})
        self.built = True
        self.binary_weight = binarize(self.w, H=self.H)

    def call(self, inputs):
        #binary_w = binarize(self.w, H=self.H)
        binary_w =self.binary
        output = K.dot(inputs, binary_w)
        if self.use_bias:
            output = K.bias_add(output, self.bias)
        if self.activation is not None:
            output = self.activation(output)
        return output

    def get_config(self):
        config = {'H': self.H,
                  'w_lr_multiplier': self.w_lr_multiplier,
                  'bias_lr_multiplier': self.bias_lr_multiplier}
        base_config = super(BinaryDense2, self).get_config()
        return dict(list(base_config.items()) + list(config.items()))


# keras在compute gradient的时候 似乎是针对没有binary之前的weight

class BinaryDense(Dense):
    ''' Binarized Dense layer
    References: 
    "BinaryNet: Training Deep Neural Networks with Weights and Activations Constrained to +1 or -1" [http://arxiv.org/abs/1602.02830]
    '''
    def __init__(self, units, H=1., w_lr_multiplier='Glorot', bias_lr_multiplier=None, **kwargs):
        super(BinaryDense, self).__init__(units, **kwargs)
        self.H = H
        self.w_lr_multiplier = w_lr_multiplier
        self.bias_lr_multiplier = bias_lr_multiplier
        
        super(BinaryDense, self).__init__(units, **kwargs)
    
    def build(self, input_shape):
        # input_shape (None,40)
        print (input_shape)

        input_dim = input_shape[1]
        if self.H == 'Glorot':
            self.H = np.float32(np.sqrt(1.5 / (int(input_dim) + self.units)))
            #print('Glorot H: {}'.format(self.H))
        if self.w_lr_multiplier == 'Glorot':
            self.w_lr_multiplier = np.float32(1. / np.sqrt(1.5 / (int(input_dim) + self.units)))
            #print('Glorot learning rate multiplier: {}'.format(self.kernel_lr_multiplier))

        self.w_constraint = Clip(-self.H, self.H)
        #self.w_initializer = initializers.RandomUniform(-self.H, self.H)
        self.w_initializer = initializers.Ones()
        self.w_regularizer =regularizers.l2(0.01)
        self.w = self.add_weight(shape=(input_dim,self.units),
                                     initializer=self.w_initializer,
                                     name='w',
                                     regularizer=self.w_regularizer,
                                     constraint=self.w_constraint)
        #self.bw=self.add_weight(shape=(input_dim,self.units),
        #                             initializer=self.w_initializer,
        #                             name='bw',
        #                             regularizer=self.w_regularizer,
        #                             constraint=self.w_constraint)
        #self.bw = binarize(self.w, H=self.H)

        if self.use_bias:
            self.lr_multipliers = [self.w_lr_multiplier, self.bias_lr_multiplier]
            self.bias = self.add_weight(shape=(self.units,), # is this shape right??? # 假设这个weight每一个都不一样好了先！
                                     initializer=self.w_initializer,
                                     name='bias')
                                     #regularizer=self.bias_regularizer,
                                     #constraint=self.bias_constraint)

        else:
            self.lr_multipliers = [self.w_lr_multiplier]
            self.bias = None

        self.input_spec = InputSpec(min_ndim=2, axes={-1: input_dim})
        self.built = True
        self.binary=binarize(self.w, H=self.H)


    def call(self, inputs):
        #print('！！',type(self.w))
        #binary_w = binarize(self.w, H=self.H)
        output = K.dot(inputs,self.binary)
        #output = K.dot(inputs,self.bw)
        if self.use_bias:
            output = K.bias_add(output, self.bias)
        if self.activation is not None:
            print('-------------------------------------------------')
            print(self.activation)
            print('-------------------------------------------------')

            output = self.activation(output)
        return output
        
    def get_config(self):
        config = {'H': self.H,
                  'w_lr_multiplier': self.w_lr_multiplier,
                  'bias_lr_multiplier': self.bias_lr_multiplier}
        base_config = super(BinaryDense, self).get_config()
        return dict(list(base_config.items()) + list(config.items()))

    def get_binary(self):
        return self.binary

# keras在compute gradient的时候 似乎是针对没有binary之前的weight

class BinaryDense3(Dense):
    ''' Binarized Dense layer
    References:
    "BinaryNet: Training Deep Neural Networks with Weights and Activations Constrained to +1 or -1" [http://arxiv.org/abs/1602.02830]
    '''

    def __init__(self, units, H=1., kernel_lr_multiplier='Glorot', bias_lr_multiplier=None, **kwargs):
        super(BinaryDense3, self).__init__(units, **kwargs)
        self.H = H
        self.kernel_lr_multiplier = kernel_lr_multiplier
        self.bias_lr_multiplier = bias_lr_multiplier

        super(BinaryDense3, self).__init__(units, **kwargs)

    def build(self, input_shape):
        # input_shape (None,40)
        print (input_shape)

        input_dim = input_shape[1]
        self.kernel = self.add_weight(shape=(input_dim, self.units),
                                      initializer=self.kernel_initializer,
                                      name='kernel',
                                      regularizer=self.kernel_regularizer,
                                      constraint=self.kernel_constraint)

        if self.H == 'Glorot':
            self.H = np.float32(np.sqrt(1.5 / (int(input_dim) + self.units)))
            # print('Glorot H: {}'.format(self.H))
        if self.kernel_lr_multiplier == 'Glorot':
            self.kernel_lr_multiplier = np.float32(1. / np.sqrt(1.5 / (int(input_dim) + self.units)))
            # print('Glorot learning rate multiplier: {}'.format(self.kernel_lr_multiplier))

        if self.use_bias:
            pass
        else:
            self.lr_multipliers = [self.kernel_lr_multiplier]
            self.bias = None

        self.input_spec = InputSpec(min_ndim=2, axes={-1: input_dim})
        self.built = True
        self.binary = binarize(self.kernel, H=self.H)

    def call(self, inputs):
        # print('！！',type(self.w))
        # binary_w = binarize(self.w, H=self.H)
        output = K.dot(inputs, self.binary)
        # output = K.dot(inputs,self.bw)
        if self.use_bias:
            output = K.bias_add(output, self.bias)
        if self.activation is not None:
            print('-------------------------------------------------')
            print(self.activation)
            print('-------------------------------------------------')

            output = self.activation(output)
        return output

    def get_config(self):
        config = {'H': self.H,
                  'kernel_lr_multiplier': self.kernel_lr_multiplier,
                  'bias_lr_multiplier': self.bias_lr_multiplier}
        base_config = super(BinaryDense3, self).get_config()
        return dict(list(base_config.items()) + list(config.items()))

    def get_binary(self):
        return self.binary
