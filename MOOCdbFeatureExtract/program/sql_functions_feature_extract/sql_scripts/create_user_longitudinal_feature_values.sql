-- Takes 1 seconds to execute
-- Created on Jun 30, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Creates empty user_longitudinal_feature_values table

--legacy command: for tables that still have the old name
-- DROP TABLE if exists `moocdb`.`dropout_feature_values`;



DROP TABLE if exists `moocdb`.`user_longitudinal_feature_values`;

CREATE TABLE `moocdb`.`user_longitudinal_feature_values` (
  `longitudinal_feature_value_id` INT NOT NULL AUTO_INCREMENT ,
  `longitudinal_feature_id` INT(3) NULL ,
  `user_id` VARCHAR(50) NULL ,
  `longitudinal_feature_week` INT(3) NULL ,
  `longitudinal_feature_value` DOUBLE NULL ,
  `date_of_extraction` DATETIME NOT NULL ,
  PRIMARY KEY (`longitudinal_feature_value_id`) );

ALTER TABLE `moocdb`.`user_longitudinal_feature_values` CHANGE COLUMN `longitudinal_feature_id` `longitudinal_feature_id` INT(3) NULL
, ADD INDEX `longitudinal_feature_id_idx` (`longitudinal_feature_id` ASC) ;

ALTER TABLE `moocdb`.`user_longitudinal_feature_values`
ADD INDEX `user_week_idx` (`user_id` ASC, `longitudinal_feature_week` ASC) ;

ALTER TABLE `moocdb`.`user_longitudinal_feature_values`
ADD INDEX `feature_value_date_idx` (`longitudinal_feature_value_id` ASC, `date_of_extraction` ASC) ;