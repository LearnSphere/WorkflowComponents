import pandas as pd
import numpy as np
from sklearn.preprocessing import StandardScaler
from sklearn.cluster import KMeans
import argparse
import os

# Parse input arguments
parser = argparse.ArgumentParser()
parser.add_argument('-programDir', type=str)
parser.add_argument('-workingDir', type=str)
parser.add_argument('-node', nargs=1, action='append')
parser.add_argument('-fileIndex', nargs=2, action='append')
parser.add_argument('-excludeAttr', nargs='*', default=[], help='List of attributes to exclude')
parser.add_argument('-numClusters', type=int, default=3)
parser.add_argument('-userId', type=str, default='')
args, _ = parser.parse_known_args()

# Locate the input file
input_file = None
for n, fi in zip(args.node, args.fileIndex):
    if n[0] == '0' and fi[0] == '0':
        input_file = fi[1]

# Load the dataset
data = pd.read_csv(input_file, sep='\t')

# Select numeric columns and exclude user-specified attributes
numeric_cols = data.select_dtypes(include=np.number).columns.tolist()
for attr in args.excludeAttr:
    if attr in numeric_cols:
        numeric_cols.remove(attr)

data_numeric = data[numeric_cols]

# Normalize the data
scaler = StandardScaler()
data_normalized = scaler.fit_transform(data_numeric)

# Perform KMeans clustering
kmeans = KMeans(n_clusters=args.numClusters, n_init=25, random_state=123)
kmeans.fit(data_normalized)

# Add cluster assignments to the original dataset
data['Cluster'] = kmeans.labels_

# Sort by Cluster
data = data.sort_values(by='Cluster')

# Save the output
output_path = os.path.join(args.workingDir, "kmeans_output.txt")
data.to_csv(output_path, sep='\t', index=False)
