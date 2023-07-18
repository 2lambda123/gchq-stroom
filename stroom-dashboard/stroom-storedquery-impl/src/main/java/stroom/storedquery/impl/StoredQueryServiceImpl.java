package stroom.storedquery.impl;

import stroom.dashboard.shared.FindStoredQueryCriteria;
import stroom.dashboard.shared.StoredQuery;
import stroom.security.api.SecurityContext;
import stroom.storedquery.api.StoredQueryService;
import stroom.util.AuditUtil;
import stroom.util.shared.PermissionException;
import stroom.util.shared.ResultPage;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

public class StoredQueryServiceImpl implements StoredQueryService {

    private final SecurityContext securityContext;
    private final StoredQueryDao dao;

    @Inject
    public StoredQueryServiceImpl(final SecurityContext securityContext, final StoredQueryDao dao) {
        this.securityContext = securityContext;
        this.dao = dao;
    }

    @Override
    public StoredQuery create(@NotNull final StoredQuery storedQuery) {
        AuditUtil.stamp(securityContext, storedQuery);
        return securityContext.secureResult(() -> dao.create(storedQuery));
    }

    @Override
    public StoredQuery update(@NotNull final StoredQuery storedQuery) {
        AuditUtil.stamp(securityContext, storedQuery);
        return securityContext.secureResult(() -> dao.update(storedQuery));
    }

    @Override
    public boolean delete(int id) {
        return securityContext.secureResult(() -> dao.delete(id));
    }

    @Override
    public StoredQuery fetch(int id) {
        final StoredQuery storedQuery = securityContext.secureResult(() ->
                dao.fetch(id)).orElse(null);

        if (storedQuery != null
                && !storedQuery.getUpdateUser().equals(securityContext.getSubjectId())) {
            throw new PermissionException(securityContext.getUserIdentityForAudit(),
                    "This retrieved stored query belongs to another user");
        }
        return storedQuery;
    }

    @Override
    public ResultPage<StoredQuery> find(FindStoredQueryCriteria criteria) {
        final String userId = securityContext.getSubjectId();
        criteria.setUserId(userId);

        return securityContext.secureResult(() -> dao.find(criteria));
    }
}
