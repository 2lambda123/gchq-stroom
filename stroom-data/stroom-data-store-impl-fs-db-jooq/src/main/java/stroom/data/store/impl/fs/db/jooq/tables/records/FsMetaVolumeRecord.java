/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables.records;


import stroom.data.store.impl.fs.db.jooq.tables.FsMetaVolume;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;

import javax.annotation.processing.Generated;


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
public class FsMetaVolumeRecord extends UpdatableRecordImpl<FsMetaVolumeRecord> implements Record2<Long, Integer> {

    private static final long serialVersionUID = -1818034751;

    /**
     * Setter for <code>stroom.fs_meta_volume.meta_id</code>.
     */
    public void setMetaId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.fs_meta_volume.meta_id</code>.
     */
    public Long getMetaId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>stroom.fs_meta_volume.fs_volume_id</code>.
     */
    public void setFsVolumeId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.fs_meta_volume.fs_volume_id</code>.
     */
    public Integer getFsVolumeId() {
        return (Integer) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Long, Integer> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Long, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Long, Integer> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return FsMetaVolume.FS_META_VOLUME.META_ID;
    }

    @Override
    public Field<Integer> field2() {
        return FsMetaVolume.FS_META_VOLUME.FS_VOLUME_ID;
    }

    @Override
    public Long component1() {
        return getMetaId();
    }

    @Override
    public Integer component2() {
        return getFsVolumeId();
    }

    @Override
    public Long value1() {
        return getMetaId();
    }

    @Override
    public Integer value2() {
        return getFsVolumeId();
    }

    @Override
    public FsMetaVolumeRecord value1(Long value) {
        setMetaId(value);
        return this;
    }

    @Override
    public FsMetaVolumeRecord value2(Integer value) {
        setFsVolumeId(value);
        return this;
    }

    @Override
    public FsMetaVolumeRecord values(Long value1, Integer value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached FsMetaVolumeRecord
     */
    public FsMetaVolumeRecord() {
        super(FsMetaVolume.FS_META_VOLUME);
    }

    /**
     * Create a detached, initialised FsMetaVolumeRecord
     */
    public FsMetaVolumeRecord(Long metaId, Integer fsVolumeId) {
        super(FsMetaVolume.FS_META_VOLUME);

        set(0, metaId);
        set(1, fsVolumeId);
    }
}
