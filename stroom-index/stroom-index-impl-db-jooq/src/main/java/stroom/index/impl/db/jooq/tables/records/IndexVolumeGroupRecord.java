/*
 * This file is generated by jOOQ.
 */
package stroom.index.impl.db.jooq.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.index.impl.db.jooq.tables.IndexVolumeGroup;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class IndexVolumeGroupRecord extends UpdatableRecordImpl<IndexVolumeGroupRecord> implements Record9<Integer, Integer, Long, String, Long, String, String, Boolean, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>stroom.index_volume_group.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>stroom.index_volume_group.version</code>.
     */
    public void setVersion(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.version</code>.
     */
    public Integer getVersion() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>stroom.index_volume_group.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>stroom.index_volume_group.create_user</code>.
     */
    public void setCreateUser(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.create_user</code>.
     */
    public String getCreateUser() {
        return (String) get(3);
    }

    /**
     * Setter for <code>stroom.index_volume_group.update_time_ms</code>.
     */
    public void setUpdateTimeMs(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.update_time_ms</code>.
     */
    public Long getUpdateTimeMs() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>stroom.index_volume_group.update_user</code>.
     */
    public void setUpdateUser(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.update_user</code>.
     */
    public String getUpdateUser() {
        return (String) get(5);
    }

    /**
     * Setter for <code>stroom.index_volume_group.name</code>.
     */
    public void setName(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.name</code>.
     */
    public String getName() {
        return (String) get(6);
    }

    /**
     * Setter for <code>stroom.index_volume_group.is_default</code>.
     */
    public void setIsDefault(Boolean value) {
        set(7, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.is_default</code>.
     */
    public Boolean getIsDefault() {
        return (Boolean) get(7);
    }

    /**
     * Setter for <code>stroom.index_volume_group.uuid</code>.
     */
    public void setUuid(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.index_volume_group.uuid</code>.
     */
    public String getUuid() {
        return (String) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, Integer, Long, String, Long, String, String, Boolean, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<Integer, Integer, Long, String, Long, String, String, Boolean, String> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.ID;
    }

    @Override
    public Field<Integer> field2() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.VERSION;
    }

    @Override
    public Field<Long> field3() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.CREATE_TIME_MS;
    }

    @Override
    public Field<String> field4() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.CREATE_USER;
    }

    @Override
    public Field<Long> field5() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.UPDATE_TIME_MS;
    }

    @Override
    public Field<String> field6() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.UPDATE_USER;
    }

    @Override
    public Field<String> field7() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.NAME;
    }

    @Override
    public Field<Boolean> field8() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.IS_DEFAULT;
    }

    @Override
    public Field<String> field9() {
        return IndexVolumeGroup.INDEX_VOLUME_GROUP.UUID;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getVersion();
    }

    @Override
    public Long component3() {
        return getCreateTimeMs();
    }

    @Override
    public String component4() {
        return getCreateUser();
    }

    @Override
    public Long component5() {
        return getUpdateTimeMs();
    }

    @Override
    public String component6() {
        return getUpdateUser();
    }

    @Override
    public String component7() {
        return getName();
    }

    @Override
    public Boolean component8() {
        return getIsDefault();
    }

    @Override
    public String component9() {
        return getUuid();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getVersion();
    }

    @Override
    public Long value3() {
        return getCreateTimeMs();
    }

    @Override
    public String value4() {
        return getCreateUser();
    }

    @Override
    public Long value5() {
        return getUpdateTimeMs();
    }

    @Override
    public String value6() {
        return getUpdateUser();
    }

    @Override
    public String value7() {
        return getName();
    }

    @Override
    public Boolean value8() {
        return getIsDefault();
    }

    @Override
    public String value9() {
        return getUuid();
    }

    @Override
    public IndexVolumeGroupRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value2(Integer value) {
        setVersion(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value3(Long value) {
        setCreateTimeMs(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value4(String value) {
        setCreateUser(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value5(Long value) {
        setUpdateTimeMs(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value6(String value) {
        setUpdateUser(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value7(String value) {
        setName(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value8(Boolean value) {
        setIsDefault(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord value9(String value) {
        setUuid(value);
        return this;
    }

    @Override
    public IndexVolumeGroupRecord values(Integer value1, Integer value2, Long value3, String value4, Long value5, String value6, String value7, Boolean value8, String value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached IndexVolumeGroupRecord
     */
    public IndexVolumeGroupRecord() {
        super(IndexVolumeGroup.INDEX_VOLUME_GROUP);
    }

    /**
     * Create a detached, initialised IndexVolumeGroupRecord
     */
    public IndexVolumeGroupRecord(Integer id, Integer version, Long createTimeMs, String createUser, Long updateTimeMs, String updateUser, String name, Boolean isDefault, String uuid) {
        super(IndexVolumeGroup.INDEX_VOLUME_GROUP);

        setId(id);
        setVersion(version);
        setCreateTimeMs(createTimeMs);
        setCreateUser(createUser);
        setUpdateTimeMs(updateTimeMs);
        setUpdateUser(updateUser);
        setName(name);
        setIsDefault(isDefault);
        setUuid(uuid);
        resetChangedOnNotNull();
    }
}
