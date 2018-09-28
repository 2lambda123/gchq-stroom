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

package stroom.pipeline.server.writer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.feed.server.FeedService;
import stroom.feed.shared.Feed;
import stroom.node.server.NodeCache;
import stroom.pipeline.destination.RollingDestination;
import stroom.pipeline.destination.RollingDestinationFactory;
import stroom.pipeline.destination.RollingStreamDestination;
import stroom.pipeline.destination.StreamKey;
import stroom.pipeline.server.errorhandler.ProcessException;
import stroom.pipeline.server.factory.ConfigurableElement;
import stroom.pipeline.server.factory.PipelineProperty;
import stroom.pipeline.server.factory.PipelinePropertyDocRef;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.state.StreamHolder;
import stroom.query.api.v2.DocRef;
import stroom.streamstore.server.StreamStore;
import stroom.streamstore.server.StreamTarget;
import stroom.streamstore.server.StreamTypeService;
import stroom.streamstore.shared.Stream;
import stroom.streamstore.shared.StreamType;
import stroom.util.spring.StroomScope;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Joins text instances into a single text instance.
 */
@Component
@Scope(StroomScope.PROTOTYPE)
@ConfigurableElement(type = "RollingStreamAppender", category = Category.DESTINATION, roles = {
        PipelineElementType.ROLE_TARGET, PipelineElementType.ROLE_DESTINATION,
        PipelineElementType.VISABILITY_STEPPING}, icon = ElementIcons.STREAM)
public class RollingStreamAppender extends AbstractRollingAppender implements RollingDestinationFactory {
    private static final int MB = 1024 * 1024;
    private static final int DEFAULT_ROLL_SIZE = 100 * MB;

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;

    private final StreamStore streamStore;
    private final StreamHolder streamHolder;
    private final FeedService feedService;
    private final StreamTypeService streamTypeService;
    private final NodeCache nodeCache;

    private DocRef feedRef;
    private Feed feed;
    private String streamType;
    private boolean segmentOutput = true;
    private Long frequency = HOUR;
    private SimpleCron schedule;
    private long rollSize = DEFAULT_ROLL_SIZE;

    private boolean validatedSettings;

    private StreamKey key;

    @Inject
    RollingStreamAppender(final StreamStore streamStore, final StreamHolder streamHolder, final FeedService feedService, final StreamTypeService streamTypeService, final NodeCache nodeCache) {
        this.streamStore = streamStore;
        this.streamHolder = streamHolder;
        this.feedService = feedService;
        this.streamTypeService = streamTypeService;
        this.nodeCache = nodeCache;
    }

    @Override
    public RollingDestination createDestination() throws IOException {
        if (key.getStreamType() == null) {
            throw new ProcessException("Stream type not specified");
        }
        final StreamType st = streamTypeService.loadByName(key.getStreamType());
        if (st == null) {
            throw new ProcessException("Stream type not specified");
        }

        // Don't set the processor or the task or else this rolling stream will be deleted automatically because the
        // system will think it is superseded output.
        final Stream stream = Stream.createProcessedStream(streamHolder.getStream(), key.getFeed(), st,
                null, null);

        final String nodeName = nodeCache.getDefaultNode().getName();
        final StreamTarget streamTarget = streamStore.openStreamTarget(stream);
        return new RollingStreamDestination(key,
                getFrequency(),
                getRollSize(),
                System.currentTimeMillis(),
                streamStore,
                streamTarget,
                nodeName);
    }

    @Override
    Object getKey() {
        if (key == null) {
            key = new StreamKey(feed, streamType, segmentOutput);
        }

        return key;
    }

    @Override
    void validateSpecificSettings() {
        if (feed == null) {
            if (feedRef != null) {
                feed = feedService.loadByUuid(feedRef.getUuid());
            } else {
                final Stream parentStream = streamHolder.getStream();
                if (parentStream == null) {
                    throw new ProcessException("Unable to determine feed as no parent stream set");
                }

                // Use current feed if none other has been specified.
                feed = feedService.load(parentStream.getFeed());
            }
        }

            if (streamType == null) {
                throw new ProcessException("Stream type not specified");
            }

            if (frequency != null && frequency <= 0) {
                throw new ProcessException("Rolling frequency must be greater than 0");
            }

            if (rollSize <= 0) {
                throw new ProcessException("Roll size must be greater than 0");
            }
        }
    }

    @PipelineProperty(description = "The feed that output stream should be written to. If not specified the feed the input stream belongs to will be used.")
    @PipelinePropertyDocRef(types = Feed.ENTITY_TYPE)
    public void setFeed(final DocRef feedRef) {
        this.feedRef = feedRef;
    }

    @PipelineProperty(description = "The stream type that the output stream should be written as. This must be specified.")
    public void setStreamType(final String streamType) {
        this.streamType = streamType;
    }

    @PipelineProperty(description = "Should the output stream be marked with indexed segments to allow fast access to individual records?", defaultValue = "true")
    public void setSegmentOutput(final boolean segmentOutput) {
        this.segmentOutput = segmentOutput;
    }

    @PipelineProperty(description = "Choose how frequently streams are rolled.", defaultValue = "1h")
    public void setFrequency(final String frequency) {
        if (frequency == null || frequency.trim().length() == 0) {
            this.frequency = null;
        } else {
            try {
                final Long value = ModelStringUtil.parseDurationString(frequency);
                if (value == null || value <= 0) {
                    throw new PipelineFactoryException("Incorrect value for frequency: " + frequency);
                }

                this.frequency = value;
            } catch (final NumberFormatException e) {
                throw new PipelineFactoryException("Incorrect value for frequency: " + frequency);
            }
        }
    }

    @PipelineProperty(description = "Provide a cron expression to determine when streams are rolled.")
    public void setSchedule(final String expression) {
        if (expression == null || expression.trim().length() == 0) {
            this.schedule = null;
        } else {
            try {
                this.schedule = SimpleCron.compile(expression);
            } catch (final NumberFormatException e) {
                throw new PipelineFactoryException("Incorrect value for schedule: " + expression);
            }
        }
    }

    @PipelineProperty(description = "Choose the maximum size that a stream can be before it is rolled.", defaultValue = "100M")
    public void setRollSize(final String rollSize) {
        super.setRollSize(rollSize);
    }
}
