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

package stroom.job.shared;

import stroom.docref.SharedObject;
import stroom.util.shared.Expander;
import stroom.util.shared.TreeRow;

public class RootRow implements SharedObject, TreeRow {
    private static final long serialVersionUID = -2511849708703770119L;

    private Expander expander;

    public RootRow() {
        // Default constructor necessary for GWT serialisation.
    }

    public RootRow(final Expander expander) {
        this.expander = expander;
    }

    @Override
    public Expander getExpander() {
        return expander;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return !(obj == null || !(obj instanceof RootRow));
    }
}
