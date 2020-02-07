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

package stroom.util.shared;



public class Expander {
    private static final long serialVersionUID = 8021806527404021016L;

    private int depth;
    private boolean expanded;
    private boolean leaf;

    public Expander() {
        // Default constructor necessary for GWT serialisation.
    }

    public Expander(final int depth, final boolean expanded, final boolean leaf) {
        this.depth = depth;
        this.expanded = expanded;
        this.leaf = leaf;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(final int depth) {
        this.depth = depth;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(final boolean leaf) {
        this.leaf = leaf;
    }
}
