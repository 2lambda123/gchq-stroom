/*
 * This file is generated by jOOQ.
 */
package stroom.index.impl.db.jooq;


import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import stroom.index.impl.db.jooq.tables.IndexShard;
import stroom.index.impl.db.jooq.tables.IndexVolume;
import stroom.index.impl.db.jooq.tables.IndexVolumeGroup;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.index_shard</code>.
     */
    public final IndexShard INDEX_SHARD = IndexShard.INDEX_SHARD;

    /**
     * The table <code>stroom.index_volume</code>.
     */
    public final IndexVolume INDEX_VOLUME = IndexVolume.INDEX_VOLUME;

    /**
     * The table <code>stroom.index_volume_group</code>.
     */
    public final IndexVolumeGroup INDEX_VOLUME_GROUP = IndexVolumeGroup.INDEX_VOLUME_GROUP;

    /**
     * No further instances allowed
     */
    private Stroom() {
        super("stroom", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            IndexShard.INDEX_SHARD,
            IndexVolume.INDEX_VOLUME,
            IndexVolumeGroup.INDEX_VOLUME_GROUP
        );
    }
}
