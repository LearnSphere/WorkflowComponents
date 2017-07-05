-- time the script takes to run on moocdb db:  xx seconds
-- date:  12/10/2013
-- author: Josep Marc Mingot Hidalgo
-- email:  jm.mingot@gmail.com
-- description of feature:
-- Average time(in days) the student takes to react when a new resource is posted.
-- Pretends to capture how fast a student is reacting to new content.
-- name for feature: time_to_react
-- would you like to be cited, and if so, how?
-- Josep Marc Mingot Hidalgo
-- Modified by Colin Taylor (3/5/2014) to insert into database with feature number 302

-- Modified by Ben Schreck (6/1/15) to make faster and make sure to not include
-- users with null dropout weeks

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @num_weeks = NUM_WEEKS_PLACEHOLDER;
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id,
																	longitudinal_feature_id,
                                                    user_id,
                                                    longitudinal_feature_week,
                                                    longitudinal_feature_value,
                                                    date_of_extraction)

SELECT @feature_extraction_id,
	302,
	a.user_id,
	FLOOR((UNIX_TIMESTAMP(a.observed_event_timestamp) - UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	AVG((UNIX_TIMESTAMP(a.observed_event_timestamp) - UNIX_TIMESTAMP(b.resource_release_timestamp))) AS avg_reacting_time,
    @current_date
FROM
		`moocdb`.observed_events AS a

        INNER JOIN `moocdb`.users as u
            ON a.user_id = u.user_id

	    INNER JOIN `moocdb`.resources_urls as c
            ON a.url_id = c.url_id

        INNER JOIN `moocdb`.resources as b
            ON b.resource_id = c.resource_id
WHERE
    a.validity = 1
AND
    u.user_dropout_week IS NOT NULL
GROUP BY user_id , week;
