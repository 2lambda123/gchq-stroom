/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.cluster.lock.impl.db;

import stroom.cluster.lock.api.ClusterLockService;
import stroom.node.api.NodeInfo;
import stroom.util.logging.LogExecutionTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ClusterLockServiceImpl implements ClusterLockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterLockServiceImpl.class);
    private final ConcurrentHashMap<String, ClusterLockKey> lockMap = new ConcurrentHashMap<>();

    private final ClusterLockHandler clusterLockHandler;
    private final NodeInfo nodeInfo;
    private final DbClusterLock dbClusterLock;

    @Inject
    ClusterLockServiceImpl(final ClusterLockHandler clusterLockHandler,
                           final NodeInfo nodeInfo,
                           final DbClusterLock dbClusterLock) {
        this.clusterLockHandler = clusterLockHandler;
        this.nodeInfo = nodeInfo;
        this.dbClusterLock = dbClusterLock;
    }

    @Override
    public void lock(final String lockName, final Runnable runnable) {
        dbClusterLock.lock(lockName, runnable);
    }

    @Override
    public void tryLock(final String lockName, final Runnable runnable) {
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        LOGGER.debug("tryLock({}) - >>>", lockName);
        boolean success = false;

        // Don't bother the master node if we already hold the lock.
        ClusterLockKey clusterLockKey = lockMap.get(lockName);
        if (clusterLockKey == null) {
            clusterLockKey = new ClusterLockKey(lockName, nodeInfo.getThisNodeName(),
                    System.currentTimeMillis());
            final Boolean didLock = clusterLockHandler.tryLock(clusterLockKey);
            if (didLock != null) {
                success = Boolean.TRUE.equals(didLock);
            }

            if (success) {
                // We managed to acquire this cluster lock so add it to the lock
                // set for the purposes of keep alive.
                lockMap.put(lockName, clusterLockKey);
            }
        }

        LOGGER.debug("tryLock({}) - <<< {}", lockName, success);

        if (success) {
            try {
                runnable.run();
            } finally {
                releaseLock(lockName);
            }
        } else {
            LOGGER.info("Skipped process as did not get lock {} in {}", lockName, logExecutionTime);
        }
    }

    private void releaseLock(final String lockName) {
        LOGGER.debug("releaseLock({}) - >>>", lockName);
        // Remove the lock name from the lock map.
        final ClusterLockKey clusterLockKey = lockMap.remove(lockName);

        boolean success = false;
        if (clusterLockKey == null) {
            LOGGER.error("releaseLock({}) - Lock not found", lockName);
        } else {
            final Boolean result = clusterLockHandler.releaseLock(clusterLockKey);
            if (result != null) {
                success = result;
            }
        }

        LOGGER.debug("releaseLock({}) - <<< {}", lockName, success);
    }

    void keepAlive() {
        LOGGER.debug("keepAlive() - >>>");

        for (final Entry<String, ClusterLockKey> entry : lockMap.entrySet()) {
            final String lockName = entry.getKey();
            final ClusterLockKey clusterLockKey = entry.getValue();

            LOGGER.debug("keepAlive({}) - >>>", lockName);
            final Boolean success = clusterLockHandler.keepLockAlive(clusterLockKey);
            LOGGER.debug("keepAlive({}) - <<< {}", lockName, success);

            // We should only receive FALSE if the master node knows nothing
            // about the lock we are trying to keep alive. This should only ever
            // be the case if we have told master to release the lock, so double
            // check that we expected to keep this lock alive as the map might
            // have coincidentally had the lock removed in the releaseLock()
            // method.
            if (Boolean.FALSE.equals(success)) {
                final ClusterLockKey currentKey = lockMap.get(lockName);
                if (currentKey != null && currentKey.equals(clusterLockKey)) {
                    LOGGER.error("keepAlive() - Lock no longer exists - {}", clusterLockKey);
                }
            }
        }

        LOGGER.debug("keepAlive() - <<<");
    }
}
