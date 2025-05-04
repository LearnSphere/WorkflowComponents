import pip
import sys
import datetime
import subprocess

def install_packages():
    try:
        import pandas
        import openpyxl
    except ImportError:
        print("Installing pandas and openpyxl...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "pandas", "openpyxl"])
        print("Installation complete.")

install_packages()

import argparse
import pandas as pd
import os

TEST_MODE=False

# Helper function to get module details
def get_module_details(email, scores, progress, modules, modules_raw):
  all_module_details = []
  for module, module_raw in zip(modules, modules_raw):
    module_details = {}
    
    progress_values = progress[(progress['module_title'] == module_raw) & (progress['email'] == email)]['percent_progress'].tolist()
    module_details['progress'] = progress_values[0] if progress_values else None

    pre_test_values = scores[(scores['page'].str.contains("Pre-Test - " + module, na=False)) & (scores['email'] == email)]['percent_score'].tolist()
    module_details['pre-test'] = pre_test_values[0] if pre_test_values else None

    post_test_values = scores[(scores['page'].str.contains("Post-Test - " + module, na=False)) & (scores['email'] == email)]['percent_score'].tolist()
    module_details['post-test'] = post_test_values[0] if post_test_values else None
    module_details['module'] = module

    # if there are duplicate records in progress, then progress_values will have
    # 2 records. The record of interest will be the one that isn't zero,
    # given that scores are non-zero
    if (module_details.get('pre-test') is not None and module_details['pre-test'] > 0) or \
   (module_details.get('post-test') is not None and module_details['post-test'] > 0):
       if len(progress_values) > 1:
          if progress_values[0] > 0:
             module_details['progress'] = progress_values[0]
          else:
             module_details['progress'] = progress_values[1]


    all_module_details.append(module_details)
  return all_module_details


# Helper function to save output
def save_output(student_details, working_dir):
  # Convert student_details dictionary to a list of dictionaries
  data = []
  modules = set()

  for student in student_details:
      row = {
          ('', 'Section Name'): student['section'],
          ('', 'Student Name'): student['student'],
          ('', 'Student Email'): student['email']
      }
      for module in student['module_details']:
          row[(module['module'], 'Progress')] = module['progress']
          row[(module['module'], 'Pre-Test Score')] = module['pre-test']
          row[(module['module'], 'Post-Test Score')] = module['post-test']
          modules.add(module['module'])

      data.append(row)

  # Create a DataFrame
  df = pd.DataFrame(data)

  # Create MultiIndex columns
  df.columns = pd.MultiIndex.from_tuples(df.columns)

  # Save the output in CSV format
  df.to_csv(os.path.join(working_dir, 'output.csv'), index=False)


"""
Main Program

"""
def main_program(scores, progress, working_dir):

    scores = pd.read_csv(scores, nrows=200 if TEST_MODE else None)
    progress = pd.read_csv(progress, nrows=50 if TEST_MODE else None)

    # Transform score so we get a new field, percent_score
    scores['percent_score'] = round((scores['score'] / scores['out_of']) * 100,2)
    progress['percent_progress'] = round(progress['progress'] * 100)

    # for this part, I want to see all unique pages in scores
    unique_pages = scores['page'].unique()

    # Get unique module titles
    unique_modules = progress['module_title'].unique()

    unique_modules_raw = unique_modules

    # Remove the anything surrounded in brackets from module
    unique_modules = [module.split(' (')[0] for module in unique_modules]

    # Select relevant columns beforehand
    output_scores = scores[['section', 'student', 'email']].drop_duplicates()

    # Optimize filtering of scores (vectorized approach)
    filtered_scores = scores[scores['page'].str.contains('|'.join(unique_modules), na=False, regex=True)]

    # Get unique students & merge student details efficiently
    unique_students_df = output_scores[output_scores['email'].isin(filtered_scores['email'])]

    # Fetch module details for all students at once
    student_details = unique_students_df.copy()
    student_details['module_details'] = student_details['email'].map(
        lambda email: get_module_details(email, scores, progress, unique_modules, unique_modules_raw)
    )

    # Convert to list (if needed)
    student_details = student_details.to_dict(orient='records')

    # Convert student details to Excel
    save_output(student_details, working_dir)


def progressMessage(progressLogFilePath, message):
    f = open(progressLogFilePath, "a");
    now = datetime.datetime.now()
    progressPrepend = "%Progress::"
    f.write(progressPrepend + "@" + str(now) + "@" + message);
    f.close();


def merge_csv_to_excel(scores_file, progress_file, working_dir):
    # Read the CSV files into DataFrames
    scores_df = pd.read_csv(scores_file)
    progress_df = pd.read_csv(progress_file)
    
    # Merge the DataFrames on a common column (assuming 'ID' is the key)
    merged_df = pd.merge(scores_df, progress_df, on='email', how='outer')
    
    # Write the merged DataFrame to an Excel file
    merged_df.to_excel(os.path.join(working_dir, 'output.xlsx'), index=False)
    

    output_file = os.path.join(working_dir, 'output.xlsx')
    # print(f"Merged data has been written to {output_file}")

if __name__ == "__main__":

    inFile0 = None
    inFile1 = None

    parser = argparse.ArgumentParser(description='Process datashop file.')

    parser.add_argument('-programDir', type=str,
            help='the component program directory')

    parser.add_argument('-workingDir', type=str,
            help='the component instance working directory')

    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')

    parser.add_argument('-userId', type=str,
            help='the user executing the component', default='')

    args, option_file_index_args = parser.parse_known_args()

    progressLogFilePath = args.workingDir + "/progress_log.wfl"

    progressMessage(progressLogFilePath, "starting")


    working_dir = args.workingDir

    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            inFile0 = args.fileIndex[x][1]
        if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
            inFile1 = args.fileIndex[x][1]
	
		
    # parser.add_argument('-myEnumOption', choices=["Apple", "Orange"],
    #         help='an enum option (default="Apple")',
    #             default="Apple")

    # parser.add_argument('-myStringOption', type=str,
    #         help='a generic string option')

    
    main_program(inFile0, inFile1, working_dir)