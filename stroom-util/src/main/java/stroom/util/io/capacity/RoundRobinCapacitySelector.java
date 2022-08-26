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

package stroom.util.io.capacity;

import stroom.util.concurrent.AtomicLoopedItemSequence;
import stroom.util.shared.HasCapacity;

import java.util.List;

public class RoundRobinCapacitySelector implements HasCapacitySelector {
    public static final String NAME = "RoundRobin";

//    private final AtomicInteger roundRobinPosition = new AtomicInteger();
    private final AtomicLoopedItemSequence atomicLoopedIntegerSequence = AtomicLoopedItemSequence.create();

    @Override
    public <T extends HasCapacity> T select(final List<T> list) {
        return atomicLoopedIntegerSequence.getNextItem(list)
                .orElse(null);
//        if (list.size() == 0) {
//            return null;
//        }
//        if (list.size() == 1) {
//            return list.get(0);
//        }
//
//        final int pos = atomicLoopedIntegerSequence.getNext();

        // Ensure the position is limited.
//        if (pos > 1_000_000) {
//            synchronized (roundRobinPosition) {
//                if (roundRobinPosition.get() > 10_000) {
//                    roundRobinPosition.addAndGet(-10_000);
//                }
//            }
//        }

//        final int index = pos % list.size();
//        return list.get(index);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
