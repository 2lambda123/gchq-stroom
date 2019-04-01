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

package stroom.search.impl;

import stroom.dashboard.expression.v1.Val;

public class Event implements Comparable<Event> {
    private final long id;
    private final Val[] values;

    public Event(final long id, final Val[] values) {
        this.id = id;
        this.values = values;
    }

    public long getId() {
        return id;
    }

    public Val[] getValues() {
        return values;
    }

    @Override
    public int compareTo(final Event o) {
        return Long.compare(id, o.id);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
