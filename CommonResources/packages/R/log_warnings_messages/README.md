Carnegie Mellon University, Massachusetts Institute of Technology, Stanford University, University of Memphis.
Copyright 2016. All Rights Reserved.

# Purpose of this package

This package can be used in other R scripts that run on LearnSphere Workflow. The purpose is to output R's warnings and messages to a workflow log file (.wfl) 
so LearnSphere Workflow platform can run correctly and capture the warnings and messages for Workflow author to read.
  
# How to Install this Package and Dependencies

1. This package requires rlang version at least 0.3.4. To install rlang, in R, run as Administrator or sudo:
options(repos=structure(c(CRAN="http://cran.cnr.berkeley.edu/")))
install.packages(("rlang", lib="/usr/local/lib/R/3.3/site-library")

2. After rlang is installed successfully, install logWarningsMessages package:
install.packages("logWarningsMessagesPkg_0.1.0.tar.gz", repos = NULL, type="source", lib="C:/Program Files/R/R-3.4.1/library")

# How to Use it in your own R file

1. Put the followings as the first two libraries to install, so they will not mask other libraries' functions. rlang is required
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(rlang))

2. Examples of how to call logWarningsMessages() from logWarningsMessagesPkg. 
logWarningsMessages(fit_model <- lm(x~y+z), logFileName = "my_log_file_name.wfl")
or
logWarningsMessages(eval(parse(text=modelingString)), logFileName = "my_log_file_name.wfl")
In the examples, all warnings and messages that are generated can be found in file: my_log_file_name.wfl. Since this file has extension .wfl, it's content 
are picked up by Workflow web application UI interface. 
