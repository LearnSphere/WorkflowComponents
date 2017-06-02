-- Takes 2 seconds to execute
-- Created on June 30, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- edited by Colin Taylor on Feb 17, 2014
-- Feature 3: number of forum posts
-- (supported by http://francky.me/mit/moocdb/all/forum_posts_per_day_date_labels_cutoff120_with_and_without_cert.html)

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value, date_of_extraction)

SELECT @feature_extraction_id, 3,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(collaborations.collaboration_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	COUNT(*),
    @current_date
FROM `moocdb`.users AS users
INNER JOIN `moocdb`.collaborations AS collaborations
 ON collaborations.user_id = users.user_id
WHERE users.user_dropout_week IS NOT NULL
	AND (collaborations.collaboration_type_id = 1 OR collaborations.collaboration_type_id = 2 OR collaborations.collaboration_type_id = 3)
	AND FLOOR((UNIX_TIMESTAMP(collaborations.collaboration_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) < @num_weeks
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0
;

