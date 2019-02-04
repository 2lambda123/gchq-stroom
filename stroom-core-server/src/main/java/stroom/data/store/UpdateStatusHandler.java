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

package stroom.data.store;

import stroom.meta.shared.MetaService;
import stroom.security.Security;
import stroom.streamstore.shared.UpdateStatusAction;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.SharedInteger;

import javax.inject.Inject;


class UpdateStatusHandler extends AbstractTaskHandler<UpdateStatusAction, SharedInteger> {
    private final MetaService metaService;
    private final Security security;

    @Inject
    UpdateStatusHandler(final MetaService metaService,
                        final Security security) {
        this.metaService = metaService;
        this.security = security;
    }

    @Override
    public SharedInteger exec(final UpdateStatusAction task) {
        return security.secureResult(() -> new SharedInteger(metaService.updateStatus(task.getCriteria(), task.getNewStatus())));
    }
}
