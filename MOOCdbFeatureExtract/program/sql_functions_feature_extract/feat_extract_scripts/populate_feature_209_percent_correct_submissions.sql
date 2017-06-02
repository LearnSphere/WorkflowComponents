-- Created on Feb 14, 2014
-- @author: Colin Taylor colin2328@gmail.com
-- Feature 209- Percentage of total submissions that were correct (feature 208 / feature 7)
-- Must have run populate_feature_208 and populate_feature_7 first!

set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;

INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)


SELECT @feature_extraction_id, 209,
	features.user_id,
	features.longitudinal_feature_week,
	CASE WHEN features.longitudinal_feature_value=0 then 0 else features2.longitudinal_feature_value  / features.longitudinal_feature_value end,
    @current_date
FROM `moocdb`.user_longitudinal_feature_values AS features,
	`moocdb`.user_longitudinal_feature_values AS features2
WHERE features.user_id = features2.user_id
	AND features.longitudinal_feature_week = features2.longitudinal_feature_week
	AND features.longitudinal_feature_id = 7
	AND features2.longitudinal_feature_id = 208
	AND features.feature_extraction_id = @feature_extraction_id
	AND features2.feature_extraction_id = @feature_extraction_id
  --  AND features.date_of_extraction >= @current_date
  --  AND features2.date_of_extraction >= @current_date
;
