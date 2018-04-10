#!/usr/bin/env python
import os
import sys
import argparse
import requests
import json
import csv
from collections import defaultdict
import datetime

parser = argparse.ArgumentParser(description='Count occurences of something in a tab-separated file')

logger = open("/datashop/workflow_components/TransformClassifyLightside/program/pylog.txt","a")
logger.write("-----Starting python wrapper at "+ str(datetime.datetime.now()) + "\n")
parser.add_argument('-programDir', default = ".")
parser.add_argument('-workingDir', default = ".")
parser.add_argument('-userId', default = "")
parser.add_argument('-text')
parser.add_argument('-classificationColumnName')
parser.add_argument('-file0')
parser.add_argument('-file1')

args = parser.parse_args()
textcolumnName = args.text
classificationColumnName = args.classificationColumnName

temp_in_csv_name = args.file0+ ".csv"
temp_out_csv_name = args.workingDir + "tmpoutput.csv"
out_tsv_name = args.workingDir + "output.txt"
lightside_model = args.file1

# Read the TSV file passed in and convert to CSV
intsv = csv.DictReader(open(args.file0, "r"), delimiter="\t")
temp_in_f = open(temp_in_csv_name, "w")
temp_in_csv = csv.writer(temp_in_f)
cols = intsv.fieldnames
temp_in_csv.writerow(["text" if c == textcolumnName else c for c in cols])
for row in intsv:
    temp_in_csv.writerow([row[h] for h in cols])
temp_in_f.close()

# Execute lightside with temp_in_csv_name and lightside_model and utf-8
cmd = "bash program/predict.sh " + lightside_model + " utf-8 " + temp_in_csv_name + " " + temp_out_csv_name

logger.write("Executing " + cmd + "   @ " + args.programDir + "\n")
os.chdir(args.programDir)
os.system(cmd)
logger.write("Executed!" + "\n")

# Read the CSV file passed out from Lightside and convert to TSV
outcsv = csv.DictReader(open(temp_out_csv_name, "r"))
cols = outcsv.fieldnames

outf = open(out_tsv_name, "w")
outtsv = csv.writer(outf, delimiter="\t")
outtsv.writerow([classificationColumnName if c == "predicted" else textcolumnName if c == "text" else c for c in cols])
for row in outcsv:
    outtsv.writerow([row[h] for h in cols])
logger.write("Wrote output to " + out_tsv_name + "\n")
outf.close()

