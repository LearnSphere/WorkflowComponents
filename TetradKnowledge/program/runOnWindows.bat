FOR %%A IN (%*) DO (
     ECHO BATCH ARG: %%A
)

"C:/Program Files/Java/jdk1.8.0_91/bin/java.exe" -cp C:\Users\peter\Documents\CMURound2\WorkflowComponents\TetradKnowledge\program\tetrad-gui-6.3.4-launch.jar -jar C:\Users\peter\Documents\CMURound2\WorkflowComponents\TetradKnowledge\program\TetradKnowledge.jar %*
