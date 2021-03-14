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
-- Create the fs_volume table
--
CREATE TABLE IF NOT EXISTS fs_volume (
    id                        int NOT NULL AUTO_INCREMENT,
    version                   int NOT NULL,
    create_time_ms            bigint NOT NULL,
    create_user               varchar(255) NOT NULL,
    update_time_ms            bigint NOT NULL,
    update_user               varchar(255) NOT NULL,
    path                      varchar(255) NOT NULL,
    status                    tinyint(4) NOT NULL,
    byte_limit                bigint DEFAULT NULL,
    fk_fs_volume_state_id     int NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY  path (path),
    KEY fs_volume_fk_fs_volume_state_id (fk_fs_volume_state_id),
    CONSTRAINT fs_volume_fk_fs_volume_state_id
    FOREIGN KEY (fk_fs_volume_state_id)
        REFERENCES fs_volume_state (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=4 tabstop=4 expandtab:
