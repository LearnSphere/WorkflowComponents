-- Created on Feb 17, 2014
-- @author: Colin Taylor colin2328@gmail.com
-- Feature 210- Average time between problem submission and problem due date (in seconds)

-- Comments from Sebastien Boyer : If predeadlines are not available replace problems.problem_hard_deadline with end_date

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)


SELECT @feature_extraction_id, 210,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(submissions.submission_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	AVG((UNIX_TIMESTAMP(submissions.submission_timestamp)
			- UNIX_TIMESTAMP(problems.problem_hard_deadline))) AS time_difference,
    @current_date
FROM `moocdb`.submissions
INNER JOIN `moocdb`.users
	ON submissions.user_id = users.user_id
INNER JOIN `moocdb`.problems
	ON submissions.problem_id = problems.problem_id
WHERE users.user_dropout_week IS NOT NULL
AND submissions.validity = 1
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0;
