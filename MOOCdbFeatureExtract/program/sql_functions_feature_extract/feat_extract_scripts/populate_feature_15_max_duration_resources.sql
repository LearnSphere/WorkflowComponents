-- Takes 2700 seconds to execute
-- Created on August 23, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Feature 15: Duration of longest observed event
set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;


INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id,longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)

SELECT @feature_extraction_id, 15,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	MAX(observed_events.observed_event_duration),
    @current_date
FROM `moocdb`.users AS users
INNER JOIN `moocdb`.observed_events AS observed_events
 ON observed_events.user_id = users.user_id
WHERE users.user_dropout_week IS NOT NULL
	-- AND users.user_id < 100
	AND FLOOR((UNIX_TIMESTAMP(observed_events.observed_event_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) < @num_weeks
    AND observed_events.validity = 1
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0
;

