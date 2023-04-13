/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables;


import stroom.meta.impl.db.jooq.Keys;
import stroom.meta.impl.db.jooq.Stroom;
import stroom.meta.impl.db.jooq.tables.records.MetaProcessorRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaProcessor extends TableImpl<MetaProcessorRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.meta_processor</code>
     */
    public static final MetaProcessor META_PROCESSOR = new MetaProcessor();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetaProcessorRecord> getRecordType() {
        return MetaProcessorRecord.class;
    }

    /**
     * The column <code>stroom.meta_processor.id</code>.
     */
    public final TableField<MetaProcessorRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.meta_processor.processor_uuid</code>.
     */
    public final TableField<MetaProcessorRecord, String> PROCESSOR_UUID = createField(DSL.name("processor_uuid"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>stroom.meta_processor.pipeline_uuid</code>.
     */
    public final TableField<MetaProcessorRecord, String> PIPELINE_UUID = createField(DSL.name("pipeline_uuid"), SQLDataType.VARCHAR(255), this, "");

    private MetaProcessor(Name alias, Table<MetaProcessorRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetaProcessor(Name alias, Table<MetaProcessorRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.meta_processor</code> table reference
     */
    public MetaProcessor(String alias) {
        this(DSL.name(alias), META_PROCESSOR);
    }

    /**
     * Create an aliased <code>stroom.meta_processor</code> table reference
     */
    public MetaProcessor(Name alias) {
        this(alias, META_PROCESSOR);
    }

    /**
     * Create a <code>stroom.meta_processor</code> table reference
     */
    public MetaProcessor() {
        this(DSL.name("meta_processor"), null);
    }

    public <O extends Record> MetaProcessor(Table<O> child, ForeignKey<O, MetaProcessorRecord> key) {
        super(child, key, META_PROCESSOR);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<MetaProcessorRecord, Integer> getIdentity() {
        return (Identity<MetaProcessorRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<MetaProcessorRecord> getPrimaryKey() {
        return Keys.KEY_META_PROCESSOR_PRIMARY;
    }

    @Override
    public List<UniqueKey<MetaProcessorRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_META_PROCESSOR_PROCESSOR_UUID, Keys.KEY_META_PROCESSOR_PIPELINE_UUID_IDX);
    }

    @Override
    public MetaProcessor as(String alias) {
        return new MetaProcessor(DSL.name(alias), this);
    }

    @Override
    public MetaProcessor as(Name alias) {
        return new MetaProcessor(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaProcessor rename(String name) {
        return new MetaProcessor(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaProcessor rename(Name name) {
        return new MetaProcessor(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
