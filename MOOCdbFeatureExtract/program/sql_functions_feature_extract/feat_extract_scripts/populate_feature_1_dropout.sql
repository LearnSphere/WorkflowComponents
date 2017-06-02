-- Takes 12 seconds
-- Created on June 30, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Feature 1: has the student dropped out (binary, that's what we try to predict)
-- Edited by Colin Taylor on Nov 27, 2013 to include missing last submission id
-- Meant to be run after users_populate_dropout_week.sql is run

-- TRUNCATE TABLE `moocdb`.user_longitudinal_feature_values;
-- ALTER TABLE `moocdb`.user_longitudinal_feature_values AUTO_INCREMENT = 1;


CREATE PROCEDURE `moocdb`.compute_feature_1()
BEGIN

  DECLARE v_max INT UNSIGNED DEFAULT 'NUM_WEEKS_PLACEHOLDER';
  DECLARE v_counter INT UNSIGNED DEFAULT 0;

  SET @current_date = CAST('CURRENT_DATE_PLACEHOLDER' AS DATETIME);
  set @feature_extraction_id = FEATURE_EXTRACTION_ID_PLACEHOLDER;
  SET @start_date = CAST('START_DATE_PLACEHOLDER' AS DATETIME);
  SET @earliest_submission_date = CAST('EARLIEST_SUBMISSION_DATE_PLACEHOLDER' AS DATETIME);
  SET @week_diff = FLOOR((UNIX_TIMESTAMP(@earliest_submission_date) - UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7));
  
  WHILE v_counter < v_max DO

      INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value, date_of_extraction)
      SELECT @feature_extraction_id, 1, users.user_id, v_counter, 0, @current_date
      FROM `moocdb`.users AS users
      WHERE (users.user_dropout_week + @week_diff) <= v_counter
        AND users.user_dropout_week IS NOT NULL;

      INSERT INTO `moocdb`.user_longitudinal_feature_values(feature_extraction_id, longitudinal_feature_id, user_id, longitudinal_feature_week, longitudinal_feature_value, date_of_extraction)
      SELECT @feature_extraction_id, 1, users.user_id, v_counter, 1, @current_date
      FROM `moocdb`.users AS users
      WHERE (users.user_dropout_week + @week_diff) > v_counter
        AND users.user_dropout_week  IS NOT NULL;



      SET v_counter=v_counter+1;
  END WHILE;
END;


CALL `moocdb`.compute_feature_1();

DROP PROCEDURE IF EXISTS `moocdb`.compute_feature_1;




