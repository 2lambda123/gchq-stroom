/*
 * This file is generated by jOOQ.
 */
package stroom.analytics.impl.db.jooq.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.analytics.impl.db.jooq.tables.AnalyticProcessorFilterTracker;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AnalyticProcessorFilterTrackerRecord extends UpdatableRecordImpl<AnalyticProcessorFilterTrackerRecord> implements Record9<String, Long, Integer, Long, Long, Long, Long, Long, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.fk_analytic_processor_filter_uuid</code>.
     */
    public void setFkAnalyticProcessorFilterUuid(String value) {
        set(0, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.fk_analytic_processor_filter_uuid</code>.
     */
    public String getFkAnalyticProcessorFilterUuid() {
        return (String) get(0);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.last_poll_ms</code>.
     */
    public void setLastPollMs(Long value) {
        set(1, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.last_poll_ms</code>.
     */
    public Long getLastPollMs() {
        return (Long) get(1);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.last_poll_task_count</code>.
     */
    public void setLastPollTaskCount(Integer value) {
        set(2, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.last_poll_task_count</code>.
     */
    public Integer getLastPollTaskCount() {
        return (Integer) get(2);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.last_meta_id</code>.
     */
    public void setLastMetaId(Long value) {
        set(3, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.last_meta_id</code>.
     */
    public Long getLastMetaId() {
        return (Long) get(3);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.last_event_id</code>.
     */
    public void setLastEventId(Long value) {
        set(4, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.last_event_id</code>.
     */
    public Long getLastEventId() {
        return (Long) get(4);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.last_event_time</code>.
     */
    public void setLastEventTime(Long value) {
        set(5, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.last_event_time</code>.
     */
    public Long getLastEventTime() {
        return (Long) get(5);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.meta_count</code>.
     */
    public void setMetaCount(Long value) {
        set(6, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.meta_count</code>.
     */
    public Long getMetaCount() {
        return (Long) get(6);
    }

    /**
     * Setter for
     * <code>stroom.analytic_processor_filter_tracker.event_count</code>.
     */
    public void setEventCount(Long value) {
        set(7, value);
    }

    /**
     * Getter for
     * <code>stroom.analytic_processor_filter_tracker.event_count</code>.
     */
    public Long getEventCount() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>stroom.analytic_processor_filter_tracker.message</code>.
     */
    public void setMessage(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.analytic_processor_filter_tracker.message</code>.
     */
    public String getMessage() {
        return (String) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<String, Long, Integer, Long, Long, Long, Long, Long, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<String, Long, Integer, Long, Long, Long, Long, Long, String> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.FK_ANALYTIC_PROCESSOR_FILTER_UUID;
    }

    @Override
    public Field<Long> field2() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.LAST_POLL_MS;
    }

    @Override
    public Field<Integer> field3() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.LAST_POLL_TASK_COUNT;
    }

    @Override
    public Field<Long> field4() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.LAST_META_ID;
    }

    @Override
    public Field<Long> field5() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.LAST_EVENT_ID;
    }

    @Override
    public Field<Long> field6() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.LAST_EVENT_TIME;
    }

    @Override
    public Field<Long> field7() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.META_COUNT;
    }

    @Override
    public Field<Long> field8() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.EVENT_COUNT;
    }

    @Override
    public Field<String> field9() {
        return AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER.MESSAGE;
    }

    @Override
    public String component1() {
        return getFkAnalyticProcessorFilterUuid();
    }

    @Override
    public Long component2() {
        return getLastPollMs();
    }

    @Override
    public Integer component3() {
        return getLastPollTaskCount();
    }

    @Override
    public Long component4() {
        return getLastMetaId();
    }

    @Override
    public Long component5() {
        return getLastEventId();
    }

    @Override
    public Long component6() {
        return getLastEventTime();
    }

    @Override
    public Long component7() {
        return getMetaCount();
    }

    @Override
    public Long component8() {
        return getEventCount();
    }

    @Override
    public String component9() {
        return getMessage();
    }

    @Override
    public String value1() {
        return getFkAnalyticProcessorFilterUuid();
    }

    @Override
    public Long value2() {
        return getLastPollMs();
    }

    @Override
    public Integer value3() {
        return getLastPollTaskCount();
    }

    @Override
    public Long value4() {
        return getLastMetaId();
    }

    @Override
    public Long value5() {
        return getLastEventId();
    }

    @Override
    public Long value6() {
        return getLastEventTime();
    }

    @Override
    public Long value7() {
        return getMetaCount();
    }

    @Override
    public Long value8() {
        return getEventCount();
    }

    @Override
    public String value9() {
        return getMessage();
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value1(String value) {
        setFkAnalyticProcessorFilterUuid(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value2(Long value) {
        setLastPollMs(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value3(Integer value) {
        setLastPollTaskCount(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value4(Long value) {
        setLastMetaId(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value5(Long value) {
        setLastEventId(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value6(Long value) {
        setLastEventTime(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value7(Long value) {
        setMetaCount(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value8(Long value) {
        setEventCount(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord value9(String value) {
        setMessage(value);
        return this;
    }

    @Override
    public AnalyticProcessorFilterTrackerRecord values(String value1, Long value2, Integer value3, Long value4, Long value5, Long value6, Long value7, Long value8, String value9) {
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
     * Create a detached AnalyticProcessorFilterTrackerRecord
     */
    public AnalyticProcessorFilterTrackerRecord() {
        super(AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER);
    }

    /**
     * Create a detached, initialised AnalyticProcessorFilterTrackerRecord
     */
    public AnalyticProcessorFilterTrackerRecord(String fkAnalyticProcessorFilterUuid, Long lastPollMs, Integer lastPollTaskCount, Long lastMetaId, Long lastEventId, Long lastEventTime, Long metaCount, Long eventCount, String message) {
        super(AnalyticProcessorFilterTracker.ANALYTIC_PROCESSOR_FILTER_TRACKER);

        setFkAnalyticProcessorFilterUuid(fkAnalyticProcessorFilterUuid);
        setLastPollMs(lastPollMs);
        setLastPollTaskCount(lastPollTaskCount);
        setLastMetaId(lastMetaId);
        setLastEventId(lastEventId);
        setLastEventTime(lastEventTime);
        setMetaCount(metaCount);
        setEventCount(eventCount);
        setMessage(message);
    }
}
