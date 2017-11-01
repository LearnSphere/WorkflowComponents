#!/bin/bash

"/usr/java/jdk1.8.0_131/jre/bin/java" -Djava.util.prefs.systemRoot=/usr/java/jdk1.8.0_131/jre/ -Djava.util.prefs.userRoot=/usr/java/jdk1.8.0_131/jre/ -cp /datashop/workflow_components/CustomLibraries/tetrad-gui-6.3.4-launch.jar -jar /datashop/workflow_components/RowOperations/dist/RowOperations.jar $@
