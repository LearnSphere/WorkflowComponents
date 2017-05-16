DROP TABLE if exists `moocdb`.`longitudinal_features`;

CREATE TABLE `moocdb`.`longitudinal_features` (
  `longitudinal_feature_id` INT(5) NOT NULL,
  `longitudinal_feature_name` TINYTEXT NULL ,
  `longitudinal_feature_description` TEXT NULL ,
  PRIMARY KEY (`longitudinal_feature_id`) );
