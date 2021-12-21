/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.meta.impl.db.jooq.tables.MetaProcessor;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaProcessorRecord extends UpdatableRecordImpl<MetaProcessorRecord> implements Record3<Integer, String, String> {

    private static final long serialVersionUID = 1501944995;

    /**
     * Setter for <code>stroom.meta_processor.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.meta_processor.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>stroom.meta_processor.processor_uuid</code>.
     */
    public void setProcessorUuid(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.meta_processor.processor_uuid</code>.
     */
    public String getProcessorUuid() {
        return (String) get(1);
    }

    /**
     * Setter for <code>stroom.meta_processor.pipeline_uuid</code>.
     */
    public void setPipelineUuid(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.meta_processor.pipeline_uuid</code>.
     */
    public String getPipelineUuid() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return MetaProcessor.META_PROCESSOR.ID;
    }

    @Override
    public Field<String> field2() {
        return MetaProcessor.META_PROCESSOR.PROCESSOR_UUID;
    }

    @Override
    public Field<String> field3() {
        return MetaProcessor.META_PROCESSOR.PIPELINE_UUID;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getProcessorUuid();
    }

    @Override
    public String component3() {
        return getPipelineUuid();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getProcessorUuid();
    }

    @Override
    public String value3() {
        return getPipelineUuid();
    }

    @Override
    public MetaProcessorRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public MetaProcessorRecord value2(String value) {
        setProcessorUuid(value);
        return this;
    }

    @Override
    public MetaProcessorRecord value3(String value) {
        setPipelineUuid(value);
        return this;
    }

    @Override
    public MetaProcessorRecord values(Integer value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MetaProcessorRecord
     */
    public MetaProcessorRecord() {
        super(MetaProcessor.META_PROCESSOR);
    }

    /**
     * Create a detached, initialised MetaProcessorRecord
     */
    public MetaProcessorRecord(Integer id, String processorUuid, String pipelineUuid) {
        super(MetaProcessor.META_PROCESSOR);

        set(0, id);
        set(1, processorUuid);
        set(2, pipelineUuid);
    }
}
