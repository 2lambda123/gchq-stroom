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

package stroom.pipeline.refdata.store.offheapstore.serdes;

import stroom.lmdb.Serde;
import stroom.pipeline.refdata.store.offheapstore.RangeStoreKey;
import stroom.pipeline.refdata.store.offheapstore.UID;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.Range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * < mapUid >< rangeStartInc >< rangeEndExc >
 * < 4 bytes >< 8 bytes >< 8 bytes >
 */
public class RangeStoreKeySerde implements Serde<RangeStoreKey> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RangeStoreKeySerde.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(RangeStoreKeySerde.class);

    public static final int RANGE_FROM_OFFSET = UID.UID_ARRAY_LENGTH;
    public static final int RANGE_TO_OFFSET = RANGE_FROM_OFFSET + Long.BYTES;

    private static final int BUFFER_CAPACITY = UID.UID_ARRAY_LENGTH + (Long.BYTES * 2);

    @Override
    public RangeStoreKey deserialize(final ByteBuffer byteBuffer) {

        // Create a bytebuffer that is a view onto the existing buffer
        // NOTE: if the passed bytebuffer is owned by LMDB then this deserialize method
        // needs to be used with care
        final ByteBuffer dupBuffer = byteBuffer.duplicate();

        // Set the limit at the end of the UID part
        dupBuffer.limit(byteBuffer.position() + UID.UID_ARRAY_LENGTH);
        final UID mapUid = UID.wrap(dupBuffer);

        // advance the position now we have a dup of the UID portion
        byteBuffer.position(byteBuffer.position() + UID.UID_ARRAY_LENGTH);

        long rangeFromInc = byteBuffer.getLong();
        long rangeToExc = byteBuffer.getLong();
        byteBuffer.flip();

        return new RangeStoreKey(mapUid, new Range<>(rangeFromInc, rangeToExc));
    }

    @Override
    public void serialize(final ByteBuffer byteBuffer, final RangeStoreKey rangeStoreKey) {
        UIDSerde.writeUid(byteBuffer, rangeStoreKey.getMapUid());
        Range<Long> range = rangeStoreKey.getKeyRange();
        byteBuffer.putLong(range.getFrom());
        byteBuffer.putLong(range.getTo());
        byteBuffer.flip();
    }

    public static boolean isKeyInRange(final ByteBuffer byteBuffer, final long key) {
        // from = inclusive, to = exclusive
        long rangeFromInc = byteBuffer.getLong(RANGE_FROM_OFFSET);

        if (key >= rangeFromInc) {
            final long rangeToExc = byteBuffer.getLong(RANGE_TO_OFFSET);
            return key < rangeToExc;
        }
        return false;
    }

    public void serializeWithoutRangePart(final ByteBuffer byteBuffer, final RangeStoreKey key) {

        serialize(byteBuffer, key);

        // set the limit to just after the UID part
        byteBuffer.limit(UID.UID_ARRAY_LENGTH);
    }

    @Override
    public int getBufferCapacity() {
        return BUFFER_CAPACITY;
    }
}
