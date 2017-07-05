-- time the script takes to run on moocdb db:  xx seconds
-- date:  12/10/2013
-- author: Josep Marc Mingot
-- email:  jm.mingot@gmail.com
-- description of feature:
-- Standard deviation of the hours the user produces events and collaborations.
-- Pretends to capture how regular a student is in her schedule while doing a MOOC
-- name for feature: std_hours_working
-- would you like to be cited, and if so, how?
--
-- Modified by Colin Taylor (3/5/2014) to insert into database with feature number 301
set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @start_date = 'START_DATE_PLACEHOLDER';
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id,
																		longitudinal_feature_id,
                                                    user_id,
                                                    longitudinal_feature_week,
                                                    longitudinal_feature_value,
                                                    date_of_extraction)
SELECT @feature_extraction_id,
    301,
	user_id,
	FLOOR((UNIX_TIMESTAMP(event_timestamp) - UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) AS week,
	std(hours) AS std_hours_working,
    @current_date

FROM
	(
        SELECT
            o.user_id,
            hour(observed_event_timestamp) AS hours,
            observed_event_timestamp       AS event_timestamp
	    FROM
		    `moocdb`.observed_events as o
      INNER JOIN
        `moocdb`.users AS u
      ON o.user_id = u.user_id
        WHERE
            o.validity = 1
        AND
            u.user_dropout_week IS NOT NULL
    UNION ALL
        SELECT
		        c.user_id,
            hour(collaboration_timestamp) AS hours,
			collaboration_timestamp       AS event_timestamp
	    FROM
		    collaborations as c
      INNER JOIN
        `moocdb`.users as u
      ON c.user_id = u.user_id
        WHERE
            u.user_dropout_week IS NOT NULL
    ) AS A
GROUP BY user_id , week
;
