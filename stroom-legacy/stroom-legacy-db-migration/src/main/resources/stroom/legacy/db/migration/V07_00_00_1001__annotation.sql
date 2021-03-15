-- Stop NOTE level warnings about objects (not)? existing
SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;

--
-- Create the annotation tables
--
CREATE TABLE IF NOT EXISTS annotation (
  id                    bigint NOT NULL AUTO_INCREMENT,
  version               int NOT NULL,
  create_time_ms        bigint NOT NULL,
  create_user           varchar(255) NOT NULL,
  update_time_ms        bigint NOT NULL,
  update_user           varchar(255) NOT NULL,
  title                 longtext,
  subject               longtext,
  status                varchar(255) NOT NULL,
  assigned_to           varchar(255) DEFAULT NULL,
  comment               longtext,
  history               longtext,
  PRIMARY KEY           (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS annotation_entry (
  id                    bigint NOT NULL AUTO_INCREMENT,
  version               int NOT NULL,
  create_time_ms        bigint NOT NULL,
  create_user           varchar(255) NOT NULL,
  update_time_ms        bigint NOT NULL,
  update_user           varchar(255) NOT NULL,
  fk_annotation_id      bigint NOT NULL,
  type                  varchar(255) NOT NULL,
  data                  longtext,
  PRIMARY KEY           (id),
  CONSTRAINT            annotation_entry_fk_annotation_id FOREIGN KEY (fk_annotation_id) REFERENCES annotation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS annotation_data_link (
  id                    bigint NOT NULL AUTO_INCREMENT,
  fk_annotation_id      bigint NOT NULL,
  stream_id             bigint NOT NULL,
  event_id              bigint NOT NULL,
  PRIMARY KEY           (id),
  UNIQUE KEY            fk_annotation_id_stream_id_event_id (fk_annotation_id, stream_id, event_id),
  CONSTRAINT            annotation_data_link_fk_annotation_id FOREIGN KEY (fk_annotation_id) REFERENCES annotation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET SQL_NOTES=@OLD_SQL_NOTES;
