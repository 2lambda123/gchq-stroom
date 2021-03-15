-- ------------------------------------------------------------------------
-- Copyright 2020 Crown Copyright
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- ------------------------------------------------------------------------

-- Stop NOTE level warnings about objects (not)? existing
SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;

--Create Table: CREATE TABLE `STRM_PROC_FILT_TRAC` (
--  `ID` int NOT NULL AUTO_INCREMENT,
--  `VER` tinyint NOT NULL,
--  `MIN_STRM_ID` bigint NOT NULL,
--  `MIN_EVT_ID` bigint NOT NULL,
--  `MIN_STRM_CRT_MS` bigint DEFAULT NULL,
--  `MAX_STRM_CRT_MS` bigint DEFAULT NULL,
--  `STRM_CRT_MS` bigint DEFAULT NULL,
--  `LAST_POLL_MS` bigint DEFAULT NULL,
--  `LAST_POLL_TASK_CT` int DEFAULT NULL,
--  `STAT` varchar(255) DEFAULT NULL,
--  `STRM_CT` bigint DEFAULT NULL,
--  `EVT_CT` bigint DEFAULT NULL,
--  PRIMARY KEY (`ID`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4

-- Create the table
CREATE TABLE IF NOT EXISTS `processor_filter_tracker` (
  `id` int NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL,
  `min_meta_id` bigint NOT NULL,
  `min_event_id` bigint NOT NULL,
  `min_meta_create_ms` bigint DEFAULT NULL,
  `max_meta_create_ms` bigint DEFAULT NULL,
  `meta_create_ms` bigint DEFAULT NULL,
  `last_poll_ms` bigint DEFAULT NULL,
  `last_poll_task_count` int DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `meta_count` bigint DEFAULT NULL,
  `event_count` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS copy_processor_filter_tracker;
DELIMITER //
CREATE PROCEDURE copy_processor_filter_tracker ()
BEGIN

    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'STRM_PROC_FILT_TRAC') THEN

        RENAME TABLE STRM_PROC_FILT_TRAC TO OLD_STRM_PROC_FILT_TRAC;
    END IF;

    -- Check again so it is idempotent
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'OLD_STRM_PROC_FILT_TRAC') THEN
        -- Copy data into the table, use ID predicate to make it re-runnable
        INSERT
        INTO processor_filter_tracker (
            id,
            version,
            min_meta_id,
            min_event_id,
            min_meta_create_ms,
            max_meta_create_ms,
            meta_create_ms,
            last_poll_ms,
            last_poll_task_count,
            status,
            meta_count,
            event_count)
        SELECT
            ID,
            VER,
            MIN_STRM_ID,
            MIN_EVT_ID,
            MIN_STRM_CRT_MS,
            MAX_STRM_CRT_MS,
            STRM_CRT_MS,
            LAST_POLL_MS,
            LAST_POLL_TASK_CT,
            STAT,
            STRM_CT,
            EVT_CT
        FROM OLD_STRM_PROC_FILT_TRAC
        WHERE ID > (SELECT COALESCE(MAX(id), 0) FROM processor_filter_tracker)
        ORDER BY ID;

        -- Work out what to set our auto_increment start value to
        SELECT CONCAT('ALTER TABLE processor_filter_tracker AUTO_INCREMENT = ', COALESCE(MAX(id) + 1, 1))
        INTO @alter_table_sql
        FROM processor_filter_tracker;

        PREPARE alter_table_stmt FROM @alter_table_sql;
        EXECUTE alter_table_stmt;
    END IF;

END//
DELIMITER ;
CALL copy_processor_filter_tracker();
DROP PROCEDURE copy_processor_filter_tracker;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=4 tabstop=4 expandtab:
