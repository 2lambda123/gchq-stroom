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

package stroom.processor.impl.db.task;

import stroom.processor.impl.db.tables.ProcessorFilter;
import stroom.processor.shared.task.CreateProcessorAction;
import stroom.security.Security;
import stroom.security.shared.PermissionNames;
import stroom.processor.StreamProcessorFilterService;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


public class CreateProcessorHandler extends AbstractTaskHandler<CreateProcessorAction, ProcessorFilter> {
    private final StreamProcessorFilterService streamProcessorFilterService;
    private final Security security;

    @Inject
    CreateProcessorHandler(final StreamProcessorFilterService streamProcessorFilterService,
                           final Security security) {
        this.streamProcessorFilterService = streamProcessorFilterService;
        this.security = security;
    }

    @Override
    public ProcessorFilter exec(final CreateProcessorAction action) {
        return security.secureResult(PermissionNames.MANAGE_PROCESSORS_PERMISSION, () ->
                streamProcessorFilterService.createFilter(action.getPipeline(), action.getQueryData(),
                        action.isEnabled(), action.getPriority()));
    }
}
