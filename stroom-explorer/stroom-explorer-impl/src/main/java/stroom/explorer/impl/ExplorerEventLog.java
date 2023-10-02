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

package stroom.explorer.impl;

import stroom.docref.DocRef;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.PermissionInheritance;

interface ExplorerEventLog {

    void create(String type,
                String uuid,
                String name,
                DocRef folder,
                PermissionInheritance permissionInheritance,
                Exception ex);

    void copy(DocRef document, DocRef folder, PermissionInheritance permissionInheritance, Exception ex);

    void move(DocRef document, DocRef folder, PermissionInheritance permissionInheritance, Exception ex);

    void rename(DocRef document, String name, Exception ex);

    void update(ExplorerNode explorerNodeBefore, ExplorerNode explorerNodeAfter, Exception ex);

    void delete(DocRef document, Exception ex);
}
