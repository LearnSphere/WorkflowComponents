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

PYTHONPATH="$path" python program/main.py -programDir $cwd -workingDir $cwd/test/output -userId='testuser' -node 0 -fileIndex 0 $cwd/test/tx_test_pl2.csv -transaction_id "Transaction Id" -student_id "Anon Student Id" -session_id "Session Id" -outcome_column "Outcome" -duration_column "Duration (sec)" -input_column "Input" -problem_column="Problem Name" -step_column="Step Name" -correct_labels="CORRECT" -incorrect_labels="INCORRECT" -hint_labels="HINT" -bug_labels="" >> run_component.log

deactivate
