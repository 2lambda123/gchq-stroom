/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.proxy.repo.db.jooq.Indexes;
import stroom.proxy.repo.db.jooq.Keys;
import stroom.proxy.repo.db.jooq.Public;
import stroom.proxy.repo.db.jooq.tables.records.ZipEntryRecord;


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
public class ZipEntry extends TableImpl<ZipEntryRecord> {

    private static final long serialVersionUID = 1341323401;

    /**
     * The reference instance of <code>PUBLIC.ZIP_ENTRY</code>
     */
    public static final ZipEntry ZIP_ENTRY = new ZipEntry();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ZipEntryRecord> getRecordType() {
        return ZipEntryRecord.class;
    }

    /**
     * The column <code>PUBLIC.ZIP_ENTRY.ID</code>.
     */
    public final TableField<ZipEntryRecord, Long> ID = createField(DSL.name("ID"), org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>PUBLIC.ZIP_ENTRY.EXTENSION</code>.
     */
    public final TableField<ZipEntryRecord, String> EXTENSION = createField(DSL.name("EXTENSION"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>PUBLIC.ZIP_ENTRY.BYTE_SIZE</code>.
     */
    public final TableField<ZipEntryRecord, Long> BYTE_SIZE = createField(DSL.name("BYTE_SIZE"), org.jooq.impl.SQLDataType.BIGINT.defaultValue(org.jooq.impl.DSL.field("NULL", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>PUBLIC.ZIP_ENTRY.FK_ZIP_SOURCE_ID</code>.
     */
    public final TableField<ZipEntryRecord, Long> FK_ZIP_SOURCE_ID = createField(DSL.name("FK_ZIP_SOURCE_ID"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>PUBLIC.ZIP_ENTRY.FK_ZIP_DATA_ID</code>.
     */
    public final TableField<ZipEntryRecord, Long> FK_ZIP_DATA_ID = createField(DSL.name("FK_ZIP_DATA_ID"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * Create a <code>PUBLIC.ZIP_ENTRY</code> table reference
     */
    public ZipEntry() {
        this(DSL.name("ZIP_ENTRY"), null);
    }

    /**
     * Create an aliased <code>PUBLIC.ZIP_ENTRY</code> table reference
     */
    public ZipEntry(String alias) {
        this(DSL.name(alias), ZIP_ENTRY);
    }

    /**
     * Create an aliased <code>PUBLIC.ZIP_ENTRY</code> table reference
     */
    public ZipEntry(Name alias) {
        this(alias, ZIP_ENTRY);
    }

    private ZipEntry(Name alias, Table<ZipEntryRecord> aliased) {
        this(alias, aliased, null);
    }

    private ZipEntry(Name alias, Table<ZipEntryRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> ZipEntry(Table<O> child, ForeignKey<O, ZipEntryRecord> key) {
        super(child, key, ZIP_ENTRY);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.PRIMARY_KEY_E, Indexes.ZIP_ENTRY_EXTENSION_INDEX_E, Indexes.ZIP_ENTRY_FK_ZIP_DATA_ID_INDEX_E, Indexes.ZIP_ENTRY_FK_ZIP_SOURCE_ID_INDEX_E);
    }

    @Override
    public Identity<ZipEntryRecord, Long> getIdentity() {
        return Keys.IDENTITY_ZIP_ENTRY;
    }

    @Override
    public UniqueKey<ZipEntryRecord> getPrimaryKey() {
        return Keys.CONSTRAINT_E;
    }

    @Override
    public List<UniqueKey<ZipEntryRecord>> getKeys() {
        return Arrays.<UniqueKey<ZipEntryRecord>>asList(Keys.CONSTRAINT_E, Keys.ZIP_ENTRY_EXTENSION);
    }

    @Override
    public List<ForeignKey<ZipEntryRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<ZipEntryRecord, ?>>asList(Keys.ZIP_ENTRY_FK_ZIP_SOURCE_ID, Keys.ZIP_ENTRY_FK_ZIP_DATA_ID);
    }

    public ZipSource zipSource() {
        return new ZipSource(this, Keys.ZIP_ENTRY_FK_ZIP_SOURCE_ID);
    }

    public ZipData zipData() {
        return new ZipData(this, Keys.ZIP_ENTRY_FK_ZIP_DATA_ID);
    }

    @Override
    public ZipEntry as(String alias) {
        return new ZipEntry(DSL.name(alias), this);
    }

    @Override
    public ZipEntry as(Name alias) {
        return new ZipEntry(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ZipEntry rename(String name) {
        return new ZipEntry(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ZipEntry rename(Name name) {
        return new ZipEntry(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Long, String, Long, Long, Long> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}
