cd program
rem copy "C:\Users\peter\Documents\CMURound2\PetersTetradFiles\TetradComponent.jar" "C:\Users\peter\Documents\CMURound2\WorkflowComponents\TetradMissingValues\program\"
javac -classpath .;tetrad-gui-6.3.4-launch.jar TetradMissingValues.java 
jar cvfm TetradMissingValues.jar manifest.txt TetradMissingValues.class tetrad-gui-6.3.4-launch.jar 
cd ..
