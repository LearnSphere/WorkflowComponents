#!/usr/bin/env python
import os
import sys
import argparse
import requests
import json
import csv
from collections import defaultdict

parser = argparse.ArgumentParser(description='Count occurences of something in a tab-separated file')

parser.add_argument('-programDir', type=str,
           help='the component program directory')

parser.add_argument('-workingDir', type=str,
           help='the component instance working directory')

parser.add_argument("-node", nargs=1, action='append')
parser.add_argument("-fileIndex", nargs=2, action='append')

parser.add_argument('-userId', type=str,
           help='the user executing the component', default='')

parser.add_argument('-text')
parser.add_argument('-classificationColumnName')

args, option_file_index_args = parser.parse_known_args()

for x in range(len(args.node)):
    if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
        inFile0 = args.fileIndex[x][1]
    if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
        inFile1 = args.fileIndex[x][1]

		
textcolumnName = args.text
classificationColumnName = args.classificationColumnName

temp_in_csv_name = inFile0 + ".csv"
temp_out_csv_name = args.workingDir + "tmpoutput.csv"
out_tsv_name = args.workingDir + "output.txt"
lightside_model = inFile1

# Read the TSV file passed in and convert to CSV
intsv_f = open(inFile0, "r")
intsv = csv.DictReader(intsv_f, delimiter="\t")
temp_in_f = open(temp_in_csv_name, "w")
temp_in_csv = csv.writer(temp_in_f)
cols = intsv.fieldnames
temp_in_csv.writerow(["text" if c == textcolumnName else c for c in cols])
for row in intsv:
    temp_in_csv.writerow([row[h] for h in cols])
temp_in_f.close()
intsv_f.close()

# Execute lightside with temp_in_csv_name and lightside_model and utf-8
cmd = "bash program/predict.sh " + lightside_model + " utf-8 " + temp_in_csv_name + " " + temp_out_csv_name
os.chdir(args.programDir)
os.system(cmd)

# Read the CSV file passed out from Lightside and convert to TSV
tmpout_f = open(temp_out_csv_name, "r")
outcsv = csv.DictReader(tmpout_f)
cols = outcsv.fieldnames

outf = open(out_tsv_name, "w")
outtsv = csv.writer(outf, delimiter="\t")
outtsv.writerow([classificationColumnName if c == "predicted" else textcolumnName if c == "text" else c for c in cols])
for row in outcsv:
    outtsv.writerow([row[h] for h in cols])
tmpout_f.close()
outf.close()

