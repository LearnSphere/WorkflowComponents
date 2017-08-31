cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" TetradRegression.java
jar cvfm TetradRegression.jar manifest.txt TetradRegression.class tetrad-gui-6.3.4-launch.jar
cd ..
