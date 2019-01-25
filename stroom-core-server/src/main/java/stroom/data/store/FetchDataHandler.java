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

package stroom.data.store;

import stroom.data.store.api.StreamStore;
import stroom.pipeline.feed.FeedProperties;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.factory.PipelineDataCache;
import stroom.pipeline.factory.PipelineFactory;
import stroom.pipeline.scope.PipelineScopeRunnable;
import stroom.pipeline.shared.AbstractFetchDataResult;
import stroom.pipeline.shared.FetchDataAction;
import stroom.pipeline.state.FeedHolder;
import stroom.pipeline.state.MetaDataHolder;
import stroom.pipeline.state.PipelineHolder;
import stroom.pipeline.state.StreamHolder;
import stroom.security.Security;
import stroom.security.shared.PermissionNames;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;
import javax.inject.Provider;


class FetchDataHandler extends AbstractTaskHandler<FetchDataAction, AbstractFetchDataResult> {
    private final Security security;
    private final DataFetcher dataFetcher;

    @Inject
    FetchDataHandler(final StreamStore streamStore,
                     final FeedProperties feedProperties,
                     final Provider<FeedHolder> feedHolderProvider,
                     final Provider<MetaDataHolder> metaDataHolderProvider,
                     final Provider<PipelineHolder> pipelineHolderProvider,
                     final Provider<StreamHolder> streamHolderProvider,
                     final PipelineStore pipelineStore,
                     final Provider<PipelineFactory> pipelineFactoryProvider,
                     final Provider<ErrorReceiverProxy> errorReceiverProxyProvider,
                     final PipelineDataCache pipelineDataCache,
                     final StreamEventLog streamEventLog,
                     final Security security,
                     final PipelineScopeRunnable pipelineScopeRunnable) {
        dataFetcher = new DataFetcher(streamStore,
                feedProperties,
                feedHolderProvider,
                metaDataHolderProvider,
                pipelineHolderProvider,
                streamHolderProvider,
                pipelineStore,
                pipelineFactoryProvider,
                errorReceiverProxyProvider,
                pipelineDataCache,
                streamEventLog,
                security,
                pipelineScopeRunnable);
        this.security = security;
    }

    @Override
    public AbstractFetchDataResult exec(final FetchDataAction action) {
        return security.secureResult(PermissionNames.VIEW_DATA_PERMISSION, () -> {
            final Long streamId = action.getStreamId();

            if (streamId != null) {
                return dataFetcher.getData(streamId, action.getChildStreamType(), action.getStreamRange(), action.getPageRange(),
                        action.isMarkerMode(), null, action.isShowAsHtml(), action.getExpandedSeverities());
            }

            return null;
        });
    }
}
