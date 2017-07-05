-- Created on Feb 3rd, 2014
-- @author:  Colin Taylor
-- Feature 205: Pset Grade over time: pset grade - avg (pset grade from previos weeks)
-- Meant to be run in order to run after populate_feature_204_pset_grade.sql



CREATE PROCEDURE `moocdb`.populate_205()
BEGIN
    DECLARE x  INT;
    DECLARE v_max INT UNSIGNED DEFAULT 'NUM_WEEKS_PLACEHOLDER';
    SET x = 0;
    set @current_date = cast('CURRENT_DATE_PLACEHOLDER' as datetime);
    set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;
    
    WHILE x  < v_max DO
        INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value,date_of_extraction)
        SELECT  @feature_extraction_id, 205, d1.user_id, x AS week, d1.longitudinal_feature_value -
            (SELECT AVG(longitudinal_feature_value)
            FROM `moocdb`.user_longitudinal_feature_values AS d2 WHERE feature_extraction_id = @feature_extraction_id AND longitudinal_feature_id = 204 AND longitudinal_feature_week < x AND d1.user_id = d2.user_id ),
            @current_date
        FROM `moocdb`.user_longitudinal_feature_values AS d1 WHERE feature_extraction_id = @feature_extraction_id AND longitudinal_feature_id = 204 AND longitudinal_feature_week = x ;
        SET  x = x + 1;
    END WHILE;
END;

CALL `moocdb`.populate_205();

DROP PROCEDURE IF EXISTS `moocdb`.populate_205;

