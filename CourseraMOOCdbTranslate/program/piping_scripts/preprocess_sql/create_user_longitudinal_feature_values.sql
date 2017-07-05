-- Takes 1 seconds to execute
-- Created on Jun 30, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com
-- Creates empty user_longitudinal_feature_values table


DROP TABLE if exists `moocdb`.`user_longitudinal_feature_values`;

CREATE TABLE `moocdb`.`user_longitudinal_feature_values` (
  `longitudinal_feature_value_id` INT NOT NULL AUTO_INCREMENT ,
  `feature_extraction_id` INT ,
  `longitudinal_feature_id` INT NULL ,
  `user_id` VARCHAR(50) NULL ,
  `longitudinal_feature_week` INT(3) NULL ,
  `longitudinal_feature_value` DOUBLE NULL ,
  `date_of_extraction` DATETIME NOT NULL ,
  PRIMARY KEY (`longitudinal_feature_value_id`));
  
ALTER TABLE `moocdb`.`user_longitudinal_feature_values` 
ADD INDEX `feature_values_feature_extraction_id_idx` (`feature_extraction_id`) ;

ALTER TABLE `moocdb`.`user_longitudinal_feature_values` 
ADD INDEX `feature_values_longitudinal_feature_id_idx` (`longitudinal_feature_id` ASC) ;

ALTER TABLE `moocdb`.`user_longitudinal_feature_values`
ADD INDEX `feature_values_user_week_idx` (`user_id` ASC, `longitudinal_feature_week` ASC) ;

ALTER TABLE `moocdb`.`user_longitudinal_feature_values`
ADD INDEX `feature_values_feature_value_date_idx` (`longitudinal_feature_value_id` ASC, `date_of_extraction` ASC) ;