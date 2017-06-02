DROP TABLE if exists `moocdb`.`models`;

CREATE TABLE `moocdb`.`models` (
  `longitudinal_feature_value_id` INT NOT NULL AUTO_INCREMENT ,
  `longitudinal_feature_id` INT(3) NULL,
  `longitudinal_feature_week` INT(2) NULL ,
  `longitudinal_feature_value` DOUBLE NULL ,
  `exp_id` INT(8) NULL,
  PRIMARY KEY (`longitudinal_feature_value_id`) );
