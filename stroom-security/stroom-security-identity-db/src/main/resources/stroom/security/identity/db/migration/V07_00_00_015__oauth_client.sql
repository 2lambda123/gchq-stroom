-- ------------------------------------------------------------------------
-- Copyright 2021 Crown Copyright
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
-- Create the oauth_client table
--
CREATE TABLE IF NOT EXISTS oauth_client (
    id              int(11) NOT NULL AUTO_INCREMENT,
    version         int(11) NOT NULL,
    create_time_ms  bigint(20) NOT NULL,
    create_user     varchar(255) NOT NULL,
    update_time_ms  bigint(20) NOT NULL,
    update_user     varchar(255) NOT NULL,
    name            varchar(255) NOT NULL,
    client_id       varchar(255) NOT NULL,
    client_secret   varchar(255) NOT NULL,
    uri_pattern     longtext,
    PRIMARY KEY (id),
    UNIQUE KEY client_id (client_id),
    UNIQUE KEY name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET SQL_NOTES=@OLD_SQL_NOTES;

-- vim: set shiftwidth=4 tabstop=4 expandtab:
