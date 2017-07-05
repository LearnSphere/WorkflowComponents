-- Created on Feb 14, 2014
-- @author: Colin Taylor colin2328@gmail.com
-- Feature 208- number of attempts that were correct


set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;


INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)


SELECT @feature_extraction_id, 208,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(submissions.submission_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	COUNT(*),
    @current_date
FROM `moocdb`.users AS users
INNER JOIN `moocdb`.submissions AS submissions
	ON submissions.user_id = users.user_id
INNER JOIN `moocdb`.assessments
	ON assessments.submission_id = submissions.submission_id
WHERE users.user_dropout_week IS NOT NULL
AND assessments.assessment_grade = 1
AND submissions.validity = 1
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0;
