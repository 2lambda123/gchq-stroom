/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq;


import stroom.meta.impl.db.jooq.tables.Meta;
import stroom.meta.impl.db.jooq.tables.MetaVal;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in stroom.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index META_META_CREATE_TIME = Internal.createIndex(DSL.name("meta_create_time"), Meta.META, new OrderField[] { Meta.META.CREATE_TIME }, false);
    public static final Index META_META_FEED_ID_CREATE_TIME = Internal.createIndex(DSL.name("meta_feed_id_create_time"), Meta.META, new OrderField[] { Meta.META.FEED_ID, Meta.META.CREATE_TIME }, false);
    public static final Index META_META_FEED_ID_EFFECTIVE_TIME = Internal.createIndex(DSL.name("meta_feed_id_effective_time"), Meta.META, new OrderField[] { Meta.META.FEED_ID, Meta.META.EFFECTIVE_TIME }, false);
    public static final Index META_META_PARENT_ID = Internal.createIndex(DSL.name("meta_parent_id"), Meta.META, new OrderField[] { Meta.META.PARENT_ID }, false);
    public static final Index META_META_PROCESSOR_ID_CREATE_TIME = Internal.createIndex(DSL.name("meta_processor_id_create_time"), Meta.META, new OrderField[] { Meta.META.PROCESSOR_ID, Meta.META.CREATE_TIME }, false);
    public static final Index META_META_STATUS = Internal.createIndex(DSL.name("meta_status"), Meta.META, new OrderField[] { Meta.META.STATUS }, false);
    public static final Index META_VAL_META_VAL_CREATE_TIME = Internal.createIndex(DSL.name("meta_val_create_time"), MetaVal.META_VAL, new OrderField[] { MetaVal.META_VAL.CREATE_TIME }, false);
    public static final Index META_VAL_META_VAL_META_ID = Internal.createIndex(DSL.name("meta_val_meta_id"), MetaVal.META_VAL, new OrderField[] { MetaVal.META_VAL.META_ID }, false);
}
