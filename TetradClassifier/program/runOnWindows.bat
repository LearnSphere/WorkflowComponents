FOR %%A IN (%*) DO (
     ECHO BATCH ARG: %%A
)

"C:/java23/bin/java.exe" -cp C:\Users\Peter\WorkflowsAndTetrad\WorkflowComponents\CustomLibraries\Tetrad\tetrad-gui-6.5.4-launch.jar -jar C:\WPIDevelopment\dev06_dev\WorkflowComponents_new\TetradClassifier\dist\TetradClassifier.jar %*
