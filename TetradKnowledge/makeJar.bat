cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" TetradKnowledge.java
jar cvfm TetradKnowledge.jar manifest.txt TetradKnowledge.class tetrad-gui-6.3.4-launch.jar
cd ..
