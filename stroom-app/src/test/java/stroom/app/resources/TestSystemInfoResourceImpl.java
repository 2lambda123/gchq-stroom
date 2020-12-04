package stroom.app.resources;

import stroom.core.sysinfo.SystemInfoResource;
import stroom.core.sysinfo.SystemInfoResourceImpl;
import stroom.core.sysinfo.SystemInfoService;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.test.common.util.test.AbstractResourceTest;
import stroom.util.sysinfo.HasSystemInfo;
import stroom.util.sysinfo.SystemInfoResult;
import stroom.util.sysinfo.SystemInfoResultList;

import event.logging.Event;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;


class TestSystemInfoResourceImpl extends AbstractResourceTest<SystemInfoResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSystemInfoResourceImpl.class);

    @Mock
    private SystemInfoService systemInfoService;
    @Mock
    private StroomEventLoggingService stroomEventLoggingService;

    @BeforeEach
    void setUp() {
        LOGGER.info("Setup");

        when(stroomEventLoggingService.createEvent())
                .thenReturn(new Event());

        doAnswer(invocation -> {
            LOGGER.info("log() called for {}", invocation.getArguments()[0]);
            return null;
        })
                .when(stroomEventLoggingService).log(Mockito.any());
    }

    @Test
    void getAll() {

        final HasSystemInfo systemInfoSupplier1 = getSystemInfoSupplier(buildSystemInfoResult("name1"));
        final HasSystemInfo systemInfoSupplier2 = getSystemInfoSupplier(buildSystemInfoResult("name2"));

        final SystemInfoResultList expectedResults = SystemInfoResultList.of(
                systemInfoSupplier1.getSystemInfo(),
                systemInfoSupplier2.getSystemInfo());

        when(systemInfoService.getAll())
                .thenAnswer(invocation -> {
                    LOGGER.info("SystemInfoService.getAll() mock called");

                    return List.of(
                            systemInfoSupplier1.getSystemInfo(),
                            systemInfoSupplier2.getSystemInfo());
                });

        final SystemInfoResultList result = doGetTest("/", SystemInfoResultList.class, expectedResults);
    }

    private SystemInfoResult buildSystemInfoResult(final String name) {
        return SystemInfoResult.builder()
                .name(name)
                .addDetail("key1", "value1")
                .addDetail("key2", "value2")
                .build();
    }

    @NotNull
    private HasSystemInfo getSystemInfoSupplier(final SystemInfoResult systemInfoResult) {

        return new HasSystemInfo() {
            @Override
            public String getSystemInfoName() {
                return systemInfoResult.getName();
            }

            @Override
            public SystemInfoResult getSystemInfo() {
                return systemInfoResult;
            }
        };
    }

    @Override
    public SystemInfoResource getRestResource() {
        return new SystemInfoResourceImpl(systemInfoService, stroomEventLoggingService);
    }

    @Override
    public String getResourceBasePath() {
        return SystemInfoResource.BASE_PATH;
    }
}