#!/bin/bash
# Carnegie Mellon University, Human-Computer Interaction Institute.
# Copyright 2016. All Rights Reserved.
#
# Purpose: Creates the build.properties <<EOF for CMU RHEL servers.
#

source /datashop/tools/variables.sh

dir=${BASE}/workflow_components

cd ${dir}

cat > ImportDiscourseDB/build.properties <<EOF
component.interpreter.path=
component.program.path=program/run.sh
EOF

cat > RLMFitting/build.properties <<EOF
component.interpreter.path=/usr/local/bin/Rscript
component.program.path=program/RLMFitting.R
EOF

cat > AnalysisTkt/build.properties <<EOF
component.interpreter.path=/usr/local/bin/Rscript
component.program.path=program/TKT-model.R
EOF

cat > AnalysisPfa/build.properties <<EOF
component.interpreter.path=/usr/local/bin/Rscript
component.program.path=program/PFA-model.R
EOF

cat > GenerateTktFeatures/build.properties <<EOF
component.interpreter.path=/usr/local/bin/Rscript
component.program.path=program/TKT-features.R
EOF

cat > AnalysisBkt/build.properties <<EOF
executable.dir=/datashop/workflow_components/AnalysisBkt/program/
EOF

cat > GeneratePfaFeatures/build.properties <<EOF
component.interpreter.path=/usr/local/bin/Rscript
component.program.path=program/PFA-features.R
EOF

cat > AnalysisPyAfm/build.properties <<EOF
component.interpreter.path=/usr/local/bin/python3.5
component.program.path=program/afms_workflow_predict.py
EOF

cat > RTemplate/build.properties <<EOF
component.interpreter.path=/usr/local/bin/Rscript
component.program.path=program/DataShop-AFM.R
EOF

# Compile BKT's programs for this platform
cd AnalysisBkt/program/standard-bkt-public-standard-bkt
make
cp predicthmm.exe ../
cp trainhmm.exe ../
chmod ../predicthmm.exe ug+rx
chmod ../trainhmm.exe ug+rx

find -type f -name "*.sh" -exec dos2unix {} \;
find -type f -name "*.py" -exec dos2unix {} \;
find -type f -name "*.xsd" -exec dos2unix {} \;
find -type f -name "*.xml" -exec dos2unix {} \;


