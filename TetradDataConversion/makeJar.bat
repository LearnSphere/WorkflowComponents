cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" TetradDataConversion.java
jar cvfm TetradDataConversion.jar manifest.txt TetradDataConversion.class tetrad-gui-6.3.4-launch.jar
cd ..
