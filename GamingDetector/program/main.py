"""
Script to label 2011-12 Bernacki data for gaming using Paquette et al, 2014

"""
import sys
import os
import logging
from logging.handlers import SysLogHandler
from logging import StreamHandler
from logging import FileHandler
import hashlib
from random import randint
import random
from datetime import datetime as dt
import pandas as pd
import numpy as np

# Calculate changes in answers
from Levenshtein import distance

from cmd_parser import *

if __name__ == '__main__':

    # Parse argumennts
    parser = get_default_arg_parser("Initialize a new problem")
    parser.add_argument("-node", nargs=1, action="append")
    parser.add_argument("-fileIndex", nargs=2, action="append")
    parser.add_argument("-transaction_id", type=str, help="")
    parser.add_argument("-transaction_id_nodeIndex", nargs=1, action="append")
    parser.add_argument("-transaction_id_fileIndex", nargs=1, action="append")
    parser.add_argument("-student_id", type=str, help="")
    parser.add_argument("-student_id_nodeIndex", nargs=1, action="append")
    parser.add_argument("-student_id_fileIndex", nargs=1, action="append")
    parser.add_argument("-session_id", type=str, help="")
    parser.add_argument("-session_id_nodeIndex", nargs=1, action="append")
    parser.add_argument("-session_id_fileIndex", nargs=1, action="append")
    parser.add_argument("-outcome_column", type=str, help="")
    parser.add_argument("-outcome_column_nodeIndex", nargs=1, action="append")
    parser.add_argument("-outcome_column_fileIndex", nargs=1, action="append")
    parser.add_argument("-duration_column", type=str, help="")
    parser.add_argument("-duration_column_nodeIndex", nargs=1, action="append")
    parser.add_argument("-duration_column_fileIndex", nargs=1, action="append")
    parser.add_argument("-input_column", type=str, help="")
    parser.add_argument("-input_column_nodeIndex", nargs=1, action="append")
    parser.add_argument("-input_column_fileIndex", nargs=1, action="append")
    parser.add_argument("-problem_column", type=str, help="")
    parser.add_argument("-problem_column_nodeIndex", nargs=1, action="append")
    parser.add_argument("-problem_column_fileIndex", nargs=1, action="append")
    parser.add_argument("-step_column", type=str, help="")
    parser.add_argument("-step_column_nodeIndex", nargs=1, action="append")
    parser.add_argument("-step_column_fileIndex", nargs=1, action="append")
    parser.add_argument("-correct_labels", nargs=1, action="append")
    parser.add_argument("-incorrect_labels", nargs=1, action="append")
    parser.add_argument("-hint_labels", nargs=1, action="append")
    parser.add_argument("-bug_labels", nargs=1, action="append")
    args = parser.parse_args()

    # Configure logging 
    logger = logging.getLogger("main")
    log_level = logging.DEBUG
    logger.setLevel(log_level)
    # Set log msg format
    formatter = logging.Formatter('%(levelname)s\t%(name)s\t%(asctime)s\t: %(message)s')

    # Write log msgs to *.wfl file for user debugging
    log_id = dt.now().isoformat()
    log_file = path.join(args.workingDir, 'log-%s.wfl' % log_id)
    ch = FileHandler(filename=log_file, encoding="UTF-16")
    ch.setLevel(log_level)
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    # Create stream handler to output error messages to stderr
    ch = StreamHandler(sys.stderr)
    ch.setLevel(logging.ERROR)
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    logger.info("Imported all packages")

    tx_file_indx = 0
    tx_files = get_input_files(args, tx_file_indx)

    # Import student tx data
    tx_path = tx_files[0]
    logger.debug("tx file: %s" % tx_path)
    tx = pd.read_csv(tx_path, delimiter='\t', header=0, low_memory=False)
    logger.info("Imported student transactions: %s" % str(tx.shape))

    ########################## Dataset Specific config ##########################

    # Column labels for this dataset
    outcome_col = 'Outcome'
    duration_col = "Duration (sec)"
    input_col = "Input"
    prob_col = "Problem Name"
    step_col = "Step Name"

    # Identifier Columns
    ses_id = 'Session Id'
    stu_id = "Anon Student Id"
    tx_id = "Transaction Id"

    # Checking Outcome column values. 
    # Outcome labels
    is_bug = ["BUG"]
    is_correct = ["OK", "OK_AMBIGUOUS"]
    is_error = ["ERROR", "BUG"]
    is_hint = ["INITIAL_HINT", "HINT_LEVEL_CHANGE"]

    ##################################################################

    ########################## Dataset Specific config ##########################

    # Column labels for this dataset
    outcome_col = args.outcome_column
    duration_col = args.duration_column
    input_col = args.input_column
    prob_col = args.problem_column
    step_col = args.step_column

    # Identifier Columns
    ses_id = args.session_id
    stu_id = args.student_id
    tx_id = args.transaction_id

    # Checking Outcome column values. 
    # Outcome labels
    logger.info("Outcome column labels in data: %s" % str(tx[outcome_col].unique()))
    is_bug = [elm.strip() for elm in args.bug_labels[0][0].split(",")]
    logger.debug("Bug labels: %s" % str(is_bug))
    is_correct = [elm.strip() for elm in args.correct_labels[0][0].split(",")]
    logger.debug("Correct labels: %s" % str(is_correct))
    is_error = [elm.strip() for elm in args.incorrect_labels[0][0].split(",")]
    logger.debug("Incorrect labels: %s" % str(is_error))
    is_hint = [elm.strip() for elm in args.hint_labels[0][0].split(",")]
    logger.debug("Hint labels: %s" % str(is_hint))

    ##################################################################

    game_col = "Is gaming"

    # Convert tx datetime columns from str to datetime
    datetime_cols = []
    date_cols = []
    time_cols = []
    for col in datetime_cols:
        if not pd.api.types.is_datetime64_any_dtype(tx[col]):
            logger.info("Converting '%s' column to datetime" % col)
            tx[col] = pd.to_datetime(tx[col]) 
        else:
            logger.info("'%s' column is already datetime type" % col)
    for col in date_cols:
        if not pd.api.types.is_datetime64_any_dtype(tx[col]):
            logger.info("Converting '%s' column to date" % col)
            tx[col] = pd.to_datetime(tx[col]).dt.date 
        else:
            logger.info("'%s' column is already datetime type" % col)
    for col in time_cols:
        if not pd.api.types.is_datetime64_any_dtype(tx[col]):
            logger.info("Converting '%s' column to time" % col)
            tx[col] = pd.to_datetime(tx[col]).dt.time 
        else:
            logger.info("'%s' column is already datetime type" % col)

    # Coerce numeric columns to numeric types
    num_cols = [duration_col]
    for col in num_cols:
        if not pd.api.types.is_numeric_dtype(tx[col]):
            logger.info("Converting '%s' column to numeric type" % col)
            tx[col] = pd.to_numeric(tx[col], errors='coerce')
        else:
            logger.debug("'%s' column is already a numeric type" % col)


    # Checking duration column is in seconds
    t = np.median(tx[duration_col].dropna())
    if t < 100 and t > 1:
        logger.info("Duration of TX seems to be in seconds. median = %f" % t)
    else:
        logger.info("Duration column not in seconds. median = %f" % t)
        ### TODO: Need to convert column to seconds
        
    #####  Previous tx column labels #####
    prev_outcome_col = "Previous outcome"
    prev_duration_col = "Previous duration"
    prev_input_col = "Previous input"
    prev_prob_col = "Previous problem name"
    prev_step_col = "Previous step name"

    ##### Adding Gaming Features Semantic Labels of TX #####
    # [Did not think before help] - Pause <= 5 seconds before help req
    # [thought before help] - Pause >= 6 seconds before help req
    # [read help messages] - Pause >= 9 seconds per help msg after a help request
    # [scanning help messages] - Pause between 4-8 seconds per help msg after help req.
    # [searching for bottom-out hint] - Pause < 4 secs per help msg after help req.
    # [thought before attempt] - Pause >= 6 before step attempt
    # [planned ahead] - Last action was a correct step with pause >= 11 sec
    # [guess] - Pause < 6 sec before step attempt
    # [unsuccessful but sincere attempt] - Pause >= 6 sec before step attempt
    # [guessing with values from problem] - Pause <= 6 before bug
    # [Read error msg] - Pause >= 9 after a bug
    # [Did not read error msg] - Pause < 9 after a bug
    # [thought about error] - Pause >= 6 after incorrect attempt
    # [same answer/diff context] - Answer was the same as the previous action but in a diff context
    # [similar answer] - Answer was similar to the prev action (Levenshtein distance of 1 or 2)
    # [Switched context before right] - Context of current actions is not the same as the 
    #    context for previous(incorrect) action (aka soft underbelly Baker, Mitrovic & Matthews, 2010)
    # [Same context] - Context of the current action is the same as the prev action
    # [repeated step] - Answer and context are the same as the previous action
    # [diff answer and/or diff context] - Answer or context is not the same as the prev action
    no_think_help_col = "Did not think before help"
    think_help_col = "Thought before help"
    read_help_col = "Read help messages"
    scan_help_col = "Scanning help messages"
    find_bottom_help_col = "Searching for bottom out hint"
    think_try_col = "Thought before attempt"
    plan_ahead_col = "Planned ahead"
    guess_col = "Guessed"
    no_success_try_col = "Unsuccessful but sincere attempt"
    guess_bug_col = "Guess with values from problem"
    read_error_col = "Read error message"
    no_read_error_col = "Did not read error message"
    thought_error_col = "Thought about error message"
    same_ans_diff_context_col = "Same answer but different context"
    similar_ans_col = "Answer is similar to last answer"
    switch_context_right_col = "Switched contexts before answering right"
    same_context_col = "Same context as last action"
    repeat_step_col = "Answer and context same as last action"
    diff_ans_diff_context_col = "Answer or context are not the same"

    # Levenstein distance threshold for similarity
    dist_thres = 2

    # Function for help working with rolling dataframes
    def get_rolling_df(sample_df, source_df):
        return(source_df.loc[sample_df.index, :])

    # Generate duplicate of tx for operations
    data = tx.copy()

    for col in data.columns:
        logger.debug("Data has column: %s" % col)

    # Add columns for previous tx
    data[prev_input_col] = data.groupby(ses_id)[input_col].shift(1)
    data[prev_prob_col] = data.groupby(ses_id)[prob_col].shift(1)
    data[prev_step_col] = data.groupby(ses_id)[step_col].shift(1)
    data[prev_duration_col] = data.groupby(ses_id)[duration_col].shift(1)
    data[prev_outcome_col] = data.groupby(ses_id)[outcome_col].shift(1)


    # [Did not think before help] - Pause <= 5 seconds before help req
    col = no_think_help_col
    thres = 5
    data[col] = (
                data[outcome_col].apply(lambda x: any([x == label for label in is_hint])) &
                (data[duration_col] <= thres )
              )
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [thought before help] - Pause >= 6 seconds before help req
    col = think_help_col
    thres = 5
    data[col] = (data[outcome_col].apply(lambda x: any([x == label for label in is_hint])) & 
                (data[duration_col] > thres )
              )
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [read help messages] - Pause >= 9 seconds per help msg after a help request
    col = read_help_col
    thres = 9

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: any([x == label for label in is_hint])) &
                 (data[duration_col] >= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [scanning help messages] - Pause between 4-8 seconds per help msg after help req.
    col = scan_help_col
    up_thres = 9
    lower_thres = 4

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: any([x == label for label in is_hint])) &
                 ((data[duration_col] >= lower_thres) &
                 (data[duration_col] < up_thres))
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [searching for bottom-out hint] - Pause < 4 secs per help msg after help req.
    col = find_bottom_help_col
    thres = 4

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: any([x == label for label in is_hint])) &
                 (data[duration_col] < thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [thought before attempt] - Pause >= 6 before step attempt
    col = think_try_col
    thres = 6

    # Generate labels
    data[col] = (
                data[outcome_col].apply(lambda x: 
                                      any([x == label for label in is_correct + is_error])) &
                 (data[duration_col] >= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [planned ahead] - Last action was a correct step with pause >= 11 sec
    col = plan_ahead_col
    thres = 11

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: any([x == label for label in is_correct])) &
                 (data[prev_duration_col] >= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [guess] - Pause < 6 sec before step attempt
    col = guess_col
    thres = 6

    # Generate labels
    data[col] = (
                data[outcome_col].apply(lambda x: 
                                       any([x == label for label in is_correct + is_error])) &
                 (data[duration_col] < thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [unsuccessful but sincere attempt] - Pause >= 6 sec before step attempt
    col = no_success_try_col
    thres = 6

    # Generate labels
    data[col] = (
                data[outcome_col].apply(lambda x: 
                                       any([x == label for label in is_correct + is_error])) &
                 (data[duration_col] >= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [guessing with values from problem] - Pause <= 6 before bug
    col = guess_bug_col
    thres = 6

    # Generate labels
    data[col] = (
                data[outcome_col].apply(lambda x: 
                                       any([x == label for label in is_bug])) &
                 (data[duration_col] <= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [Read error msg] - Pause >= 9 after a bug
    col = read_error_col
    thres = 9

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: 
                                       any([x == label for label in is_bug])) &
                 (data[duration_col] >= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [Did not read error msg] - Pause < 9 after a bug
    col = no_read_error_col
    thres = 9

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: 
                                       any([x == label for label in is_bug])) &
                 (data[duration_col] < thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [thought about error] - Pause >= 6 after incorrect attempt
    col = thought_error_col
    thres = 6

    # Generate labels
    data[col] = (
                data[prev_outcome_col].apply(lambda x: 
                                       any([x == label for label in is_error])) &
                 (data[duration_col] >= thres)
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [same answer/diff context] - Answer was the same as the previous action but in a diff context
    col = same_ans_diff_context_col

    # Generate labels
    data[col] = (
                (data[prev_input_col] == data[input_col]) &
                ((data[prev_prob_col] != data[prob_col]) |
                    (data[prev_step_col] != data[step_col]))
                )

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [similar answer] - Answer was similar to the prev action (Levenshtein distance of 1 or 2)
    col = similar_ans_col
    edit_col = "edit distance"

    # Generate labels
    data[edit_col] = data.apply(lambda x: 
                                distance(str(x[input_col]), str(x[prev_input_col])), axis=1)

    data[col] = (data[edit_col] <= dist_thres)

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))


    # [Switched context before right] - Context of current actions is not the same as the 
    #    context for previous(incorrect) action                                                 (aka soft underbelly Baker, Mitrovic & Matthews, 2010)
    col = switch_context_right_col

    # Generate labels
    data[col] = (data[prev_outcome_col].apply(lambda x: 
                                       any([x == label for label in is_error])) &
                 ((data[prev_prob_col] != data[prob_col]) |
                (data[prev_step_col] != data[step_col])))

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [Same context] - Context of the current action is the same as the prev action
    col = same_context_col

    # Generate labels
    data[col] = (((data[prev_prob_col] == data[prob_col]) |
                (data[prev_step_col] == data[step_col])))

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [repeated step] - Answer and context are the same as the previous action
    col = repeat_step_col

    # Generate labels
    data[col] = (((data[prev_prob_col] == data[prob_col]) |
                (data[prev_step_col] == data[step_col])) &
                (data[prev_input_col] == data[input_col]))

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # [diff answer and/or diff context] - Answer or context is not the same as the prev action
    col = diff_ans_diff_context_col

    # Generate labels
    data[col] = (((data[prev_prob_col] != data[prob_col]) |
                (data[prev_step_col] != data[step_col])) &
                (data[prev_input_col] != data[input_col]))

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    ##### Gaming Patterns #####
    # P1 - incorrect → [guess] & [same answer/diff. context] & incorrect
    # P2 - incorrect → [similar answer] [same context] & incorrect → [similar answer] & [same context] & attempt
    # P3 - incorrect → [similar answer] & incorrect → [same answer/diff. context] & attempt
    # P4 - [guess] & incorrect → [guess] & [diff. answer AND/OR diff. context] & incorrect → [guess] & [diff. answer AND/OR diff. context & attempt
    # P5 - incorrect → [similar answer] & incorrect → [guess] & attempt
    # P6 - help & [searching for bottom-out hint] → incorrect → [similar answer] & incorrect
    # P7 - incorrect → [same answer/diff. context] & incorrect → [switched context before correct] & attempt/help
    # P8 - bug → [same answer/diff. context] & correct → bug
    # P9 - incorrect → [similar answer] & incorrect → [switched context before correct] & incorrect
    # P10 - incorrect → [switched context before correct] & incorrect → [similar answer] & incorrect
    # P11 - incorrect → [similar answer] & incorrect → [did not think before help] & help → incorrect (with first or second answer similar to the last one)
    # P12 - help → incorrect → incorrect → incorrect (with at least one similar answer between steps)
    # P13 - incorrect → incorrect → incorrect → [did not think before help request] & help (at least one similar answer between steps)
    def get_game_pcol(num):
        return "gaming pattern " + str(num)

    # Create columns for each of the features for the previous step
    feature_cols = [no_think_help_col, think_help_col, read_help_col,
                    scan_help_col, find_bottom_help_col, think_try_col,
                    plan_ahead_col, guess_col, no_success_try_col,
                    guess_bug_col, read_error_col, no_read_error_col,
                    thought_error_col, same_ans_diff_context_col, similar_ans_col,
                    switch_context_right_col, same_context_col, repeat_step_col,
                    diff_ans_diff_context_col
                   ]

    def get_p1_col(label):
        return "Previous step " + label
    def get_p2_col(label):
        return "Two step prior " + label
    def get_p3_col(label):
        return "Three step prior " + label
    def get_p12_col(label):
        return "Prev step vs two step prior " + label
    def get_p13_col(label):
        return "Prev step vs three step prior " + label
    def get_p23_col(label):
        return "Two step prior vs three step prior " + label

    for col in feature_cols:
        pcol = get_p1_col(col)
        data[pcol] = data.groupby(ses_id)[col].shift(1)
        logger.info("Added column \"%s\"" % pcol)

    # Create columns for each of the features for the step 2 prior the current
    for col in feature_cols:
        pcol = get_p2_col(col)
        data[pcol] = data.groupby(ses_id)[col].shift(2)
        logger.info("Added column \"%s\"" % pcol)

    for col in [outcome_col, input_col]:
        pcol = get_p2_col(col)
        data[pcol] = data.groupby(ses_id)[col].shift(3)

    # Create columns for each of the features for the step 3 prior the current
    for col in feature_cols:
        pcol = get_p3_col(col)
        data[pcol] = data.groupby(ses_id)[col].shift(3)
        logger.info("Added column \"%s\"" % pcol)

    for col in [outcome_col, input_col]:
        pcol = get_p3_col(col)
        data[pcol] = data.groupby(ses_id)[col].shift(3)

    # Calculate levenstein distance for answers several steps prior
    # 2 steps prior
    col = get_p2_col(similar_ans_col)
    edit_col = get_p2_col("edit distance")

    # Generate labels
    data[edit_col] = data.apply(lambda x: 
                                distance(str(x[input_col]), str(x[get_p2_col(input_col)])), axis=1)

    data[col] = (data[edit_col] <= dist_thres)

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # 3 steps prior
    col = get_p3_col(similar_ans_col)
    edit_col = get_p3_col("edit distance")

    # Generate labels
    data[edit_col] = data.apply(lambda x: 
                                distance(str(x[input_col]), str(x[get_p3_col(input_col)])), axis=1)

    data[col] = (data[edit_col] <= dist_thres)

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # Compare prior step and 2 prior step
    col = get_p12_col(similar_ans_col)
    edit_col = get_p12_col("edit distance")

    # Generate labels
    data[edit_col] = data.apply(lambda x: 
                                distance(str(x[prev_input_col]), str(x[get_p2_col(input_col)])), axis=1)

    data[col] = (data[edit_col] <= dist_thres)

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # Compare previous step and 3 prior step
    col = get_p13_col(similar_ans_col)
    edit_col = get_p13_col("edit distance")

    # Generate labels
    data[edit_col] = data.apply(lambda x: 
                                distance(str(x[prev_input_col]), str(x[get_p3_col(input_col)])), axis=1)

    data[col] = (data[edit_col] <= dist_thres)

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # Compare 2 prior step and 3 prior step
    col = get_p23_col(similar_ans_col)
    edit_col = get_p23_col("edit distance")

    # Generate labels
    data[edit_col] = data.apply(lambda x: 
                                distance(str(x[get_p2_col(input_col)]), str(x[get_p3_col(input_col)])), axis=1)

    data[col] = (data[edit_col] <= dist_thres)

    # Save labels to tx data
    tx[col] = data[col]

    logger.info("Added \"%s\" column to tx for gaming features\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P1 - incorrect → [guess] & [same answer/diff. context] & incorrect
    col = get_game_pcol(1)
    # Generate label for pattern
    data[col] = (
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                 data[guess_col] & \
                 data[same_ans_diff_context_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P2 - incorrect → [similar answer] [same context] & incorrect → [similar answer] & [same context] & attempt
    col = get_game_pcol(2)
    # Generate label for pattern
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[get_p1_col(same_context_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error + is_correct])) & \
                 data[similar_ans_col] & \
                 data[same_context_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P3 - incorrect → [similar answer] & incorrect → [same answer/diff. context] & attempt
    col = get_game_pcol(3)
    # Generate label for pattern
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error + is_correct])) & \
                data[same_ans_diff_context_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P4 - [guess] & incorrect → [guess] & [diff. answer AND/OR diff. context] & incorrect → [guess] & [diff. answer AND/OR diff. context & attempt
    col = get_game_pcol(4)
    # Generate label for pattern
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p2_col(guess_col)] & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(guess_col)] & \
                data[get_p1_col(diff_ans_diff_context_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error + is_correct])) & \
                data[guess_col] & \
                data[diff_ans_diff_context_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))


    # P5 - incorrect → [similar answer] & incorrect → [guess] & attempt
    col = get_game_pcol(5)
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error + is_correct])) & \
                data[guess_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P6 - help & [searching for bottom-out hint] → incorrect → [similar answer] & incorrect
    col = get_game_pcol(6)
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_hint])) & \
                data[get_p2_col(find_bottom_help_col)] & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[similar_ans_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P7 - incorrect → [same answer/diff. context] & incorrect → [switched context before correct] & attempt/help
    col = get_game_pcol(7)
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(same_ans_diff_context_col)] & \
                data[switch_context_right_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P8 - bug → [same answer/diff. context] & correct → bug
    col = get_game_pcol(8)
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_bug])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_correct])) & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_bug])) & \
                data[get_p1_col(same_ans_diff_context_col)]
     
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P9 - incorrect → [similar answer] & incorrect → [switched context before correct] & incorrect
    col = get_game_pcol(9)
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[switch_context_right_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P10 - incorrect → [switched context before correct] & incorrect → [similar answer] & incorrect
    col = get_game_pcol(10)
    data[col] = (
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(switch_context_right_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[similar_ans_col]
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))


    # P11 - incorrect → [similar answer] & incorrect → [did not think before help] & help → incorrect (with first or second answer similar to the last one)
    col = get_game_pcol(11)
    data[col] = (
                data[get_p3_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_hint])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                (data[similar_ans_col] |
                     data[get_p2_col(similar_ans_col)] |
                     data[get_p3_col(similar_ans_col)])
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P12 - help → incorrect → incorrect → incorrect (with at least one similar answer between steps)
    col = get_game_pcol(12)
    data[col] = (
                data[get_p3_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_hint])) & \
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                (data[similar_ans_col] |
                     data[get_p2_col(similar_ans_col)] |
                     data[get_p12_col(similar_ans_col)]
                    )
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))

    # P13 - incorrect → incorrect → incorrect → [did not think before help request] & help (at least one similar answer between steps)
    col = get_game_pcol(13)
    data[col] = (
                data[get_p3_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p2_col(outcome_col)].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[prev_outcome_col].apply(lambda x:
                                             any([x == label for label in is_error])) & \
                data[get_p1_col(similar_ans_col)] & \
                data[outcome_col].apply(lambda x:
                                             any([x == label for label in is_hint])) & \
                data[no_think_help_col] & \
                (data[get_p12_col(similar_ans_col)] |
                     data[get_p13_col(similar_ans_col)] |
                     data[get_p23_col(similar_ans_col)]
                    )
                )

    # Save labels to tx data
    tx[col] = data[col]
    logger.info("Added \"%s\" column to tx for gaming patterns\nshape: %s\ncounts: %s" 
                % (col, str(tx.shape), str(data[col].value_counts())))


    pcols = [get_game_pcol(i) for i in range(1,14)]

    data[game_col] = data.apply(lambda x: any([x[col] for col in pcols]), axis=1)
    tx[game_col] = data[game_col]
    logger.info("Added \"%s\" column to tx data\nShape: %s\nCounts: %s" % (game_col, str(tx.shape), str(tx[game_col].value_counts())))

    # Label all tx part of identified patterns as gaming
    pattern_lengths = [2,3,3,3,3,3,3,3,3,3,4,4,4]
    debug = False
    for i, length in enumerate(pattern_lengths):
        pcol = i + 1
        d = tx[get_game_pcol(pcol)]
        for j in range(2,length+1):
            d = d | tx[get_game_pcol(pcol)].rolling(j, min_periods=j).apply(lambda x: x[-1], raw=True).shift(1-j)
        if debug:
            k = d[d].index[1]
            logger.debug(d.iloc[(k - 5):(k + 5)])
            logger.debug(tx[get_game_pcol(pcol)].iloc[(k - 5):(k + 5)])
        data[get_game_pcol(pcol)] = d
        logger.info("Original number of gaming tx in pattern %i: %i\n\
                    Expanded with pattern of length %i to: %i" % (pcol, np.sum(tx[get_game_pcol(pcol)]),
                                                                 length, np.sum(data[get_game_pcol(pcol)])))
    # Regenerate labels of all gaming tx    
    pcols = [get_game_pcol(i) for i in range(1,14)]
    data[game_col] = data.apply(lambda x: any([x[col] for col in pcols]), axis=1)
    logger.info("Original number of gaming tx in pattern %i: %i\n\
                Expanded with pattern of length %i to: %i" % (pcol, np.sum(tx[game_col]),
                                                              length, np.sum(data[game_col])))

    # Write data to file
    out_cols = [tx_id, game_col]
    out_cols.extend(pcols)
    out_path = args.workingDir
    out_file = "gaming-labels.tsv"
    out_file_path = os.path.join(out_path, out_file)
    #Ensure out directory exists
    if not os.path.exists(out_path):
        os.makedirs(out_path)
    tx.loc[:,out_cols].to_csv(out_file_path, sep='\t', index=False)
    logger.info("Wrote tx with gaming data to file: %s" % out_file_path)

