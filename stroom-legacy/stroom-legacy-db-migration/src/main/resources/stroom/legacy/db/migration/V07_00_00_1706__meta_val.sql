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
-- Create the meta_val table
--
CREATE TABLE IF NOT EXISTS `meta_val` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` bigint NOT NULL,
  `meta_id` bigint NOT NULL,
  `meta_key_id` int NOT NULL,
  `val` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `meta_val_create_time` (`create_time`),
  KEY `meta_val_meta_id` (`meta_id`)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

--
-- Copy data into the meta_val table
--
DROP PROCEDURE IF EXISTS copy_meta_val;
DELIMITER //
CREATE PROCEDURE copy_meta_val ()
BEGIN
    -- Can be run by multiple scripts
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'STRM_ATR_VAL') THEN

        RENAME TABLE STRM_ATR_VAL TO OLD_STRM_ATR_VAL;
    END IF;

    IF EXISTS (
            SELECT NULL 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'OLD_STRM_ATR_VAL') THEN

        INSERT INTO meta_val (
            id,
            create_time,
            meta_id,
            meta_key_id,
            val)
        SELECT
            ID,
            CRT_MS,
            STRM_ID,
            STRM_ATR_KEY_ID,
            VAL_NUM
        FROM OLD_STRM_ATR_VAL
        WHERE ID > (SELECT COALESCE(MAX(id), 0) FROM meta_key)
        AND VAL_NUM IS NOT NULL
        ORDER BY ID;

        -- Work out what to set our auto_increment start value to
        SELECT CONCAT('ALTER TABLE meta_val AUTO_INCREMENT = ', COALESCE(MAX(id) + 1, 1))
        INTO @alter_table_sql
        FROM meta_val;

        PREPARE alter_table_stmt FROM @alter_table_sql;
        EXECUTE alter_table_stmt;
    END IF;
END//
DELIMITER ;
CALL copy_meta_val();
DROP PROCEDURE copy_meta_val;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set tabstop=4 shiftwidth=4 expandtab:
