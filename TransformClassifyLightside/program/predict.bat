@echo off

rem Usage: scripts/predict.sh path/to/saved/model.xml [{data-encoding} path/to/unlabeled/data.csv...]
rem Outputs tab-separated predictions for new instances, using the given model. (instance number, prediction, text)
rem If no new data file is given, instances are read from the standard input.
rem Common tab encodings are UTF-8, windows-1252, and MacRoman.
rem (Make sure that the text columns and any columns used as features 
rem  have the same names in the new data as they did in the training set.)


set MAXHEAP=1024m
set OS_ARGS=
rem OTHER_ARGS=""
set OTHER_ARGS=-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -Djava.awt.headless=true

set MAIN_CLASS=edu.cmu.side.recipe.Predictor

set CLASSPATH="bin;lib/*;lib/xstream/*;wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar;wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar;wekafiles/packages/LibLINEAR/lib/liblinear-1.8.jar;wekafiles/packages/LibLINEAR/LibLINEAR.jar;wekafiles/packages/LibSVM/lib/libsvm.jar;wekafiles/packages/LibSVM/LibSVM.jar;plugins/genesis.jar"
        
java  %OS_ARGS% -Xmx%MAXHEAP% %OTHER_ARGS% -cp %CLASSPATH% %MAIN_CLASS% %*

