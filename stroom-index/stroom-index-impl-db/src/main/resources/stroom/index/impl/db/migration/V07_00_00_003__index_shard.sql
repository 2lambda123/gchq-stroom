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

CREATE TABLE IF NOT EXISTS index_shard (
    id                    bigint NOT NULL AUTO_INCREMENT,
    node_name             varchar(255) NOT NULL,
    fk_volume_id          int NOT NULL,
    index_uuid            varchar(255) NOT NULL,
    commit_document_count int DEFAULT NULL,
    commit_duration_ms    bigint DEFAULT NULL,
    commit_ms             bigint DEFAULT NULL,
    document_count        int DEFAULT 0,
    file_size             bigint DEFAULT 0,
    status                tinyint(4) NOT NULL,
    index_version         varchar(255) DEFAULT NULL,
    partition_name        varchar(255) NOT NULL,
    partition_from_ms     bigint DEFAULT NULL,
    partition_to_ms       bigint DEFAULT NULL,
    PRIMARY KEY (id),
    KEY index_shard_fk_volume_id (fk_volume_id),
    KEY index_shard_index_uuid (index_uuid),
    CONSTRAINT index_shard_fk_volume_id
        FOREIGN KEY (fk_volume_id)
        REFERENCES index_volume (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=4 tabstop=4 expandtab:
