-- Takes 12 seconds to execute (if index below is created!)
-- Created on July 1, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Feature 10: total time spent on all resources during the week (feature 2) per Number of correct problems (feature 8)


-- You need to create this index, otherwise it will take for ever
-- Takes 10 seconds to execute
-- ALTER TABLE `moocdb`.`user_longitudinal_feature_values`
-- ADD INDEX `user_week_idx` (`user_id` ASC, `longitudinal_feature_week` ASC) ;

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;


INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)


SELECT @feature_extraction_id, 10,
	user_longitudinal_feature_values.user_id,
	user_longitudinal_feature_values.longitudinal_feature_week,
	CASE WHEN user_longitudinal_feature_values.longitudinal_feature_value=0 then 0 else user_longitudinal_feature_values2.longitudinal_feature_value  / user_longitudinal_feature_values.longitudinal_feature_value end,
  @current_date
FROM `moocdb`.user_longitudinal_feature_values AS user_longitudinal_feature_values,
	`moocdb`.user_longitudinal_feature_values AS user_longitudinal_feature_values2
WHERE user_longitudinal_feature_values.user_id = user_longitudinal_feature_values2.user_id
	AND user_longitudinal_feature_values.longitudinal_feature_week = user_longitudinal_feature_values2.longitudinal_feature_week
	AND user_longitudinal_feature_values.longitudinal_feature_id = 8 and user_longitudinal_feature_values.feature_extraction_id = @feature_extraction_id
    #AND user_longitudinal_feature_values.date_of_extraction >= @current_date
    AND user_longitudinal_feature_values2.longitudinal_feature_id = 2 and user_longitudinal_feature_values2.feature_extraction_id = @feature_extraction_id
    #AND user_longitudinal_feature_values2.date_of_extraction >= @current_date
;

