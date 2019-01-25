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

package stroom.pipeline.xml.converter.ds3;

public abstract class StoreFactory extends NodeFactory {
    private final UniqueInt allReferencedGroups = new UniqueInt();
    private final UniqueInt localReferencedGroups = new UniqueInt();
    private final UniqueInt remoteReferencedGroups = new UniqueInt();

    public StoreFactory(final NodeFactory parent, final String id) {
        super(parent, id);
    }

    public void addReferencedGroup(final int groupNo, final boolean localRef) {
        allReferencedGroups.add(groupNo);
        if (localRef) {
            localReferencedGroups.add(groupNo);
        } else {
            remoteReferencedGroups.add(groupNo);
        }
    }

    public UniqueInt getAllReferencedGroups() {
        return allReferencedGroups;
    }

    public UniqueInt getLocalReferencedGroups() {
        return localReferencedGroups;
    }

    public UniqueInt getRemoteReferencedGroups() {
        return remoteReferencedGroups;
    }
}
