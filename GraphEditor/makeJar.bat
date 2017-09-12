cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" GraphEditor.java
jar cvfm GraphEditor.jar manifest.txt GraphEditor.class tetrad-gui-6.3.4-launch.jar
cd ..
