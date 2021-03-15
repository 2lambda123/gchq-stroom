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

--
-- Create the table
--
CREATE TABLE IF NOT EXISTS`processor_filter` (
  `id` int NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL,
  `create_time_ms` bigint NOT NULL,
  `create_user` varchar(255) NOT NULL,
  `update_time_ms` bigint NOT NULL,
  `update_user` varchar(255) NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `fk_processor_id` int NOT NULL,
  `fk_processor_filter_tracker_id` int NOT NULL,
  `data` longtext NOT NULL,
  `priority` int NOT NULL,
  `reprocess` tinyint NOT NULL DEFAULT '0',
  `enabled` tinyint NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `processor_filter_fk_processor_id` (`fk_processor_id`),
  KEY `processor_filter_fk_processor_filter_tracker_id` (`fk_processor_filter_tracker_id`),
  CONSTRAINT `processor_filter_fk_processor_filter_tracker_id` FOREIGN KEY (`fk_processor_filter_tracker_id`) REFERENCES `processor_filter_tracker` (`id`),
  CONSTRAINT `processor_filter_fk_processor_id` FOREIGN KEY (`fk_processor_id`) REFERENCES `processor` (`id`)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP PROCEDURE IF EXISTS copy_processor_filter;
DELIMITER //
CREATE PROCEDURE copy_processor_filter ()
BEGIN
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'STRM_PROC_FILT') THEN

        RENAME TABLE STRM_PROC_FILT TO OLD_STRM_PROC_FILT;
    END IF;

    -- Check again so it is idempotent
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'OLD_STRM_PROC_FILT') THEN
        --
        -- Copy data into the table, use ID predicate to make it re-runnable
        --
        INSERT INTO processor_filter (
            id,
            version,
            create_time_ms,
            create_user,
            update_time_ms,
            update_user,
            uuid,
            fk_processor_id,
            fk_processor_filter_tracker_id,
            data,
            priority,
            enabled)
        SELECT
            ID,
            VER,
            IFNULL(CRT_MS,  0),
            IFNULL(CRT_USER,  'UNKNOWN'),
            IFNULL(UPD_MS,  0),
            IFNULL(UPD_USER,  'UNKNOWN'),
            md5(UUID()),
            FK_STRM_PROC_ID,
            FK_STRM_PROC_FILT_TRAC_ID,
            DAT,
            PRIOR,
            ENBL
        FROM OLD_STRM_PROC_FILT
        WHERE ID > (SELECT COALESCE(MAX(id), 0) FROM processor_filter)
        ORDER BY ID;

        -- Work out what to set our auto_increment start value to
        SELECT CONCAT('ALTER TABLE processor_filter AUTO_INCREMENT = ', COALESCE(MAX(id) + 1, 1))
        INTO @alter_table_sql
        FROM processor_filter;

        PREPARE alter_table_stmt FROM @alter_table_sql;
        EXECUTE alter_table_stmt;
    END IF;

END//
DELIMITER ;
CALL copy_processor_filter();
DROP PROCEDURE copy_processor_filter;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=4 tabstop=4 expandtab:
