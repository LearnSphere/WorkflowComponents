-- Adds NULL 'validity' column to submissions and observed_events tables

DROP PROCEDURE IF EXISTS `moocdb`.AlterSubmissionsValidity;

DROP PROCEDURE IF EXISTS `moocdb`.AlterObservedEventsValidity;

CREATE PROCEDURE `moocdb`.AlterSubmissionsValidity()
    BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'moocdb' AND
                            TABLE_NAME = 'submissions' AND
                            COLUMN_NAME = 'validity');
    IF _count = 0 THEN
    ALTER TABLE `moocdb`.`submissions`
    ADD COLUMN `validity` INT(1) NULL ;
    END IF;
    END;


CREATE PROCEDURE `moocdb`.AlterObservedEventsValidity()
    BEGIN
    DECLARE _count INT;
    SET _count = (  SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE   TABLE_SCHEMA = 'moocdb' AND
                            TABLE_NAME = 'observed_events' AND
                            COLUMN_NAME = 'validity');
    IF _count = 0 THEN
        ALTER TABLE `moocdb`.`observed_events`
        ADD COLUMN `validity` INT(1) NULL ;
    END IF;
    END;


CALL `moocdb`.AlterSubmissionsValidity();

CALL `moocdb`.AlterObservedEventsValidity();
