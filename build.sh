#!/bin/bash



dir=`pwd`

if [ "$#" == "1" ]; then
  dir=$1
fi

cd $dir

rm -Rf */dist

for cdir in `find $dir -maxdepth 1  -type d -name "[^.]*"`; do
  if [ "$cdir" == "$dir/Templates" ]
  then
    echo "Skipping Templates dir"
    continue
  fi
  cd $cdir

  if [ -f build.xml ]; then
    ant dist
  fi

  cd $dir
done

rm -Rf */test/ComponentTestOutput */WorkflowComponent.log */build



