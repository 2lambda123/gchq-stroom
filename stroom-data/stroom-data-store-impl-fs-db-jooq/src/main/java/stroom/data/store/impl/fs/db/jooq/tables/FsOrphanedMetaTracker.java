/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables;


import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import stroom.data.store.impl.fs.db.jooq.Keys;
import stroom.data.store.impl.fs.db.jooq.Stroom;
import stroom.data.store.impl.fs.db.jooq.tables.records.FsOrphanedMetaTrackerRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class FsOrphanedMetaTracker extends TableImpl<FsOrphanedMetaTrackerRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.fs_orphaned_meta_tracker</code>
     */
    public static final FsOrphanedMetaTracker FS_ORPHANED_META_TRACKER = new FsOrphanedMetaTracker();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FsOrphanedMetaTrackerRecord> getRecordType() {
        return FsOrphanedMetaTrackerRecord.class;
    }

    /**
     * The column <code>stroom.fs_orphaned_meta_tracker.id</code>.
     */
    public final TableField<FsOrphanedMetaTrackerRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.fs_orphaned_meta_tracker.min_meta_id</code>.
     */
    public final TableField<FsOrphanedMetaTrackerRecord, Long> MIN_META_ID = createField(DSL.name("min_meta_id"), SQLDataType.BIGINT.nullable(false).defaultValue(DSL.inline("0", SQLDataType.BIGINT)), this, "");

    private FsOrphanedMetaTracker(Name alias, Table<FsOrphanedMetaTrackerRecord> aliased) {
        this(alias, aliased, null);
    }

    private FsOrphanedMetaTracker(Name alias, Table<FsOrphanedMetaTrackerRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.fs_orphaned_meta_tracker</code> table
     * reference
     */
    public FsOrphanedMetaTracker(String alias) {
        this(DSL.name(alias), FS_ORPHANED_META_TRACKER);
    }

    /**
     * Create an aliased <code>stroom.fs_orphaned_meta_tracker</code> table
     * reference
     */
    public FsOrphanedMetaTracker(Name alias) {
        this(alias, FS_ORPHANED_META_TRACKER);
    }

    /**
     * Create a <code>stroom.fs_orphaned_meta_tracker</code> table reference
     */
    public FsOrphanedMetaTracker() {
        this(DSL.name("fs_orphaned_meta_tracker"), null);
    }

    public <O extends Record> FsOrphanedMetaTracker(Table<O> child, ForeignKey<O, FsOrphanedMetaTrackerRecord> key) {
        super(child, key, FS_ORPHANED_META_TRACKER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public UniqueKey<FsOrphanedMetaTrackerRecord> getPrimaryKey() {
        return Keys.KEY_FS_ORPHANED_META_TRACKER_PRIMARY;
    }

    @Override
    public FsOrphanedMetaTracker as(String alias) {
        return new FsOrphanedMetaTracker(DSL.name(alias), this);
    }

    @Override
    public FsOrphanedMetaTracker as(Name alias) {
        return new FsOrphanedMetaTracker(alias, this);
    }

    @Override
    public FsOrphanedMetaTracker as(Table<?> alias) {
        return new FsOrphanedMetaTracker(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public FsOrphanedMetaTracker rename(String name) {
        return new FsOrphanedMetaTracker(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FsOrphanedMetaTracker rename(Name name) {
        return new FsOrphanedMetaTracker(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public FsOrphanedMetaTracker rename(Table<?> name) {
        return new FsOrphanedMetaTracker(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, Long> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super Long, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super Long, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
