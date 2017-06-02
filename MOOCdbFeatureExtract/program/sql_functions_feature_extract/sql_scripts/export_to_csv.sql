-- Created on Jan 9, 2016
-- @author: Alec Anderson for LIDS, MIT lab: alecand@mit.edu
-- Exports user_longitudinal_feature_values table to a csv file
-- Currently requires setting secure_file_priv system variable to '' manually
-- Currently writes to /tmp folder

-- SET SESSION secure_file_priv = '';

SELECT 'longitudinal_feature_value_id','longitudinal_feature_id','user_id',
'longitudinal_feature_week','longitudinal_feature_value','date_of_extraction'
UNION ALL

SELECT * FROM moocdb.user_longitudinal_feature_values
--INTO OUTFILE '/tmp/user_longitudinal_feature_values.csv'
INTO OUTFILE 'c:/MOOC_DB/MOOCdb-DSF-master/user_longitudinal_feature_values.csv'
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n'