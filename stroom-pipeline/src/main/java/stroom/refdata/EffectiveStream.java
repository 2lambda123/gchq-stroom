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

package stroom.refdata;

import stroom.util.shared.EqualsBuilder;
import stroom.util.shared.HashCodeBuilder;

public class EffectiveStream implements Comparable<EffectiveStream> {
    private final long streamId;
    private final long effectiveMs;
    private final int hashCode;

    public EffectiveStream(final long streamId, final long effectiveMs) {
        this.streamId = streamId;
        this.effectiveMs = effectiveMs;

        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(streamId);
        builder.append(effectiveMs);
        hashCode = builder.toHashCode();
    }

    public long getStreamId() {
        return streamId;
    }

    public long getEffectiveMs() {
        return effectiveMs;
    }

    @Override
    public int compareTo(final EffectiveStream o) {
        return Long.compare(effectiveMs, o.effectiveMs);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof EffectiveStream)) {
            return false;
        }

        final EffectiveStream effectiveStream = (EffectiveStream) o;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(streamId, effectiveStream.streamId);
        builder.append(effectiveMs, effectiveStream.effectiveMs);
        return builder.isEquals();
    }

    @Override
    public String toString() {
        return "streamId=" + streamId + ", effectiveMs=" + effectiveMs;
    }
}
