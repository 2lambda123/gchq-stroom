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
 *
 */

package stroom.pipeline.refdata.store;

import stroom.bytebuffer.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Supplier;

public class FastInfosetValue implements RefDataValue {

    public static final int TYPE_ID = 1;

    private final ByteBuffer fastInfosetByteBuffer;
    private volatile Long fastInfosetValueHash = null;

    public FastInfosetValue(final ByteBuffer fastInfosetByteBuffer) {
        this.fastInfosetByteBuffer = fastInfosetByteBuffer;
    }

    public static FastInfosetValue wrap(final ByteBuffer fastInfosetByteBuffer) {
        return new FastInfosetValue(fastInfosetByteBuffer);
    }

    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    @Override
    public long getValueHashCode(final ValueStoreHashAlgorithm valueStoreHashAlgorithm) {
        // Lazily compute the hash and hold for future use these values can be quite big.
        // This will mostly be used during a load which is single threaded so no need to
        // avoid a synch at the risk of getting the same hash value twice.
        if (fastInfosetValueHash == null) {
            fastInfosetValueHash = valueStoreHashAlgorithm.hash(fastInfosetByteBuffer);
        }
        return fastInfosetValueHash;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FastInfosetValue that = (FastInfosetValue) o;
        return Objects.equals(fastInfosetByteBuffer, that.fastInfosetByteBuffer);
    }

    @Override
    public int hashCode() {
        return ByteBufferUtils.basicHashCode(fastInfosetByteBuffer);
    }

    public ByteBuffer getByteBuffer() {
        return fastInfosetByteBuffer;
    }

    public RefDataValue copy(final Supplier<ByteBuffer> byteBufferSupplier) {
        ByteBuffer newByteBuffer = byteBufferSupplier.get();
        ByteBufferUtils.copy(this.fastInfosetByteBuffer, newByteBuffer);
        return new FastInfosetValue(newByteBuffer);
    }

    public int size() {
        return fastInfosetByteBuffer.limit() - fastInfosetByteBuffer.position();
    }

    @Override
    public String toString() {
        return "FastInfosetValue{" +
                "fastInfosetByteBuffer=" + fastInfosetByteBuffer +
                '}';
    }
}
