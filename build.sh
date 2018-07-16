#!/bin/bash



dir=`pwd`

if [ "$#" == "1" ]; then
  dir=$1
fi

cd $dir

rm -f ${dir}/build_errors.txt ${dir}/build_errors_info.txt
rm -Rf */dist

for cdir in `find $dir -maxdepth 1  -type d -name "[^.]*"`; do
  if [ "$cdir" == "$dir/Templates" ]
  then
    echo "Skipping Templates dir"
    continue
  fi
  cd $cdir

  if [ -f build.xml ] && [ "$cdir" != "$dir" ]; then
    buildOutput=`ant dist 2>> ${dir}/build_errors.txt`
    if [ "$?" != 0 ]; then
      echo ""
      echo "******************************************"
      echo "Error building jar in ${cdir}"
      echo "******************************************"
      echo ""
      echo ${buildOutput} >> build_errors_info.txt
    else
      echo "Success: ${cdir}"
    fi
  fi

  cd $dir
done

rm -Rf */test/ComponentTestOutput */WorkflowComponent.log */build

if [ ! -s ${dir}/build_errors.txt ]; then
   # the file is empty
   rm ${dir}/build_errors.txt ${dir}/build_errors_info.txt
   echo "No errors were found during the build process."
else
   echo "Errors were found during the build process. Please see build_errors.txt or build_errors_info.txt for additional details."
fi



