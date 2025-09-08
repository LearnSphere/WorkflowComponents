#!/usr/bin/env python3
import inspect 
import os, pandas as pd
import sys
import argparse
import csv
from datetime import datetime as dt, timedelta

def get_cli_input():
    if '-workingDir' in sys.argv:
        p = argparse.ArgumentParser()
        p.add_argument('-workingDir', required=True)
        p.add_argument('-programDir')
        p.add_argument('-node',      nargs=1, action='append')
        p.add_argument('-fileIndex', nargs=2, action='append')
        p.add_argument('-userId', type=str, help='placeholder for WF', default='')
        a = p.parse_args()
        
        # If we have node and fileIndex arguments, use the original logic
        if a.node and a.fileIndex:
            for n, fi in zip(a.node, a.fileIndex):
                if n[0] == '0' and fi[0] == '0':
                    return fi[1], a.workingDir
            raise RuntimeError("Expected fileIndex 0/0 not found. Did you forget to connect the Join output?")
        else:
            # If no node/fileIndex, construct the path based on the component structure
            # The workingDir is typically the component test output directory
            # We need to go up and find the input file in the Import-1-x995490/output/ directory
            
            # Get the test directory (should be test/)
            work_dir_clean = a.workingDir.rstrip('/\\')
            # workingDir is like: test/ComponentTestOutput/output/
            # We need to go up to get to test/
            test_dir = work_dir_clean
            while test_dir and not test_dir.endswith('test'):
                test_dir = os.path.dirname(test_dir)
            
            if not test_dir:
                # Fallback: try to find test directory relative to current location
                current_dir = os.getcwd()
                test_dir = os.path.join(current_dir, 'test')
            
            # Try the Import directory as specified in the component XML
            alt_input_file = os.path.join(test_dir, "Import-1-x995490", "output", "joinedResult.txt")
            print(f"Trying input file path: {alt_input_file}")
            if os.path.exists(alt_input_file):
                return alt_input_file, a.workingDir
                
            # Try the direct test data directory
            data_input_file = os.path.join(test_dir, "data", "joinedResult.txt")
            print(f"Trying input file path: {data_input_file}")
            if os.path.exists(data_input_file):
                return data_input_file, a.workingDir
                
            # Try in the working directory itself
            input_file = os.path.join(a.workingDir, "joinedResult.txt")
            print(f"Trying input file path: {input_file}")
            if os.path.exists(input_file):
                return input_file, a.workingDir
                
            # Print debug information
            print(f"Working directory: {a.workingDir}")
            print(f"Test directory: {test_dir}")
            print(f"Current directory: {os.getcwd()}")
            
            # Default fallback
            return "joinedResult.txt", a.workingDir
    elif len(sys.argv) > 1:
        return sys.argv[1], '.'
    return 'joinedResult.txt', '.'

input_0, work_dir = get_cli_input()

# Output directory for LearnSphere
output_dir = work_dir 
output_0 = os.path.join(output_dir, "sessions_custom.txt")

# ───────────────────────────────────────────────────────────── #
# 1. Load and clean DataShop file
# ───────────────────────────────────────────────────────────── #
read_csv_args = dict(
    sep='\t',
    engine='python',
    quoting=csv.QUOTE_NONE
)

# pandas ≥ 1.3 supports on_bad_lines; older versions use error_bad_lines
if "on_bad_lines" in inspect.signature(pd.read_csv).parameters:
    read_csv_args["on_bad_lines"] = "skip"
else:
    read_csv_args["error_bad_lines"] = False  # skip bad rows quietly
    read_csv_args["warn_bad_lines"]  = False

df = pd.read_csv(input_0, **read_csv_args)
print("INPUT =", input_0)
print("SHAPE AFTER READ =", df.shape)
print("FIRST 5 COLS =", df.columns[:5].tolist())
print(df.head(2))            # peek at some data

df.columns = df.columns.str.strip()
df['Time'] = pd.to_datetime(df['Time'], errors='coerce')
df.dropna(subset=['Time'], inplace=True)
df.sort_values(['Anon Student Id', 'Time'], inplace=True)

# ───────────────────────────────────────────────────────────── #
# 2. Event categories and helpers
# ───────────────────────────────────────────────────────────── #
document_event = ['document-ready']
window_event = ['window-unload', 'window-blur', 'window-focus']
pe_event = [
    'jsav-matrix-click', 'jsav-exercise-grade', 'jsav-exercise-reset', 'jsav-node-click',
    'button-identifybutton', 'button-editbutton', 'button-addrowbutton', 'button-deletebutton',
    'button-setterminalbutton', 'button-addchildbutton', 'button-checkbutton', 'button-autobutton',
    'button-donebutton', 'submit-helpbutton', 'submit-edgeButton', 'submit-deleteButton',
    'submit-undoButton', 'submit-redoButton', 'submit-editButton', 'submit-nodeButton',
    'submit-begin', 'submit-finish', 'button-hintbutton', 'button-movebutton',
    'button-removetreenodebutton', 'button-savefile', 'button-edgebutton',
    'jsav-exercise-model-end', 'jsav-exercise-model-begin', 'jsav-array-click',
    'jsav-exercise-gradeable-step', 'jsav-exercise-model-open', 'jsav-exercise-model-forward',
    'jsav-exercise-model-close', 'jsav-exercise-grade-change', 'jsav-exercise-step-fixed',
    'jsav-arraytree-click', 'jsav-exercise-undo', 'jsav-exercise-model-backward',
    'jsav-exercise-step-undone', 'odsa-award-credit', 'odsa-exercise-init',
    'button-classify', 'button-throwRoll', 'button-calculate', 'button-decrement',
    'button-help', 'button-selecting', 'button-sorting', 'button-incrementing',
    'button-run', 'button-partition', 'button-markSorted', 'button-reset',
    'button-outputbuffer', 'button-noaction', 'button-submit', 'button-insert',
    'button-remove', 'button-next', 'button-about', 'button-undir', 'button-dir',
    'button-clear', 'button-read', 'button-write', 'button-restart'
]
ff_event = ['jsav-begin', 'jsav-end', 'jsav-forward', 'jsav-backward']
other_event = ['hyperlink', 'jsav-narration-on', 'jsav-narration-off', 'button-layoutRef', 'odsa-exercise-init']

def normalize_event(ev):
    raw = str(ev).strip().strip('"').strip("'")
    return raw.replace(' ', '-').lower()

document_event = [normalize_event(x) for x in document_event]
window_event   = [normalize_event(x) for x in window_event]
pe_event       = [normalize_event(x) for x in pe_event]
ff_event       = [normalize_event(x) for x in ff_event]
other_event    = [normalize_event(x) for x in other_event]

def writeEvName(row):
    ev = normalize_event(row['Step Name'])
    if ev in document_event: return "document event"
    if ev in window_event  : return "window event"
    if ev in ff_event      : return "FF event"
    if ev in pe_event      : return "PE event"
    if ev in other_event   : return "Other event"
    return "Other event"

def writeDesc(row):
    if row['CF (Exercise Type)'] == 'pe':
        return "Attempted to solve PE"
    if pd.isna(row['Level (Section)']):
        return row['Action']
    return f"Attempted to solve {row.get('CF (Short Name)', 'exercise') or 'exercise'}"

def check_pe(cmd): return normalize_event(cmd) in pe_event

def bundle_pe(curr, nxt):
    return (curr['CF (Exercise Type)'] == 'pe' or check_pe(curr['Step Name'])) and \
           (nxt['CF (Exercise Type)'] == 'pe' or check_pe(nxt['Step Name']))

def bundle_ff(curr, nxt):
    return curr['CF (Short Name)'] == nxt['CF (Short Name)'] if pd.notna(curr['CF (Short Name)']) else False

def writeTime(row, start, end):
    ev = normalize_event(row['Step Name'])
    sec = (end - start).total_seconds()
    if row['CF (Exercise Type)'] == 'pe' or check_pe(ev):
        return f"{sec} seconds"
    if ev in ff_event:
        return f"{sec} seconds"
    if "document" not in ev:
        kind = "slideshow" if row['CF (Short Name)'] else "exercise"
        return f"In {kind} for {sec} seconds" if sec > 0 else None
    return ""

# ───────────────────────────────────────────────────────────── #
# 3. Build sessions and grouped events
# ───────────────────────────────────────────────────────────── #
rows = []
cols = ["session", "Anon Student Id", "Level (Book)", "Event name", "Event Description",
        "Start time", "End Time", "Action Time", "CF (Short Name)", "Number of events"]

for uid, g in df.groupby('Anon Student Id'):
    g = g.reset_index(drop=True)
    sess = 1
    i = 0
    while i < len(g):
        start_i = i
        while i + 1 < len(g):
            t0 = g.at[i, 'Time']
            t1 = g.at[i + 1, 'Time']
            if (t1 - t0).total_seconds() > 600: break
            i += 1
        seg = g.iloc[start_i:i + 1]
        j = 0
        while j < len(seg):
            curr = seg.iloc[j]
            k = j
            while k + 1 < len(seg):
                next_row = seg.iloc[k + 1]
                curr_time = seg.iloc[k]['Time']
                next_time = next_row['Time']
                if (next_time - curr_time).total_seconds() > 60: break
                if (str(next_row['Step Name']).strip() == str(curr['Step Name']).strip() or
                    bundle_pe(curr, next_row) or
                    bundle_ff(curr, next_row)):
                    k += 1
                else:
                    break
            s_start = seg.iloc[j]['Time']
            s_end   = seg.iloc[k]['Time']
            rows.append([
                sess,
                uid,
                curr['Level (Book)'],
                writeEvName(curr),
                writeDesc(curr),
                s_start,
                s_end,
                writeTime(curr, s_start, s_end),
                curr['CF (Short Name)'],
                k - j + 1
            ])
            j = k + 1
        sess += 1
        i += 1

# ───────────────────────────────────────────────────────────── #
# 4. Output to .txt (tab-delimited)
# ───────────────────────────────────────────────────────────── #
session_df = pd.DataFrame(rows, columns=cols)
session_df.to_csv(output_0, sep='\t', index=False)

# ───────────────────────────────────────────────────────────── #
# 5. Write progress log for LearnSphere UI
# ───────────────────────────────────────────────────────────── #
with open(os.path.join(output_dir, "progress_log.wfl"), "w") as f:
    f.write("%Progress::@0@Finished")
print("INPUT =", input_0)                 # confirm path
print("DF SHAPE =", df.shape)             # rows / cols after read_csv
print("COLUMNS =", df.columns.tolist())   # make sure expected names exist
print("ROWS BEFORE SAVE =", len(rows)) 