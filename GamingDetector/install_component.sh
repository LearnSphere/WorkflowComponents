#!/bin/bash

# This script should be run from the local directory

# Assumed dependencies:
# -- python pip
# -- python virtualenv
echo "Setting up Component for local system"

cwd="$(pwd)"
wcc_dir="$(dirname $cwd)"
venv="$cwd/venv"
if [ ! -d $venv ]; then
    virtualenv $venv --python=python3.6
    source $venv/bin/activate
    pip install --upgrade pip
    pip install -r requirements.txt
    
    # Deactiate python venv
    deactivate
fi

### Rebuild build.properties using local system path to venv python
source $venv/bin/activate
pypath=$(which python)
export IFS="="
f1="component.interpreter.path"
f2="component.program.path"
if [ build.properties ]; then
    rm build.properties
fi
while read -r k v; do
    [ "$k" == "component.interpreter.path" ] && echo "$f1=$pypath" >> build.properties
    [ "$k" == "component.program.path" ] && echo "$f2=$v" >> build.properties
done < build.properties.sample

# Deactiate python venv
deactivate
