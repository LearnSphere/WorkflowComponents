FOR %%A IN (%*) DO (
     ECHO BATCH ARG: %%A
)

"C:/Program Files (x86)/Java/jre1.8.0_45/bin/java.exe" -cp "%2/program;%2/program/discoursedb/discoursedb-demo-courserapsych-0.6-SNAPSHOT.jar;%2/program/discoursedb/discoursedb-demo-courserapsych-0.6-SNAPSHOT-dist/*" edu.cmu.cs.lti.discoursedb.demo.courserapsych.ConstructivePostStats %6 %4/export.csv
