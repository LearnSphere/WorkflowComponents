cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" TetradDiscretize.java
jar cvfm TetradDiscretize.jar manifest.txt TetradDiscretize.class tetrad-gui-6.3.4-launch.jar
cd ..
