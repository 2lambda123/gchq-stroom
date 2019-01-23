/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.volume;

import com.google.common.collect.ImmutableMap;
import event.logging.BaseAdvancedQueryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.entity.CriteriaLoggingUtil;
import stroom.entity.QueryAppender;
import stroom.entity.StroomEntityManager;
import stroom.entity.SystemEntityServiceImpl;
import stroom.entity.event.EntityEvent;
import stroom.entity.event.EntityEventHandler;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.Clearable;
import stroom.entity.shared.EntityAction;
import stroom.entity.shared.Sort.Direction;
import stroom.entity.util.HqlBuilder;
import stroom.node.api.NodeInfo;
import stroom.node.shared.FindVolumeCriteria;
import stroom.node.shared.Node;
import stroom.node.shared.VolumeEntity;
import stroom.node.shared.VolumeEntity.VolumeType;
import stroom.node.shared.VolumeEntity.VolumeUseStatus;
import stroom.node.shared.VolumeState;
import stroom.persist.EntityManagerSupport;
import stroom.security.Security;
import stroom.security.shared.PermissionNames;
import stroom.statistics.internal.InternalStatisticEvent;
import stroom.statistics.internal.InternalStatisticKey;
import stroom.statistics.internal.InternalStatisticsReceiver;
import stroom.util.io.FileUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implementation for the volume API.
 */
@Singleton
@EntityEventHandler(type = VolumeEntity.ENTITY_TYPE, action = {EntityAction.CREATE, EntityAction.DELETE})
public class VolumeServiceImpl extends SystemEntityServiceImpl<VolumeEntity, FindVolumeCriteria>
        implements VolumeService, EntityEvent.Handler, Clearable {

    static final Path DEFAULT_VOLUMES_SUBDIR = Paths.get("volumes");
    static final Path DEFAULT_INDEX_VOLUME_SUBDIR = Paths.get("defaultIndexVolume");
    static final Path DEFAULT_STREAM_VOLUME_SUBDIR = Paths.get("defaultStreamVolume");

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeServiceImpl.class);

    private static final Map<String, VolumeSelector> volumeSelectorMap;

    private static final VolumeSelector DEFAULT_VOLUME_SELECTOR;

    static {
        volumeSelectorMap = new HashMap<>();
        registerVolumeSelector(new MostFreePercentVolumeSelector());
        registerVolumeSelector(new MostFreeVolumeSelector());
        registerVolumeSelector(new RandomVolumeSelector());
        registerVolumeSelector(new RoundRobinIgnoreLeastFreePercentVolumeSelector());
        registerVolumeSelector(new RoundRobinIgnoreLeastFreeVolumeSelector());
        registerVolumeSelector(new RoundRobinVolumeSelector());
        registerVolumeSelector(new WeightedFreePercentRandomVolumeSelector());
        registerVolumeSelector(new WeightedFreeRandomVolumeSelector());
        DEFAULT_VOLUME_SELECTOR = volumeSelectorMap.get(RoundRobinVolumeSelector.NAME);
    }

    private final StroomEntityManager stroomEntityManager;
    private final Security security;
    private final EntityManagerSupport entityManagerSupport;
    private final NodeInfo nodeInfo;
    private final VolumeConfig volumeConfig;
    private final Optional<InternalStatisticsReceiver> optionalInternalStatisticsReceiver;
    private final AtomicReference<List<VolumeEntity>> currentVolumeState = new AtomicReference<>();

    private volatile boolean createdDefaultVolumes;
    private boolean creatingDefaultVolumes;

    @Inject
    VolumeServiceImpl(final StroomEntityManager stroomEntityManager,
                      final Security security,
                      final EntityManagerSupport entityManagerSupport,
                      final NodeInfo nodeInfo,
                      final VolumeConfig volumeConfig,
                      final Optional<InternalStatisticsReceiver> optionalInternalStatisticsReceiver) {
        super(stroomEntityManager, security);
        this.stroomEntityManager = stroomEntityManager;
        this.security = security;
        this.entityManagerSupport = entityManagerSupport;
        this.nodeInfo = nodeInfo;
        this.volumeConfig = volumeConfig;
        this.optionalInternalStatisticsReceiver = optionalInternalStatisticsReceiver;
    }

    private static void registerVolumeSelector(final VolumeSelector volumeSelector) {
        volumeSelectorMap.put(volumeSelector.getName(), volumeSelector);
    }

    @Override
    public Set<VolumeEntity> getStreamVolumeSet(final Node node) {
        return security.insecureResult(() -> {
            LocalVolumeUse localVolumeUse = null;
            if (volumeConfig.isPreferLocalVolumes()) {
                localVolumeUse = LocalVolumeUse.PREFERRED;
            }

            return getVolumeSet(node, VolumeType.PUBLIC, VolumeUseStatus.ACTIVE, null, localVolumeUse, null,
                    getResilientReplicationCount());
        });
    }

    @Override
    public Set<VolumeEntity> getIndexVolumeSet(final Node node, final Set<VolumeEntity> allowedVolumes) {
        return security.insecureResult(() -> getVolumeSet(node, null, null, VolumeUseStatus.ACTIVE, LocalVolumeUse.REQUIRED, allowedVolumes, 1));
    }

    private Set<VolumeEntity> getVolumeSet(final Node node, final VolumeType volumeType, final VolumeUseStatus streamStatus,
                                           final VolumeUseStatus indexStatus, final LocalVolumeUse localVolumeUse, final Set<VolumeEntity> allowedVolumes,
                                           final int requiredNumber) {
        final VolumeSelector volumeSelector = getVolumeSelector();
        final List<VolumeEntity> allVolumeList = getCurrentState();
        final List<VolumeEntity> freeVolumes = VolumeListUtil.removeFullVolumes(allVolumeList);
        Set<VolumeEntity> set = Collections.emptySet();

        final List<VolumeEntity> filteredVolumeList = getFilteredVolumeList(freeVolumes, node, volumeType, streamStatus,
                indexStatus, null, allowedVolumes);
        if (filteredVolumeList.size() > 0) {
            // Create a list of local volumes if we are set to prefer or require
            // local.
            List<VolumeEntity> localVolumeList = null;
            if (localVolumeUse != null) {
                localVolumeList = getFilteredVolumeList(freeVolumes, node, volumeType, streamStatus, indexStatus,
                        Boolean.TRUE, allowedVolumes);

                // If we require a local volume and there are none available
                // then return the empty set.
                if (LocalVolumeUse.REQUIRED.equals(localVolumeUse) && localVolumeList.size() == 0) {
                    return set;
                }
            }

            if (requiredNumber <= 1) {
                // With a replication count of 1 any volume will do.
                if (localVolumeList != null && localVolumeList.size() > 0) {
                    set = Collections.singleton(volumeSelector.select(localVolumeList));
                } else if (filteredVolumeList.size() > 0) {
                    set = Collections.singleton(volumeSelector.select(filteredVolumeList));
                }
            } else {
                set = new HashSet<>();

                final List<VolumeEntity> remaining = new ArrayList<>(filteredVolumeList);
                List<VolumeEntity> remainingInOtherRacks = new ArrayList<>(filteredVolumeList);

                for (int count = 0; count < requiredNumber && remaining.size() > 0; count++) {
                    if (set.size() == 0 && localVolumeList != null && localVolumeList.size() > 0) {
                        // If we are preferring local volumes and this is the
                        // first item then add a local volume here first.
                        final VolumeEntity volume = volumeSelector.select(localVolumeList);

                        remaining.remove(volume);
                        remainingInOtherRacks = VolumeListUtil.removeMatchingRack(remainingInOtherRacks,
                                volume.getNode().getRack());

                        set.add(volume);

                    } else if (remainingInOtherRacks.size() > 0) {
                        // Next try and get volumes in other racks.
                        final VolumeEntity volume = volumeSelector.select(remainingInOtherRacks);

                        remaining.remove(volume);
                        remainingInOtherRacks = VolumeListUtil.removeMatchingRack(remainingInOtherRacks,
                                volume.getNode().getRack());

                        set.add(volume);

                    } else if (remaining.size() > 0) {
                        // Finally add any other volumes to make up the required
                        // replication count.
                        final VolumeEntity volume = volumeSelector.select(remaining);

                        remaining.remove(volume);

                        set.add(volume);
                    }
                }
            }
        }

        if (requiredNumber > set.size()) {
            LOGGER.warn("getVolumeSet - Failed to obtain " + requiredNumber + " volumes as required on node "
                    + nodeInfo.getThisNode().getName() + " (set=" + set + ")");
        }

        return set;
    }

    private List<VolumeEntity> getFilteredVolumeList(final List<VolumeEntity> allVolumes, final Node node,
                                                     final VolumeType volumeType, final VolumeUseStatus streamStatus, final VolumeUseStatus indexStatus,
                                                     final Boolean local, final Set<VolumeEntity> allowedVolumes) {
        final List<VolumeEntity> list = new ArrayList<>();
        for (final VolumeEntity volume : allVolumes) {
            if (allowedVolumes == null || allowedVolumes.contains(volume)) {
                final Node nd = volume.getNode();

                // Check the volume type matches.
                boolean ok = true;
                if (volumeType != null) {
                    ok = volumeType.equals(volume.getVolumeType());
                }

                // Check the stream volume use status matches.
                if (ok) {
                    if (streamStatus != null) {
                        ok = streamStatus.equals(volume.getStreamStatus());
                    }
                }

                // Check the index volume use status matches.
                if (ok) {
                    if (indexStatus != null) {
                        ok = indexStatus.equals(volume.getIndexStatus());
                    }
                }

                // Check the node matches.
                if (ok) {
                    ok = false;
                    if (local == null) {
                        ok = true;
                    } else {
                        if ((Boolean.TRUE.equals(local) && node.equals(nd))
                                || (Boolean.FALSE.equals(local) && !node.equals(nd))) {
                            ok = true;
                        }
                    }
                }

                if (ok) {
                    list.add(volume);
                }
            }
        }
        return list;
    }

    @Override
    public void onChange(final EntityEvent event) {
        currentVolumeState.set(null);
    }

    @Override
    public void clear() {
        currentVolumeState.set(null);
    }

    private List<VolumeEntity> getCurrentState() {
        List<VolumeEntity> state = currentVolumeState.get();
        if (state == null) {
            synchronized (this) {
                state = currentVolumeState.get();
                if (state == null) {
                    state = refresh();
                    currentVolumeState.set(state);
                }
            }
        }
        return state;
    }

    @Override
    public void flush() {
        refresh();
    }

    public List<VolumeEntity> refresh() {
        final Node node = nodeInfo.getThisNode();
        final List<VolumeEntity> newState = new ArrayList<>();

        final FindVolumeCriteria findVolumeCriteria = new FindVolumeCriteria();
        findVolumeCriteria.addSort(FindVolumeCriteria.FIELD_ID, Direction.ASCENDING, false);
        final List<VolumeEntity> volumeList = find(findVolumeCriteria);
        for (final VolumeEntity volume : volumeList) {
            if (volume.getNode().equals(node)) {
                VolumeState volumeState = updateVolumeState(volume);
                volumeState = saveVolumeState(volumeState);
                volume.setVolumeState(volumeState);

                // Record some statistics for the use of this volume.
                recordStats(volume);
            }
            newState.add(volume);
        }

        return newState;
    }

    private void recordStats(final VolumeEntity volume) {
        if (optionalInternalStatisticsReceiver != null) {
            optionalInternalStatisticsReceiver.ifPresent(receiver -> {
                try {
                    final VolumeState volumeState = volume.getVolumeState();

                    final long now = System.currentTimeMillis();
                    final List<InternalStatisticEvent> events = new ArrayList<>();
                    addStatisticEvent(events, now, volume, "Limit", volume.getBytesLimit());
                    addStatisticEvent(events, now, volume, "Used", volumeState.getBytesUsed());
                    addStatisticEvent(events, now, volume, "Free", volumeState.getBytesFree());
                    addStatisticEvent(events, now, volume, "Total", volumeState.getBytesTotal());
                    receiver.putEvents(events);
                } catch (final RuntimeException e) {
                    LOGGER.warn(e.getMessage());
                    LOGGER.debug(e.getMessage(), e);
                }
            });
        }
    }

    private void addStatisticEvent(final List<InternalStatisticEvent> events,
                                   final long timeMs,
                                   final VolumeEntity volume,
                                   final String type,
                                   final Long bytes) {
        if (bytes != null) {
            Map<String, String> tags = ImmutableMap.<String, String>builder()
                    .put("Id", String.valueOf(volume.getId()))
                    .put("Node", volume.getNode().getName())
                    .put("Path", volume.getPath())
                    .put("Type", type)
                    .build();

            InternalStatisticEvent event = InternalStatisticEvent.createValueStat(
                    InternalStatisticKey.VOLUMES, timeMs, tags, bytes.doubleValue());
            events.add(event);
        }
    }

    private VolumeState updateVolumeState(final VolumeEntity volume) {
        final VolumeState volumeState = volume.getVolumeState();
        volumeState.setStatusMs(System.currentTimeMillis());
        final Path path = Paths.get(volume.getPath());

        // Ensure the path exists
        if (Files.isDirectory(path)) {
            LOGGER.debug("updateVolumeState() path exists: " + path);
            setSizes(path, volumeState);
        } else {
            try {
                Files.createDirectories(path);
                LOGGER.debug("updateVolumeState() path created: " + path);
                setSizes(path, volumeState);
            } catch (final IOException e) {
                LOGGER.error("updateVolumeState() path not created: " + path);
            }
        }

        LOGGER.debug("updateVolumeState() exit" + volume);
        return volumeState;
    }

    private void setSizes(final Path path, final VolumeState volumeState) {
        try {
            final FileStore fileStore = Files.getFileStore(path);
            final long usableSpace = fileStore.getUsableSpace();
            final long freeSpace = fileStore.getUnallocatedSpace();
            final long totalSpace = fileStore.getTotalSpace();

            volumeState.setBytesTotal(totalSpace);
            volumeState.setBytesFree(usableSpace);
            volumeState.setBytesUsed(totalSpace - freeSpace);
        } catch (final IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * On creating a new volume create the directory Never create afterwards
     */
    @Override
    public VolumeEntity save(final VolumeEntity entity) {
        return entityManagerSupport.transactionResult(em -> {
            if (!entity.isPersistent()) {
                VolumeState volumeState = entity.getVolumeState();
                if (volumeState == null) {
                    volumeState = new VolumeState();
                }
                // Save initial state
                volumeState = stroomEntityManager.saveEntity(volumeState);
                stroomEntityManager.flush();

                entity.setVolumeState(volumeState);
            }
            return super.save(entity);
        });
    }

    @Override
    public Boolean delete(final VolumeEntity entity) {
        if (Boolean.TRUE.equals(super.delete(entity))) {
            return stroomEntityManager.deleteEntity(entity.getVolumeState());
        }
        return Boolean.FALSE;
    }

    VolumeState saveVolumeState(final VolumeState volumeState) {
        return stroomEntityManager.saveEntity(volumeState);
    }

    @Override
    public Class<VolumeEntity> getEntityClass() {
        return VolumeEntity.class;
    }

    @Override
    public FindVolumeCriteria createCriteria() {
        return new FindVolumeCriteria();
    }

    private int getResilientReplicationCount() {
        int resilientReplicationCount = volumeConfig.getResilientReplicationCount();
        if (resilientReplicationCount < 1) {
            resilientReplicationCount = 1;
        }
        return resilientReplicationCount;
    }


    @Override
    public void appendCriteria(final List<BaseAdvancedQueryItem> items, final FindVolumeCriteria criteria) {
        CriteriaLoggingUtil.appendEntityIdSet(items, "nodeIdSet", criteria.getNodeIdSet());
        CriteriaLoggingUtil.appendCriteriaSet(items, "volumeTypeSet", criteria.getVolumeTypeSet());
        CriteriaLoggingUtil.appendCriteriaSet(items, "streamStatusSet", criteria.getStreamStatusSet());
        CriteriaLoggingUtil.appendCriteriaSet(items, "indexStatusSet", criteria.getIndexStatusSet());
    }

    private VolumeSelector getVolumeSelector() {
        VolumeSelector volumeSelector = null;

        try {
            final String value = volumeConfig.getVolumeSelector();
            if (value != null) {
                volumeSelector = volumeSelectorMap.get(value);
            }
        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage());
        }

        if (volumeSelector == null) {
            volumeSelector = DEFAULT_VOLUME_SELECTOR;
        }

        return volumeSelector;
    }

    @Override
    protected QueryAppender<VolumeEntity, FindVolumeCriteria> createQueryAppender(StroomEntityManager entityManager) {
        return new VolumeQueryAppender(entityManager);
    }

    @Override
    public BaseResultList<VolumeEntity> find(final FindVolumeCriteria criteria) throws RuntimeException {
        ensureDefaultVolumes();
        return super.find(criteria);
    }

//    @StroomStartup(priority = -1100)
//    public void startup() {
//        ensureDefaultVolumes();
//    }

    void ensureDefaultVolumes() {
        if (!createdDefaultVolumes) {
            createDefaultVolumes();
        }
    }

    private synchronized void createDefaultVolumes() {
        if (!createdDefaultVolumes && !creatingDefaultVolumes) {
            creatingDefaultVolumes = true;

            security.insecure(() -> {
                final boolean isEnabled = volumeConfig.isCreateDefaultOnStart();

                if (isEnabled) {
                    final List<VolumeEntity> existingVolumes = getCurrentState();
                    if (existingVolumes.size() == 0) {
                        final Optional<Path> optDefaultVolumePath = getDefaultVolumesPath();
                        if (optDefaultVolumePath.isPresent()) {
                            LOGGER.info("Creating default volumes");
                            final Node node = nodeInfo.getThisNode();
                            final Path indexVolPath = optDefaultVolumePath.get().resolve(DEFAULT_INDEX_VOLUME_SUBDIR);
                            createIndexVolume(indexVolPath, node);
                            final Path streamVolPath = optDefaultVolumePath.get().resolve(DEFAULT_STREAM_VOLUME_SUBDIR);
                            createStreamVolume(streamVolPath, node);
                        } else {
                            LOGGER.warn("No suitable directory to create default volumes in");
                        }
                    } else {
                        LOGGER.info("Existing volumes exist, won't create default volumes");
                    }
                } else {
                    LOGGER.info("Creation of default volumes is currently disabled");
                }

                createdDefaultVolumes = true;
                creatingDefaultVolumes = false;
            });
        }
    }

    private void createIndexVolume(final Path path, final Node node) {
        final VolumeEntity vol = new VolumeEntity();
        vol.setStreamStatus(VolumeUseStatus.INACTIVE);
        vol.setIndexStatus(VolumeUseStatus.ACTIVE);
        vol.setVolumeType(VolumeType.PRIVATE);
        createVolume(path, node, Optional.of(vol));
    }

    private void createStreamVolume(final Path path, final Node node) {
        final VolumeEntity vol = new VolumeEntity();
        vol.setStreamStatus(VolumeUseStatus.ACTIVE);
        vol.setIndexStatus(VolumeUseStatus.INACTIVE);
        vol.setVolumeType(VolumeType.PUBLIC);
        createVolume(path, node, Optional.of(vol));
    }

    private void createVolume(final Path path, final Node node, final Optional<VolumeEntity> optVolume) {
        String pathStr = FileUtil.getCanonicalPath(path);
        try {
            Files.createDirectories(path);
            LOGGER.info("Creating volume in {} on node {}",
                    pathStr,
                    node.getName());
            final VolumeEntity vol = optVolume.orElseGet(VolumeEntity::new);
            vol.setPath(pathStr);
            vol.setNode(node);
            //set an arbitrary default limit size of 250MB on each volume to prevent the
            //filesystem from running out of space, assuming they have 500MB free of course.
            getDefaultVolumeLimit(path).ifPresent(vol::setBytesLimit);
            save(vol);
        } catch (IOException e) {
            LOGGER.error("Unable to create volume due to an error creating directory {}", pathStr, e);
        }
    }

    private OptionalLong getDefaultVolumeLimit(final Path path) {
        try {
            long totalBytes = Files.getFileStore(path).getTotalSpace();
            //set an arbitrary limit of 90% of the filesystem total size to ensure we don't fill up the
            //filesystem.  Limit can be configured from within stroom.
            //Should be noted that although if you have multiple volumes on a filesystem the limit will apply
            //to all volumes and any other data on the filesystem. I.e. once the amount of the filesystem in use
            //is greater than the limit writes to those volumes will be prevented. See Volume.isFull() and
            //this.updateVolumeState()
            return OptionalLong.of((long) (totalBytes * 0.9));
        } catch (IOException e) {
            LOGGER.warn("Unable to determine the total space on the filesystem for path: ", FileUtil.getCanonicalPath(path));
            return OptionalLong.empty();
        }
    }

    private Optional<Path> getDefaultVolumesPath() {
        return Stream.<Supplier<Optional<Path>>>of(
                this::getApplicationJarDir,
                () -> Optional.of(FileUtil.getTempDir()),
                Optional::empty)
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get)
                .flatMap(path -> Optional.of(path.resolve(DEFAULT_VOLUMES_SUBDIR)));
    }

    private Optional<Path> getApplicationJarDir() {
        try {
            String codeSourceLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            if (Pattern.matches(".*/stroom[^/]*.jar$", codeSourceLocation)) {
                return Optional.of(Paths.get(codeSourceLocation).getParent());
            } else {
                return Optional.empty();
            }
        } catch (final RuntimeException e) {
            LOGGER.warn("Unable to determine application jar directory due to: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private enum LocalVolumeUse {
        REQUIRED, PREFERRED
    }

    @Override
    protected String permission() {
        return PermissionNames.MANAGE_VOLUMES_PERMISSION;
    }

    private static class VolumeQueryAppender extends QueryAppender<VolumeEntity, FindVolumeCriteria> {
        VolumeQueryAppender(final StroomEntityManager entityManager) {
            super(entityManager);
        }

        @Override
        public void appendBasicCriteria(HqlBuilder sql, String alias, FindVolumeCriteria criteria) {
            super.appendBasicCriteria(sql, alias, criteria);
            sql.appendEntityIdSetQuery(alias + ".node", criteria.getNodeIdSet());
            sql.appendPrimitiveValueSetQuery(alias + ".pindexStatus", criteria.getIndexStatusSet());
            sql.appendPrimitiveValueSetQuery(alias + ".pstreamStatus", criteria.getStreamStatusSet());
            sql.appendPrimitiveValueSetQuery(alias + ".pvolumeType", criteria.getVolumeTypeSet());
        }
    }

}
