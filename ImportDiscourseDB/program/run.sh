#!/bin/bash

echo "BASH ARGS: " $*

"/usr/java/jre1.8.0_92/bin/java" -cp "$2/program:$2/program/discoursedb/discoursedb-demo-courserapsych-0.6-SNAPSHOT.jar:$2/program/discoursedb/discoursedb-demo-courserapsych-0.6-SNAPSHOT-dist/*" edu.cmu.cs.lti.discoursedb.demo.courserapsych.ConstructivePostStats $6 $4/export.csv
