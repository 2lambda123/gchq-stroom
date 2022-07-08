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

package stroom.job.impl;


import stroom.cluster.api.NodeInfo;
import stroom.job.shared.Job;
import stroom.job.shared.JobNode;
import stroom.test.AbstractCoreIntegrationTest;
import stroom.test.CommonTestControl;
import stroom.util.AuditUtil;

import org.jooq.exception.DataChangedException;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TestJobNodeDao extends AbstractCoreIntegrationTest {

    @Inject
    private JobDao jobDao;
    @Inject
    private JobService jobService;
    @Inject
    private JobNodeDao jobNodeDao;
    @Inject
    private JobNodeService jobNodeService;
    @Inject
    private CommonTestControl commonTestControl;
    @Inject
    private NodeInfo nodeInfo;

    @Test
    void test() {
        Job job = new Job();
        job.setName("Test Job" + System.currentTimeMillis());
        job.setEnabled(true);
        AuditUtil.stamp("test", job);
        job = jobDao.create(job);

        // Test update
        job = jobDao.update(job);
        job = jobDao.update(job);

        // Test optimistic locking
        final Job finalJob = job;
        assertThrows(DataChangedException.class, () -> {
            jobDao.update(finalJob);
            jobDao.update(finalJob);
        });

        // Test that job service can continually update jobs.
        job.setEnabled(false);
        jobService.update(job);
        job.setEnabled(true);
        jobService.update(job);

        JobNode jobNode = new JobNode();
        jobNode.setJob(job);
        jobNode.setNodeName(nodeInfo.getThisNodeName());

        AuditUtil.stamp("test", jobNode);
        jobNode = jobNodeDao.create(jobNode);
        jobNode.setEnabled(true);

        // Test update
        jobNode = jobNodeDao.update(jobNode);
        jobNode = jobNodeDao.update(jobNode);

        // Test optimistic locking
        final JobNode finalJobNode = jobNode;
        assertThrows(DataChangedException.class, () -> {
            jobNodeDao.update(finalJobNode);
            jobNodeDao.update(finalJobNode);
        });

        // Test that job node service can continually update jobs.
        jobNode.setEnabled(false);
        jobNodeService.update(jobNode);
        jobNode.setEnabled(true);
        jobNodeService.update(jobNode);
    }
}
