cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" TetradClassifier.java
jar cvfm TetradClassifier.jar manifest.txt TetradClassifier.class tetrad-gui-6.3.4-launch.jar
cd ..
