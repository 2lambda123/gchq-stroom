/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables.records;


import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.proxy.repo.db.jooq.tables.ZipData;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ZipDataRecord extends UpdatableRecordImpl<ZipDataRecord> implements Record4<Long, String, String, Long> {

    private static final long serialVersionUID = 586131554;

    /**
     * Setter for <code>PUBLIC.ZIP_DATA.ID</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>PUBLIC.ZIP_DATA.ID</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>PUBLIC.ZIP_DATA.NAME</code>.
     */
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>PUBLIC.ZIP_DATA.NAME</code>.
     */
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>PUBLIC.ZIP_DATA.FEEDNAME</code>.
     */
    public void setFeedname(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>PUBLIC.ZIP_DATA.FEEDNAME</code>.
     */
    public String getFeedname() {
        return (String) get(2);
    }

    /**
     * Setter for <code>PUBLIC.ZIP_DATA.FK_ZIP_SOURCE_ID</code>.
     */
    public void setFkZipSourceId(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>PUBLIC.ZIP_DATA.FK_ZIP_SOURCE_ID</code>.
     */
    public Long getFkZipSourceId() {
        return (Long) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Long, String, String, Long> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Long, String, String, Long> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return ZipData.ZIP_DATA.ID;
    }

    @Override
    public Field<String> field2() {
        return ZipData.ZIP_DATA.NAME;
    }

    @Override
    public Field<String> field3() {
        return ZipData.ZIP_DATA.FEEDNAME;
    }

    @Override
    public Field<Long> field4() {
        return ZipData.ZIP_DATA.FK_ZIP_SOURCE_ID;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getName();
    }

    @Override
    public String component3() {
        return getFeedname();
    }

    @Override
    public Long component4() {
        return getFkZipSourceId();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getName();
    }

    @Override
    public String value3() {
        return getFeedname();
    }

    @Override
    public Long value4() {
        return getFkZipSourceId();
    }

    @Override
    public ZipDataRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public ZipDataRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public ZipDataRecord value3(String value) {
        setFeedname(value);
        return this;
    }

    @Override
    public ZipDataRecord value4(Long value) {
        setFkZipSourceId(value);
        return this;
    }

    @Override
    public ZipDataRecord values(Long value1, String value2, String value3, Long value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ZipDataRecord
     */
    public ZipDataRecord() {
        super(ZipData.ZIP_DATA);
    }

    /**
     * Create a detached, initialised ZipDataRecord
     */
    public ZipDataRecord(Long id, String name, String feedname, Long fkZipSourceId) {
        super(ZipData.ZIP_DATA);

        set(0, id);
        set(1, name);
        set(2, feedname);
        set(3, fkZipSourceId);
    }
}
