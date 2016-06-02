#!/bin/bash

dir=/cygdrive/c/dev/WorkflowComponentsTrunk/

cd $dir

rm -R */dist

for cdir in `ls -1`; do
  cd $cdir

  if [ -f build.xml ]; then
    ant dist
  fi

  cd $dir
done

rm -R */test/ComponentTestOutput WorkflowComponent.log */build



