FOR %%A IN (%*) DO (
     ECHO BATCH ARG: %%A
)

"C:/java23/bin/java.exe" -jar C:\WPIDevelopment\dev06_dev\WorkflowComponents_new\TetradRegression\dist\TetradRegression.jar %*
