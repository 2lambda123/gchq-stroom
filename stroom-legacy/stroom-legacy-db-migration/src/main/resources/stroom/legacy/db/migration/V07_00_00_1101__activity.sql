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
-- Rename the old ACTIVITY table
-- MUST BE DONE HERE DUE TO NAME CLASH
--
DROP PROCEDURE IF EXISTS rename_activity;
DELIMITER //
CREATE PROCEDURE rename_activity ()
BEGIN
    IF NOT EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'OLD_ACTIVITY') THEN

        IF EXISTS (
                SELECT NULL
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = database()
                AND TABLE_NAME = 'ACTIVITY') THEN

            RENAME TABLE ACTIVITY TO OLD_ACTIVITY;
        END IF;
    END IF;
END//
DELIMITER ;
CALL rename_activity();
DROP PROCEDURE rename_activity;

--
-- Create the activity table
-- MUST BE DONE HERE DUE TO NAME CLASH
--
CREATE TABLE IF NOT EXISTS activity (
  id                    int NOT NULL AUTO_INCREMENT,
  version               int NOT NULL,
  create_time_ms        bigint NOT NULL,
  create_user           varchar(255) NOT NULL,
  update_time_ms        bigint NOT NULL,
  update_user           varchar(255) NOT NULL,
  user_id               varchar(255) NOT NULL,
  json                  longtext,
  PRIMARY KEY           (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Copy data into the activity table
-- MUST BE DONE HERE DUE TO NAME CLASH
--
DROP PROCEDURE IF EXISTS copy_activity;
DELIMITER //
CREATE PROCEDURE copy_activity ()
BEGIN
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'OLD_ACTIVITY') THEN

        INSERT INTO activity (
            id,
            version,
            create_time_ms,
            create_user,
            update_time_ms,
            update_user,
            user_id,
            json)
        SELECT
            ID,
            1,
            IFNULL(CRT_MS, 0),
            IFNULL(CRT_USER, 'UNKNOWN'),
            IFNULL(UPD_MS, 0),
            IFNULL(UPD_USER, 'UNKNOWN'),
            USER_ID,
            JSON
        FROM OLD_ACTIVITY
        WHERE ID > (SELECT COALESCE(MAX(id), 0) FROM activity)
        ORDER BY ID;

        -- Work out what to set our auto_increment start value to
        SELECT CONCAT('ALTER TABLE activity AUTO_INCREMENT = ', COALESCE(MAX(id) + 1, 1))
        INTO @alter_table_sql
        FROM activity;

        PREPARE alter_table_stmt FROM @alter_table_sql;
        EXECUTE alter_table_stmt;
    END IF;
END//
DELIMITER ;
CALL copy_activity();
DROP PROCEDURE copy_activity;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=4 tabstop=4 expandtab:
