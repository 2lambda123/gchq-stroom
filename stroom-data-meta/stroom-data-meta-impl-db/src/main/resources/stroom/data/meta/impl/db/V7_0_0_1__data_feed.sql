--
-- Create the data_feed table
--
CREATE TABLE IF NOT EXISTS data_feed (
  id 				    int(11) NOT NULL AUTO_INCREMENT,
  name				    varchar(255) NOT NULL,
  PRIMARY KEY           (id),
  UNIQUE KEY            name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Copy data into the data_feed table
--
DROP PROCEDURE IF EXISTS copy;
DELIMITER //
CREATE PROCEDURE copy ()
BEGIN
  IF (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'FD' > 0) THEN

    INSERT INTO data_feed (id, name)
    SELECT ID, NAME
    FROM FD
    WHERE ID > (SELECT COALESCE(MAX(id), 0) FROM data_feed)
    ORDER BY ID;

    -- Work out what to set our auto_increment start value to
    SELECT CONCAT('ALTER TABLE data_feed AUTO_INCREMENT = ', COALESCE(MAX(id) + 1, 1))
    INTO @alter_table_sql
    FROM data_feed;

    PREPARE alter_table_stmt FROM @alter_table_sql;
    EXECUTE alter_table_stmt;
  END IF;
END//
DELIMITER ;
CALL copy();
DROP PROCEDURE copy;