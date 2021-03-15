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

CREATE TABLE IF NOT EXISTS index_volume_group (
  id                    int NOT NULL AUTO_INCREMENT,
  version               int NOT NULL,
  create_time_ms        bigint NOT NULL,
  create_user           varchar(255) NOT NULL,
  update_time_ms        bigint NOT NULL,
  update_user           varchar(255) NOT NULL,
  name                  varchar(255) NOT NULL,
  -- 'name' needs to be unique because it is used as a reference by IndexDoc.
  -- IndexDoc uses this name because it is fully portable -- if an index is imported
  -- then as long as it has the right index volume group name and the group exists
  -- it will use that index volume group. This would not be the case if the
  -- reference was a database generated ID or a uuid.
  UNIQUE (name),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS index_volume (
  id                        int NOT NULL AUTO_INCREMENT,
  version                   int NOT NULL,
  create_time_ms            bigint NOT NULL,
  create_user               varchar(255) NOT NULL,
  update_time_ms            bigint NOT NULL,
  update_user               varchar(255) NOT NULL,
  node_name                 varchar(255) DEFAULT NULL,
  path                      varchar(255) DEFAULT NULL,
  fk_index_volume_group_id  int NOT NULL,
  state                     tinyint DEFAULT NULL,
  bytes_limit               bigint DEFAULT NULL,
  bytes_used                bigint DEFAULT NULL,
  bytes_free                bigint DEFAULT NULL,
  bytes_total               bigint DEFAULT NULL,
  status_ms                 bigint DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY node_name_path (
      fk_index_volume_group_id,
      node_name,
      path),
  CONSTRAINT index_volume_group_link_fk_group_name
      FOREIGN KEY (fk_index_volume_group_id)
      REFERENCES index_volume_group (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET SQL_NOTES=@OLD_SQL_NOTES;
