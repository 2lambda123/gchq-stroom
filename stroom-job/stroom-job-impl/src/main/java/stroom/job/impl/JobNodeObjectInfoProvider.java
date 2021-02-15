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

import stroom.event.logging.api.ObjectInfoProvider;
import stroom.job.shared.JobNode;

import event.logging.BaseObject;
import event.logging.OtherObject;
import event.logging.OtherObject.Builder;
import event.logging.util.EventLoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JobNodeObjectInfoProvider implements ObjectInfoProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobNodeObjectInfoProvider.class);

    @Override
    public BaseObject createBaseObject(final java.lang.Object obj) {
        final JobNode jobNode = (JobNode) obj;

        final Builder<Void> builder = OtherObject.builder()
                .withType("JobNode")
                .withId(String.valueOf(jobNode.getId()));

        if (jobNode.getJob() != null) {
            builder.withName(jobNode.getJob().getName());
        }

        try {
            builder.addData(EventLoggingUtil.createData("Enabled", String.valueOf(jobNode.isEnabled())));
            builder.addData(EventLoggingUtil.createData("Node Name", jobNode.getNodeName()));
            builder.addData(EventLoggingUtil.createData("Schedule", jobNode.getSchedule()));
        } catch (final RuntimeException e) {
            LOGGER.error("Unable to add unknown but useful data!", e);
        }

        return builder.build();
    }

    @Override
    public String getObjectType(final java.lang.Object object) {
        return object.getClass().getSimpleName();
    }
}
