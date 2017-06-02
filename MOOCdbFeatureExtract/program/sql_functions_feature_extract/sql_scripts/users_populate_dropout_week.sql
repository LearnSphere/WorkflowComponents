-- Takes 4 seconds to execute
-- Created on Jun 30, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Edited by Colin Taylor on Nov 27, 2013 to
-- Meant to be run after create_dropout_feature_values.sql and users_populate_user_last_submission_id.sql

set @start_date = 'START_DATE_PLACEHOLDER';

--rollback if exists
SET @exist := (SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA='moocdb' AND TABLE_NAME='users' AND COLUMN_NAME='user_dropout_week' );
set @sqlstmt := if( @exist > 0, 'ALTER TABLE `moocdb`.`users` DROP COlUMN `user_dropout_week`', 'select * from `moocdb`.`users` where 1=0');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

ALTER TABLE `moocdb`.`users`
ADD COLUMN `user_dropout_week` INT(2) NULL ;

--rollback if exists
SET @exist := (SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS
          WHERE TABLE_SCHEMA='moocdb' AND TABLE_NAME='users' AND COLUMN_NAME='user_dropout_timestamp' );
set @sqlstmt := if( @exist > 0, 'ALTER TABLE `moocdb`.`users` DROP COlUMN `user_dropout_timestamp`', 'select * from `moocdb`.`users` where 1=0');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

ALTER TABLE `moocdb`.`users`
ADD COLUMN `user_dropout_timestamp` DATETIME NULL ;

-- Takes 2 seconds to execute
UPDATE `moocdb`.`users`
SET users.user_dropout_week = (
    SELECT FLOOR((UNIX_TIMESTAMP(submissions.submission_timestamp) - UNIX_TIMESTAMP(@start_date)) / (3600 * 24 * 7)) + 1 AS week
    FROM `moocdb`.`submissions` AS submissions
    WHERE submissions.submission_id = users.user_last_submission_id
)
;

-- Takes 2 seconds to execute
UPDATE `moocdb`.`users`
SET users.user_dropout_timestamp = (
    SELECT submissions.submission_timestamp
    FROM `moocdb`.`submissions` AS submissions
    WHERE submissions.submission_id = users.user_last_submission_id
)
;
