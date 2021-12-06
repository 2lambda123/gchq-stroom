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

package stroom.search.extraction;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Val;
import stroom.index.shared.IndexConstants;
import stroom.meta.api.MetaService;
import stroom.meta.shared.Meta;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.SearchProgressLog;
import stroom.util.logging.SearchProgressLog.SearchPhase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class StreamMapCreator {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(StreamMapCreator.class);

    private final MetaService metaService;

    private final int streamIdIndex;
    private final int eventIdIndex;

    private Map<Long, Optional<Object>> filteredStreamCache;
    private ExtractionException error;

    StreamMapCreator(final FieldIndex fieldIndex,
                     final MetaService metaService) {
        this.metaService = metaService;

        // First get the index in the stored data of the stream and event id fields.
        streamIdIndex = getFieldIndex(fieldIndex, IndexConstants.STREAM_ID);
        eventIdIndex = getFieldIndex(fieldIndex, IndexConstants.EVENT_ID);
    }

    private int getFieldIndex(final FieldIndex fieldIndex, final String fieldName) {
        int index = -1;

        final Integer pos = fieldIndex.getPos(fieldName);
        if (pos == null) {
            if (error == null) {
                error = new ExtractionException("The " + fieldName + " has not been stored in this index");
            }
        } else {
            index = pos;
        }

        return index;
    }

    void addEvent(final StreamEventMap streamEventMap, final Val[] storedData) {
        if (error != null) {
            throw error;
        } else {
            final long longStreamId = getLong(storedData, streamIdIndex);
            final long longEventId = getLong(storedData, eventIdIndex);

            // Stream may have been deleted but still be in the index
            final Optional<Val[]> optValues = getData(longStreamId, longEventId, storedData);
            optValues.ifPresent(data -> {
                try {
                    final Event event = new Event(longStreamId, longEventId, data);
                    SearchProgressLog.increment(SearchPhase.EXTRACTION_DECORATOR_FACTORY_STREAM_EVENT_MAP_PUT);
                    streamEventMap.put(event);
                } catch (final InterruptedException e) {
                    LOGGER.trace(e::getMessage, e);
                    // Keep interrupting this thread.
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private Optional<Val[]> getData(final long longStreamId, final long longEventId, final Val[] storedData) {
        if (longStreamId != -1 && longEventId != -1) {
            // Create a map to cache stream lookups. If we have cached more than a million streams then
            // discard the map and start again to avoid using too much memory.
            if (filteredStreamCache == null || filteredStreamCache.size() > 1000000) {
                filteredStreamCache = new HashMap<>();
            }

            final Optional<Object> optional = filteredStreamCache.computeIfAbsent(longStreamId, k -> {
                try {
                    // See if we can load the stream. We might get a StreamPermissionException if we aren't
                    // allowed to read from this stream.
                    return Optional.ofNullable(metaService.getMeta(k));
//                } catch (final StreamPermissionException e) {
//                    LOGGER.debug(e::getMessage, e);
//                    return Optional.of(e);
                } catch (final RuntimeException e) {
                    LOGGER.error(e::getMessage, e);
                    return Optional.of(e);
                }
            });

            if (optional.isEmpty()) {
                // Likely stream has been deleted due to data retention rules so we can quietly ignore it
                LOGGER.debug(() -> "Stream not found with id " + longStreamId);
            }

            return optional.map(cached -> {
                if (cached instanceof Throwable) {
                    final Throwable t = (Throwable) cached;
                    throw new ExtractionException(t.getMessage(), t);
                } else if (cached instanceof Meta) {
                    return storedData;
                } else {
                    throw new ExtractionException("Unexpected cached type " + cached.getClass().getSimpleName());
                }
            });
        }
        throw new ExtractionException("No event id supplied");
    }

    private long getLong(final Val[] storedData, final int index) {
        try {
            if (index >= 0 && storedData.length > index) {
                final Val value = storedData[index];
                return value.toLong();
            }
        } catch (final Exception e) {
            // Ignore
        }

        return -1;
    }
}
