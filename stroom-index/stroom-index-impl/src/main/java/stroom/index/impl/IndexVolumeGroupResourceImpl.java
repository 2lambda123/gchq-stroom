package stroom.index.impl;

import stroom.entity.shared.ExpressionCriteria;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.index.shared.IndexVolumeGroup;
import stroom.index.shared.IndexVolumeGroupResource;
import stroom.util.shared.ResultPage;

import javax.inject.Inject;

@AutoLogged
class IndexVolumeGroupResourceImpl implements IndexVolumeGroupResource {

    private final IndexVolumeGroupService indexVolumeGroupService;

    @Inject
    IndexVolumeGroupResourceImpl(final IndexVolumeGroupService indexVolumeGroupService) {
        this.indexVolumeGroupService = indexVolumeGroupService;
    }

    @Override
    public ResultPage<IndexVolumeGroup> find(final ExpressionCriteria request) {
        return ResultPage.createUnboundedList(indexVolumeGroupService.getAll());
    }

    @Override
    public IndexVolumeGroup create(final String name) {
        return indexVolumeGroupService.getOrCreate(name);
    }

    @Override
    public IndexVolumeGroup fetch(final Integer id) {
        return indexVolumeGroupService.get(id);
    }

    @Override
    public IndexVolumeGroup update(final Integer id, final IndexVolumeGroup indexVolumeGroup) {
        return indexVolumeGroupService.update(indexVolumeGroup);
    }

    @Override
    public Boolean delete(final Integer id) {
        indexVolumeGroupService.delete(id);
        return true;
    }
}
