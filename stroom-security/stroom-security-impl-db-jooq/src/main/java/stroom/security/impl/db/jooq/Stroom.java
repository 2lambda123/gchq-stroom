/*
 * This file is generated by jOOQ.
 */
package stroom.security.impl.db.jooq;


import stroom.security.impl.db.jooq.tables.ApiKey;
import stroom.security.impl.db.jooq.tables.AppPermission;
import stroom.security.impl.db.jooq.tables.DocPermission;
import stroom.security.impl.db.jooq.tables.StroomUser;
import stroom.security.impl.db.jooq.tables.StroomUserGroup;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.api_key</code>.
     */
    public final ApiKey API_KEY = ApiKey.API_KEY;

    /**
     * The table <code>stroom.app_permission</code>.
     */
    public final AppPermission APP_PERMISSION = AppPermission.APP_PERMISSION;

    /**
     * The table <code>stroom.doc_permission</code>.
     */
    public final DocPermission DOC_PERMISSION = DocPermission.DOC_PERMISSION;

    /**
     * The table <code>stroom.stroom_user</code>.
     */
    public final StroomUser STROOM_USER = StroomUser.STROOM_USER;

    /**
     * The table <code>stroom.stroom_user_group</code>.
     */
    public final StroomUserGroup STROOM_USER_GROUP = StroomUserGroup.STROOM_USER_GROUP;

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
            ApiKey.API_KEY,
            AppPermission.APP_PERMISSION,
            DocPermission.DOC_PERMISSION,
            StroomUser.STROOM_USER,
            StroomUserGroup.STROOM_USER_GROUP
        );
    }
}
