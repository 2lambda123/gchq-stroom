-- TODO remove
-- this is now handled in stroom-process/stroom-process-impl-db/src/main/resources/stroom/process/impl/db/migration/V7_0_0_1__processor.sql

--ALTER TABLE STRM_PROC ADD COLUMN PIPE_UUID varchar(255) default NULL;
--UPDATE STRM_PROC sp SET sp.PIPE_UUID = (SELECT p.UUID FROM PIPE p WHERE p.ID = sp.FK_PIPE_ID);
--ALTER TABLE STRM_PROC DROP FOREIGN KEY STRM_PROC_FK_PIPE_ID;
--ALTER TABLE STRM_PROC DROP COLUMN FK_PIPE_ID;