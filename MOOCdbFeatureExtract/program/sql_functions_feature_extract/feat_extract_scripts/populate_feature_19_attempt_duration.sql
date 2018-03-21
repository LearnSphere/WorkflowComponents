-- Takes 40 seconds to execute
-- Created on May 29, 2017
-- @author: Hui Cheng, CMU
-- Feature 19: Total time of observed duration for questions of quizzes - during the week

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id,longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)


SELECT @feature_extraction_id,19,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	SUM(observed_events.observed_event_duration),
    @current_date
FROM `moocdb`.users AS users
INNER JOIN `moocdb`.observed_events AS observed_events
 ON observed_events.user_id = users.user_id
WHERE users.user_dropout_week IS NOT NULL
	-- for problem_submission
	AND observed_events.observed_event_type_id = 6
	AND FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) < @num_weeks
    AND observed_events.validity = 1
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0
;


