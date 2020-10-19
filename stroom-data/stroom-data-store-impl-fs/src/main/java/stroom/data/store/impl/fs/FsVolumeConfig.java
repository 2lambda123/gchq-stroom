package stroom.data.store.impl.fs;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.Lists;

import stroom.util.cache.CacheConfig;
import stroom.util.config.annotations.RequiresRestart;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import javax.inject.Singleton;
import javax.validation.constraints.Pattern;
import java.util.List;

@Singleton
public class FsVolumeConfig extends AbstractConfig {
//    private int resilientReplicationCount = 1;
//    private boolean preferLocalVolumes;
    private String volumeSelector = "RoundRobin";

    private List<String> defaultStreamVolumePaths = Lists.asList("volumes/defaultStreamVolume",new String[]{});
    private double defaultStreamVolumeFilesystemUtilisation = 0.9;
    private boolean createDefaultStreamVolumesOnStart = true;

    private static final String VOLUME_SELECTOR_PATTERN = "^(" +
        RoundRobinVolumeSelector.NAME + "|" +
        MostFreePercentVolumeSelector.NAME + "|" +
        MostFreeVolumeSelector.NAME + "|" +
        RandomVolumeSelector.NAME + "|" +
        RoundRobinIgnoreLeastFreePercentVolumeSelector.NAME + "|" +
        RoundRobinIgnoreLeastFreeVolumeSelector.NAME + "|" +
        RoundRobinVolumeSelector.NAME + "|" +
        WeightedFreePercentRandomVolumeSelector.NAME + "|" +
        WeightedFreeRandomVolumeSelector.NAME + ")$";

    private CacheConfig feedPathCache = new CacheConfig.Builder()
            .maximumSize(1000L)
            .expireAfterAccess(StroomDuration.ofMinutes(10))
            .build();
    private CacheConfig typePathCache = new CacheConfig.Builder()
            .maximumSize(1000L)
            .expireAfterAccess(StroomDuration.ofMinutes(10))
            .build();

//    @JsonPropertyDescription("Set to determine how many volume locations will be used to store a single stream")
//    public int getResilientReplicationCount() {
//        return resilientReplicationCount;
//    }
//
//    public void setResilientReplicationCount(final int resilientReplicationCount) {
//        this.resilientReplicationCount = resilientReplicationCount;
//    }
//
//    @JsonPropertyDescription("Should the stream store always attempt to write to local volumes before writing to " +
//            "remote ones?")
//    public boolean isPreferLocalVolumes() {
//        return preferLocalVolumes;
//    }
//
//    public void setPreferLocalVolumes(final boolean preferLocalVolumes) {
//        this.preferLocalVolumes = preferLocalVolumes;
//    }

    @JsonPropertyDescription("How should volumes be selected for use? Possible volume selectors " +
            "include ('MostFreePercent', 'MostFree', 'Random', 'RoundRobinIgnoreLeastFreePercent', " +
            "'RoundRobinIgnoreLeastFree', 'RoundRobin', 'WeightedFreePercentRandom', 'WeightedFreeRandom') " +
            "default is 'RoundRobin'")
    @Pattern(regexp = VOLUME_SELECTOR_PATTERN)
    public String getVolumeSelector() {
        return volumeSelector;
    }

    public void setVolumeSelector(final String volumeSelector) {
        this.volumeSelector = volumeSelector;
    }

    @RequiresRestart(RequiresRestart.RestartScope.UI)
    @JsonPropertyDescription("If no existing stream volumes are present default volume swill be created on application " +
            "start.  Use property defaultStreamVolumePaths to define the volumes created.")
    public boolean isCreateDefaultStreamVolumesOnStart() {
        return createDefaultStreamVolumesOnStart;
    }

    public void setCreateDefaultStreamVolumesOnStart(final boolean createDefaultStreamVolumesOnStart) {
        this.createDefaultStreamVolumesOnStart = createDefaultStreamVolumesOnStart;
    }

    public CacheConfig getFeedPathCache() {
        return feedPathCache;
    }

    public void setFeedPathCache(final CacheConfig feedPathCache) {
        this.feedPathCache = feedPathCache;
    }

    public CacheConfig getTypePathCache() {
        return typePathCache;
    }

    public void setTypePathCache(final CacheConfig typePathCache) {
        this.typePathCache = typePathCache;
    }

    @JsonPropertyDescription("The paths used if the default stream volumes are created on application start.")
    public List<String >getDefaultStreamVolumePaths() {
        return defaultStreamVolumePaths;
    }

    public void setDefaultStreamVolumePaths(final List<String> defaultStreamVolumePaths) {
        this.defaultStreamVolumePaths = defaultStreamVolumePaths;
    }


    @JsonPropertyDescription("Fraction of the filesystem beyond which the system will stop writing to the " +
            "default stream volumes that may be created on application start.")
    public double getDefaultStreamVolumeFilesystemUtilisation() {
        return defaultStreamVolumeFilesystemUtilisation;
    }

    public void setDefaultStreamVolumeFilesystemUtilisation(final double defaultStreamVolumeFilesystemUtilisation) {
        this.defaultStreamVolumeFilesystemUtilisation = defaultStreamVolumeFilesystemUtilisation;
    }

    @Override
    public String toString() {
        return "VolumeConfig{" +
                "volumeSelector='" + volumeSelector + '\'' +
                ", createDefaultStreamVolumesOnStart=" + createDefaultStreamVolumesOnStart +
                ", defaultStreamVolumePaths=" + "\"" +defaultStreamVolumePaths + "\"" +
                ", defaultStreamVolumeFilesystemUtilisation=" + "\"" +defaultStreamVolumeFilesystemUtilisation + "\"" +
                '}';
    }
}
