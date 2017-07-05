DROP TABLE if exists `moocdb`.`feature_extractions`;

CREATE TABLE `moocdb`.`feature_extractions` (
  `feature_extraction_id` INT NOT NULL AUTO_INCREMENT,
  `created_by` VARCHAR(100) NULL ,
  `start_timestamp` DATETIME ,
  `end_timestamp` DATETIME ,
  `start_date` DATETIME ,
  `num_of_week` INT NULL ,
  `features_list` VARCHAR(250) NULL ,
  PRIMARY KEY (`feature_extraction_id`),
  UNIQUE KEY (`start_date`, `num_of_week`, `features_list`) );

ALTER TABLE `moocdb`.`feature_extractions` 
ADD INDEX `feature_ext_created_by_idx` (`created_by`) ;
