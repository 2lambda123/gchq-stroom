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

package stroom.entity.cluster;

import com.google.inject.AbstractModule;
import stroom.task.api.TaskHandlerBinder;

public class EntityClusterModule extends AbstractModule {
    @Override
    protected void configure() {
        TaskHandlerBinder.create(binder())
                .bind(ClearServiceClusterTask.class, ClearServiceClusterHandler.class)
                .bind(FindServiceClusterTask.class, FindServiceClusterHandler.class)
                .bind(FlushServiceClusterTask.class, FlushServiceClusterHandler.class);
    }
    //    @Bean
//    @Scope(value = StroomScope.TASK)
//    public ClearServiceClusterHandler clearServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new ClearServiceClusterHandler(stroomBeanStore);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public FindClearServiceClusterHandler findClearServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new FindClearServiceClusterHandler(stroomBeanStore);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public FindCloseServiceClusterHandler findCloseServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new FindCloseServiceClusterHandler(stroomBeanStore);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public FindDeleteServiceClusterHandler findDeleteServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new FindDeleteServiceClusterHandler(stroomBeanStore);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public FindFlushServiceClusterHandler findFlushServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new FindFlushServiceClusterHandler(stroomBeanStore);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public FindServiceClusterHandler findServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new FindServiceClusterHandler(stroomBeanStore);
//    }
//
//    @Bean
//    @Scope(value = StroomScope.TASK)
//    public FlushServiceClusterHandler flushServiceClusterHandler(final StroomBeanStore stroomBeanStore) {
//        return new FlushServiceClusterHandler(stroomBeanStore);
//    }
}