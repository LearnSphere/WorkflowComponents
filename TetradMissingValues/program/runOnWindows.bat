FOR %%A IN (%*) DO (
     ECHO BATCH ARG: %%A
)

"C:/java23/bin/java.exe" -classpath 'C:\WPIDevelopment\dev06_dev\WorkflowComponents_new\CustomLibraries\Tetrad\tetrad-gui-6.5.4-launch.jar' -jar C:\WPIDevelopment\dev06_dev\WorkflowComponents_new\TetradMissingValues\dist\TetradMissingValues.jar %*
