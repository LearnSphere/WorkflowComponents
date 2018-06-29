#!/bin/bash

#
# Script to run the Workflow Component Creator.
# Two optional args:
#    1. the directory that contains the Templates and CommonLibraries directories [default is cwd]
#    2. the name (with path) of the properties file which specifies info for new component [default is ./Templates/wcc.properties]
# Look at ./Templates/wcc.properties for how to configure the properties file.
#

dir=`pwd`

if [ "$1" == "-help" ]; then
    echo 'Usage: runWCC.sh [dir] [propFile]'
    exit 0
fi

if [ "$#" -gt 0 ]; then
    dir=$1
fi
echo Directory is: $dir

prop_file=$dir/Templates/wcc.properties.R
if [ "$#" -gt 1 ]; then
    prop_file=$2
fi
echo Using properties file: $prop_file

cd $dir

classpath=$dir/CommonLibraries/datashop.jar:$dir/CommonLibraries/commons-io-1.2.jar:$dir/CommonLibraries/commons-lang-2.2.jar:$dir/CommonLibraries/log4j-1.2.13.jar
templates_dir=$dir/Templates
classname=edu.cmu.pslc.datashop.extractors.workflows.WorkflowComponentCreator

java -classpath $templates_dir:$classpath $classname -file $prop_file -dir $dir
