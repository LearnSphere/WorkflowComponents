#!/bin/bash

# This script should be run from the local directory

# Assumed dependencies:
# -- python pip
# -- python virtualenv
echo "Setting up D3MDatasetSelector Tigris Components for local system"

virtualenv env --python=python2.7
source env/bin/activate
pip install --upgrade pip
pip install -r requirements.txt

### Rebuild build.properties using local system path to venv python
pypath=$(which python)
export IFS="="
f1="component.interpreter.path"
f2="component.program.path"
if [ -f build.properties ]; then
    rm build.properties
fi
while read -r k v; do
    [ "$k" == "component.interpreter.path" ] && echo "$f1=$pypath" >> build.properties
    [ "$k" == "component.program.path" ] && echo "$f2=$v" >> build.properties
done < build.properties.sample

# Deactiate python venv
deactivate
