#!/bin/bash

dir=`pwd`

cd $dir

rm -R */dist

for cdir in `find $dir -maxdepth 1  -type d -name "[^.]*"`; do
  cd $cdir

  if [ -f build.xml ]; then
    ant dist
  fi

  cd $dir
done

rm -R */test/ComponentTestOutput WorkflowComponent.log */build



