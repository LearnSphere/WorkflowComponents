-- Created on Jan 31, 2014
-- @author:  Colin Taylor, modified by Hui Cheng
-- Feature 204: Pset Grade: Number of homework problems correct in a week's problems / number of homework problems in a week
-- Meant to be run in order to run after problems_populate_module_week.sql

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
SET @start_date = CAST('START_DATE_PLACEHOLDER' AS DATETIME);
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)



SELECT @feature_extraction_id, 204, submissions.user_id, 
	FLOOR((UNIX_TIMESTAMP(problems.problem_hard_deadline) - UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week, 
	COUNT(*) /
	(SELECT COUNT(*)  FROM `moocdb`.problems AS p2 WHERE (p2.problem_type_id = 7 OR p2.problem_type_id = 8)
		AND p2.problem_week = problems.problem_week 
		GROUP BY FLOOR((UNIX_TIMESTAMP(p2.problem_hard_deadline) - UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7))) AS pset_grade,
    @current_date
FROM `moocdb`.submissions
INNER JOIN `moocdb`.problems
	ON submissions.problem_id = problems.problem_id
INNER JOIN `moocdb`.assessments
	ON assessments.submission_id = submissions.submission_id
INNER JOIN `moocdb`.users
	ON submissions.user_id = users.user_id
WHERE users.user_dropout_week IS NOT NULL
AND (problems.problem_type_id = 7 OR problems.problem_type_id = 8)
AND assessments.assessment_grade = 1
AND submissions.validity = 1
GROUP BY submissions.user_id, week
HAVING week < @num_weeks
AND week >= 0;
