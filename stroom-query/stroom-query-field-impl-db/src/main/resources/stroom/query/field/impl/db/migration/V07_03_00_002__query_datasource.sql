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
-- Create the index_field table
--
CREATE TABLE IF NOT EXISTS `index_field` (
    `id`                        bigint NOT NULL AUTO_INCREMENT,
    `fk_field_source_id`        int NOT NULL,
    `type`                      tinyint NOT NULL,
    `name`                      varchar(255) NOT NULL,
    `analyzer_type`             tinyint NOT NULL,
    `indexed`                   tinyint NOT NULL DEFAULT '0',
    `stored`                    tinyint NOT NULL DEFAULT '0',
    `term_positions`            tinyint NOT NULL DEFAULT '0',
    `case_sensitive`            tinyint NOT NULL DEFAULT '0',
    PRIMARY KEY                 (`id`),
    UNIQUE KEY                  `field_source_id_name` (`fk_field_source_id`, `name`),
    CONSTRAINT `field_fk_field_source_id` FOREIGN KEY (`fk_field_source_id`) REFERENCES `field_source` (`id`)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set tabstop=4 shiftwidth=4 expandtab:
