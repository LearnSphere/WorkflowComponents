-- Created on Nov 21, 2013
-- @author: Colin Taylor colin2328@gmail.com
-- Feature 201- number of forum responses per week (also known as CF1)
set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)

SELECT @feature_extraction_id, 201,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(collaborations.collaboration_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	COUNT(*),
    @current_date
FROM `moocdb`.users AS users
INNER JOIN `moocdb`.collaborations AS collaborations
	ON collaborations.user_id = users.user_id
WHERE users.user_dropout_week IS NOT NULL
AND collaborations.collaboration_type_id = 2
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0
;

