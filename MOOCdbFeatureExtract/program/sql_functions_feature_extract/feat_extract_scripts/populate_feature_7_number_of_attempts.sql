-- Takes 80 seconds to execute
-- Created on July 1, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Feature 7: number of attempts
-- (supported by http://francky.me/mit/moocdb/all/forum_posts_per_day_date_labels_cutoff120_with_and_without_cert.html)

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)



SELECT @feature_extraction_id, 7,
	users.user_id,
	FLOOR((UNIX_TIMESTAMP(submissions.submission_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	COUNT(*),
  @current_date
FROM `moocdb`.users AS users
INNER JOIN `moocdb`.submissions AS submissions
 ON submissions.user_id = users.user_id
WHERE users.user_dropout_week IS NOT NULL
	-- AND users.user_id < 100
	AND FLOOR((UNIX_TIMESTAMP(submissions.submission_timestamp)
			- UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) < @num_weeks
  AND submissions.validity = 1
GROUP BY users.user_id, week
HAVING week < @num_weeks
AND week >= 0
;

