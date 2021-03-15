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
-- Create the explorer_path table
--
CREATE TABLE IF NOT EXISTS `explorer_path` (
  `ancestor` int NOT NULL,
  `descendant` int NOT NULL,
  `depth` int NOT NULL,
  `order_index` int NOT NULL,
  PRIMARY KEY (`ancestor`,`descendant`),
  KEY `explorer_path_descendant` (`descendant`),
  KEY `explorer_path_descendant_depth` (`descendant`,`depth`),
  KEY `explorer_path_ancestor_depth_order_index` (`ancestor`,`depth`,`order_index`),
  KEY `explorer_path_depth` (`depth`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Copy data into the explorer table
--
DROP PROCEDURE IF EXISTS copy_explorer;
DELIMITER //
CREATE PROCEDURE copy_explorer ()
BEGIN
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'explorerTreePath') THEN

        RENAME TABLE explorerTreePath TO OLD_explorertreepath;
    END IF;

    -- Check again so it is idempotent
    IF EXISTS (
            SELECT NULL
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = database()
            AND TABLE_NAME = 'OLD_explorerTreePath') THEN

        INSERT INTO explorer_path (
            ancestor, 
            descendant, 
            depth, 
            order_index)
        SELECT 
            ancestor, 
            descendant, 
            depth, 
            orderIndex
        FROM OLD_explorerTreePath;
    END IF;

END//
DELIMITER ;
CALL copy_explorer();
DROP PROCEDURE copy_explorer;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set tabstop=4 shiftwidth=4 expandtab:
