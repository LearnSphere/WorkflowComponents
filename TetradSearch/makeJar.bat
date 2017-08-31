cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" SearchAlgorithmWrapper.java
javac -cp ".;tetrad-gui-6.3.4-launch.jar;SearchAlgorithmWrapper.class" TetradSearch.java
jar cvfm TetradSearch.jar manifest.txt SearchAlgorithmWrapper.class TetradSearch.class tetrad-gui-6.3.4-launch.jar
cd ..
