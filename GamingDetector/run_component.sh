#!/bin/bash

# Use this script to test the component locally within this folder


# Setup local test folder if necessary
cwd=$(pwd)

# Get venv dir
cd venv
venv=$(pwd)
cd $cwd
source $venv/bin/activate

# Add all src subdirectories to python path (This emulates the flat heirarch that 
# will exist when this script is run in Tigris
path="$PYTHONPATH":"$cwd/program"

PYTHONPATH="$path" python program/main.py -programDir $cwd -workingDir $cwd/test/output -userId='testuser' -node 0 -fileIndex 0 $cwd/test/tx_test.csv >> run_component.log

deactivate
