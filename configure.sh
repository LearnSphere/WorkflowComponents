#!/bin/bash

dir=/cygdrive/c/dev/WorkflowComponentsSvn/

cd $dir

for cdir in `find $dir -maxdepth 1  -type d -name "[^.]*"`; do
  cd $cdir

  if [ -f build.properties.sample ]; then
    cp build.properties.sample build.properties
  fi

  cd $dir
done

