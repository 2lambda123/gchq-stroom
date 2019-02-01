/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.meta.impl.db.Indexes;
import stroom.meta.impl.db.Keys;
import stroom.meta.impl.db.Stroom;
import stroom.meta.impl.db.tables.records.MetaTypeRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaType extends TableImpl<MetaTypeRecord> {

    private static final long serialVersionUID = -1969222786;

    /**
     * The reference instance of <code>stroom.meta_type</code>
     */
    public static final MetaType META_TYPE = new MetaType();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetaTypeRecord> getRecordType() {
        return MetaTypeRecord.class;
    }

    /**
     * The column <code>stroom.meta_type.id</code>.
     */
    public final TableField<MetaTypeRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.meta_type.name</code>.
     */
    public final TableField<MetaTypeRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * Create a <code>stroom.meta_type</code> table reference
     */
    public MetaType() {
        this(DSL.name("meta_type"), null);
    }

    /**
     * Create an aliased <code>stroom.meta_type</code> table reference
     */
    public MetaType(String alias) {
        this(DSL.name(alias), META_TYPE);
    }

    /**
     * Create an aliased <code>stroom.meta_type</code> table reference
     */
    public MetaType(Name alias) {
        this(alias, META_TYPE);
    }

    private MetaType(Name alias, Table<MetaTypeRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetaType(Name alias, Table<MetaTypeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> MetaType(Table<O> child, ForeignKey<O, MetaTypeRecord> key) {
        super(child, key, META_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.META_TYPE_NAME, Indexes.META_TYPE_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<MetaTypeRecord, Integer> getIdentity() {
        return Keys.IDENTITY_META_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<MetaTypeRecord> getPrimaryKey() {
        return Keys.KEY_META_TYPE_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<MetaTypeRecord>> getKeys() {
        return Arrays.<UniqueKey<MetaTypeRecord>>asList(Keys.KEY_META_TYPE_PRIMARY, Keys.KEY_META_TYPE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaType as(String alias) {
        return new MetaType(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaType as(Name alias) {
        return new MetaType(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaType rename(String name) {
        return new MetaType(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaType rename(Name name) {
        return new MetaType(name, null);
    }
}
