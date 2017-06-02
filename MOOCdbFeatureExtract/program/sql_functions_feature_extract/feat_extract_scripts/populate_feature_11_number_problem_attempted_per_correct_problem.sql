-- Takes 10 seconds to execute (if index below is created!)
-- Created on July 1, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Feature 11: number of problem attempted (feature 6) / number of correct problems (feature 8)


-- You need to create this index, otherwise it will take for ever
-- Takes 10 seconds to execute
-- ALTER TABLE `moocdb`.`user_longitudinal_feature_values`
-- ADD INDEX `user_week_idx` (`user_id` ASC, `dropout_feature_value_week` ASC) ;
set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)


SELECT @feature_extraction_id, 11,
	features.user_id,
	features.longitudinal_feature_week,
	CASE WHEN features.longitudinal_feature_value=0  then 0 else features2.longitudinal_feature_value  / features.longitudinal_feature_value end,
  @current_date
FROM `moocdb`.user_longitudinal_feature_values AS features,
	`moocdb`.user_longitudinal_feature_values AS features2
WHERE features.user_id = features2.user_id
	AND features.longitudinal_feature_week = features2.longitudinal_feature_week
	AND features.longitudinal_feature_id = 8 and features.feature_extraction_id = @feature_extraction_id
    #AND features.date_of_extraction >= @current_date
	AND features2.longitudinal_feature_id = 6 and features2.feature_extraction_id = @feature_extraction_id
    #AND features2.date_of_extraction >= @current_date
;

