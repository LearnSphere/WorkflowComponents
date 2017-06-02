
-- Created on Jan 2, 2017
-- @author: hui cehng hcheng@cs.cmu.edu
-- populate users.user_last_submission_id column


--rollback if exists
SET @exist := (SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA='moocdb' AND TABLE_NAME='users' AND COLUMN_NAME='user_last_submission_id' );
set @sqlstmt := if( @exist > 0, 'ALTER TABLE `moocdb`.`users` DROP COlUMN `user_last_submission_id`', 'select * from `moocdb`.`users` where 1=0');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

ALTER TABLE `moocdb`.`users`
ADD COLUMN `user_last_submission_id` INT NULL ;

-- Takes 2 seconds to execute
UPDATE `moocdb`.`users`
SET `moocdb`.users.user_last_submission_id = (
select distinct submission_id from `moocdb`.submissions s
join (
SELECT user_id, MAX(submission_timestamp) AS max_ts FROM `moocdb`.submissions GROUP BY user_id) max_ts
on s.user_id = max_ts.user_id and s.submission_timestamp = max_ts.max_ts
join (
SELECT user_id, max(submission_attempt_number) as max_attempt
FROM `moocdb`.submissions s
JOIN (SELECT distinct MAX(submission_timestamp) AS max_ts FROM `moocdb`.submissions GROUP BY user_id) max
ON s.submission_timestamp = max.max_ts
group by user_id) max_attempt_number
on s.user_id = max_attempt_number.user_id and s.submission_attempt_number = max_attempt_number.max_attempt
where s.user_id = users.user_id
)
;


