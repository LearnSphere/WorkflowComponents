cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" TetradEstimator.java
jar cvfm TetradEstimator.jar manifest.txt TetradEstimator.class tetrad-gui-6.3.4-launch.jar
cd ..
