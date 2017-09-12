cd program
javac -cp ".;tetrad-gui-6.3.4-launch.jar" RowOperations.java
jar cvfm RowOperations.jar manifest.txt RowOperations.class tetrad-gui-6.3.4-launch.jar
cd ..
