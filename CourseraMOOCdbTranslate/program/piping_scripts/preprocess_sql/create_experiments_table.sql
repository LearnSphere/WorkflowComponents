DROP TABLE if exists `moocdb`.`experiments`;


CREATE TABLE `moocdb`.`experiments` (
  `exp_id` INT NOT NULL AUTO_INCREMENT ,
  `lead` INT(1) NULL ,
  `lag` INT(1) NULL ,
  `auc_train` DOUBLE NULL ,
  `course_test_id` TINYTEXT NULL ,
  `auc_test` DOUBLE NULL ,
  `parameter_lambda` DOUBLE NULL ,
  `parameter_epsilon` DOUBLE NULL ,
  `experiment_time_stamp` DATETIME NULL ,
  PRIMARY KEY (`exp_id`) );
