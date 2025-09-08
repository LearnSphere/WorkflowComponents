#!/usr/bin/env python3

import os, sys, pathlib, re, pandas as pd

ID_COL = "Anon Student Id"  # Changed from "user ID"

def get_cli_input():
    if '-workingDir' in sys.argv:
        import argparse
        p = argparse.ArgumentParser()
        p.add_argument('-workingDir', required=True)
        p.add_argument('-programDir')
        p.add_argument('-node', nargs=1, action='append')
        p.add_argument('-fileIndex', nargs=2, action='append')
        p.add_argument('-userId', type=str, help='placeholder for WF', default='')
        a = p.parse_args()

        if a.node and a.fileIndex:
            for n, fi in zip(a.node, a.fileIndex):
                if n[0] == '0' and fi[0] == '0':
                    return fi[1], a.workingDir

        work_dir = a.workingDir.rstrip('/\\')
        test_dir = work_dir
        while test_dir and not test_dir.endswith('test'):
            test_dir = os.path.dirname(test_dir)

        fallback_paths = [
            os.path.join(test_dir, "Import-1-x995490", "output", "sessions_custom.txt"),
            os.path.join(test_dir, "data", "sessions_custom.txt"),
            os.path.join(a.workingDir, "sessions_custom.txt")
        ]
        for path in fallback_paths:
            if os.path.exists(path):
                return path, a.workingDir

        return "sessions_custom.txt", a.workingDir
    elif len(sys.argv) > 1:
        return sys.argv[1], '.'
    return 'sessions_custom.txt', '.'

def determine_state(row):
    """Assigns state based on event type and description."""
    name = row['Event name']
    desc = str(row['Event Description'])
    if name == 'FF event':
        return 'Visualization'
    elif name in ['window event', 'document event']:
        return 'Reading'
    elif name == 'PE event':
        return 'Proficiency_Exercise'
    elif name == 'Other event':
        return 'Multiple_choice_Exercise'
    return None

def extract_time(txt):
    """Extracts numeric values from Action Time, excluding 'Away time'."""
    if pd.isna(txt) or not isinstance(txt, str) or "Away time:" in txt:
        return 0.0
    return sum(float(x) for x in re.findall(r"\d+\.\d+|\d+", txt))

def build_transitions(df):
    """Computes state transitions and time spent per student."""
    states = ['Reading', 'Visualization', 'Proficiency_Exercise', 'Multiple_choice_Exercise']
    transitions = [f'{a}-{b}' for a in states for b in states if a != b]

    output = {}
    for sid, group in df.groupby(ID_COL):
        trans_count = {t: 0 for t in transitions}
        time_spent = {f'{s}_Time': 0.0 for s in states}
        prev = None

        for _, row in group.iterrows():
            curr = row['State']
            if curr:
                time_spent[f'{curr}_Time'] += row['Time_Spent']
                if prev and prev != curr:
                    key = f'{prev}-{curr}'
                    if key in trans_count:
                        trans_count[key] += 1
                prev = curr
            elif row['Time_Spent'] > 0:
                time_spent['Reading_Time'] += row['Time_Spent']

        output[sid] = {**trans_count, **time_spent}

    df_out = pd.DataFrame.from_dict(output, orient='index').reset_index()
    df_out.rename(columns={'index': ID_COL}, inplace=True)
    return df_out

def main():
    input_file, work_dir = get_cli_input()
    out_dir = pathlib.Path(work_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    print("Reading input from:", input_file)

    df = pd.read_csv(input_file, sep='\t' if input_file.endswith('.txt') else ',', encoding='utf-8', low_memory=False)
    df['State'] = df.apply(determine_state, axis=1)
    df['Time_Spent'] = df['Action Time'].apply(extract_time)

    result_df = build_transitions(df)
    output_path = out_dir / "engagement_metrics.txt"
    result_df.to_csv(output_path, sep='\t', index=False)

    print(f"Output written to: {output_path}")
    print("%Progress::@100@Finished")

if __name__ == "__main__":
    main()
