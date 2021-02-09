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
 */

package stroom.job.impl;

import stroom.event.logging.api.DocumentEventLog;
import stroom.job.shared.Job;
import stroom.job.shared.JobResource;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.ResultPage;

import event.logging.AdvancedQuery;
import event.logging.And;
import event.logging.Query;

import javax.inject.Inject;
import java.util.function.Consumer;

class JobResourceImpl implements JobResource {
    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(JobResourceImpl.class);

    private final JobService jobService;
    private final DocumentEventLog documentEventLog;

    @Inject
    JobResourceImpl(final JobService jobService,
                    final DocumentEventLog documentEventLog) {
        this.jobService = jobService;
        this.documentEventLog = documentEventLog;
    }

    @Override
    public ResultPage<Job> list() {
        ResultPage<Job> response = null;

        final Query query = Query.builder()
                .withAdvanced(AdvancedQuery.builder()
                        .addAnd(And.builder()
                                .build())
                        .build())
                .build();

        try {
            final FindJobCriteria findJobCriteria = new FindJobCriteria();
            findJobCriteria.setSort(FindJobCriteria.FIELD_ADVANCED);
            findJobCriteria.addSort(FindJobCriteria.FIELD_NAME);

            response = jobService.find(findJobCriteria);
            documentEventLog.search(
                    "ListJobs",
                    query,
                    Job.class.getSimpleName(),
                    response.getPageResponse(),
                    null);
        } catch (final RuntimeException e) {
            documentEventLog.search(
                    "ListJobs",
                    query,
                    Job.class.getSimpleName(),
                    null,
                    e);
            throw e;
        }
        return response;
    }

    @Override
    public void setEnabled(final Integer id, final Boolean enabled) {
        modifyJob(id, job -> job.setEnabled(enabled));
    }

    private void modifyJob(final int id, final Consumer<Job> mutation) {
        Job job = null;
        Job before = null;
        Job after = null;

        try {
            // Get the before version.
            before = jobService.fetch(id).orElse(null);
            job = jobService.fetch(id).orElse(null);
            if (job == null) {
                throw new RuntimeException("Unknown job: " + id);
            }
            mutation.accept(job);
            after = jobService.update(job);

            documentEventLog.update(before, after, null);

        } catch (final RuntimeException e) {
            documentEventLog.update(before, after, e);
            throw e;
        }
    }
}
