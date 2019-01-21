/*
 * Copyright 2018 Crown Copyright
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

package stroom.policy;

import com.google.inject.AbstractModule;
import stroom.ruleset.shared.FetchDataRetentionPolicyAction;
import stroom.ruleset.shared.SaveDataRetentionPolicyAction;
import stroom.task.api.TaskHandlerBinder;

public class PolicyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PolicyService.class).to(PolicyServiceImpl.class);

        TaskHandlerBinder.create(binder())
                .bind(FetchDataRetentionPolicyAction.class, stroom.policy.FetchDataRetentionPolicyHandler.class)
                .bind(SaveDataRetentionPolicyAction.class, stroom.policy.SaveDataRetentionPolicyHandler.class);
    }
}