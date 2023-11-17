package stroom.meta.impl;

import stroom.meta.api.PhysicalDelete;

import jakarta.inject.Inject;

import java.util.Collection;

class PhysicalDeleteImpl implements PhysicalDelete {

    private final MetaDao metaDao;
    private final MetaValueDao metaValueDao;

    @Inject
    PhysicalDeleteImpl(final MetaDao metaDao, final MetaValueDao metaValueDao) {
        this.metaDao = metaDao;
        this.metaValueDao = metaValueDao;
    }

    @Override
    public void cleanup(final Collection<Long> metaIds) {
        // Delete meta attributes.
        metaValueDao.delete(metaIds);

        // Delete meta data.
        metaDao.delete(metaIds);
    }
}
