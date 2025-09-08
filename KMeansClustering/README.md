# KMeans Clustering Component

## Description:

This component performs KMeans clustering on the numeric columns of a tab-delimited dataset.
It allows the user to specify:

- the number of clusters (default = 3)
- any number of attributes to exclude (e.g., identifiers or non-feature columns)

The output is a dataset identical to the input, with an additional column 'Cluster' indicating the cluster assignment of each row.

## Inputs:

- One tab-delimited (.txt or .tsv) file containing data to be clustered.

## Options:

- excludeAttr: One or more attributes to exclude from clustering (e.g., "Anon Student Id", "Timestamp"). Optional.
- numClusters: Number of clusters to form (default: 3). Optional.

## Outputs:

- One tab-delimited file named `kmeans_output.txt` in the output directory, containing the original dataset plus the added 'Cluster' column (sorted by cluster index).

## How to Use:

1. Add this component to your workflow in LearnSphere.
2. Connect it to any component that produces a tab-delimited dataset.
3. In the options panel:
   - Enter one or more columns to exclude in `excludeAttr` (optional).
   - Enter the desired number of clusters in `numClusters` (optional).
4. Run the workflow. The output will appear in the component’s output tab.

## Dependencies:

- Python 3.6+
- pandas
- numpy
- scikit-learn

## Notes:

- If no `excludeAttr` is specified, all numeric columns will be used for clustering.
- Non-numeric columns (except those excluded) are automatically ignored.
- Make sure scikit-learn is installed in your Python environment (`pip install scikit-learn`).

## Author:

Jharana Sapkota  
jharana@vt.edu
