-- Takes ?? seconds to execute
-- Created on June 1, 2017
-- @author: Hui Cheng, CMU
-- Feature 303: final grade
set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)

SELECT @feature_extraction_id, 303,
	users.user_id,
	-1 AS week,
	users.user_final_grade,
    @current_date
FROM `moocdb`.users AS users
WHERE users.user_dropout_week IS NOT NULL
	-- AND users.user_id < 100
	AND users.user_final_grade IS NOT NULL
;
