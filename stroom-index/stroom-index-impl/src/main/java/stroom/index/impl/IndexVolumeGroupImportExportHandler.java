package stroom.index.impl;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docref.HasDocRef;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.importexport.api.NonExplorerDocRefProvider;
import stroom.importexport.shared.ImportSettings;
import stroom.importexport.shared.ImportState;
import stroom.index.shared.IndexVolumeGroup;
import stroom.util.NullSafe;
import stroom.util.logging.LogUtil;
import stroom.util.shared.Message;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexVolumeGroupImportExportHandler
        implements ImportExportActionHandler, NonExplorerDocRefProvider {

    private final IndexVolumeGroupService indexVolumeGroupService;

    @Inject
    public IndexVolumeGroupImportExportHandler(final IndexVolumeGroupService indexVolumeGroupService) {
        this.indexVolumeGroupService = indexVolumeGroupService;
    }

    @Override
    public DocRef importDocument(final DocRef docRef,
                                 final Map<String, byte[]> dataMap,
                                 final ImportState importState,
                                 final ImportSettings importSettings) {
        return null;
    }

    @Override
    public Map<String, byte[]> exportDocument(final DocRef docRef,
                                              final boolean omitAuditFields,
                                              final List<Message> messageList) {
        return null;
    }

    @Override
    public Set<DocRef> listDocuments() {
        return NullSafe.stream(indexVolumeGroupService.getAll())
                .map(HasDocRef::asDocRef)
                .collect(Collectors.toSet());
    }

    @Override
    public String getType() {
        return IndexVolumeGroup.DOCUMENT_TYPE;
    }

    @Override
    public Set<DocRef> findAssociatedNonExplorerDocRefs(final DocRef docRef) {
        return Collections.emptySet();
    }

    @Override
    public DocRef getOwnerDocument(final DocRef docRef, final Map<String, byte[]> dataMap) {
        return null;
    }

    @Override
    public DocRef findNearestExplorerDocRef(final DocRef docref) {
        return null;
    }

    @Override
    public DocRefInfo info(final String uuid) {
        final DocRef docRef = IndexVolumeGroup.buildDocRef()
                .uuid(uuid)
                .build();
        return Optional.ofNullable(indexVolumeGroupService.get(docRef))
                .map(indexVolumeGroup ->
                        DocRefInfo.builder()
                                .docRef(indexVolumeGroup.asDocRef())
                                .createTime(indexVolumeGroup.getCreateTimeMs())
                                .createUser(indexVolumeGroup.getCreateUser())
                                .updateTime(indexVolumeGroup.getUpdateTimeMs())
                                .updateUser(indexVolumeGroup.getUpdateUser())
                                .build())
                .orElseThrow(() -> new IllegalArgumentException(LogUtil.message(
                        "FS Volume Group with UUID {} not found", uuid)));
    }

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        return Collections.emptyMap();
    }

    @Override
    public Set<DocRef> getDependencies(final DocRef docRef) {
        return Collections.emptySet();
    }

    @Override
    public void remapDependencies(final DocRef docRef, final Map<DocRef, DocRef> remappings) {
        // Nothing to remap
    }
}
