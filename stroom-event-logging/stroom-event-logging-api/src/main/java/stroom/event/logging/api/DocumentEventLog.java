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

package stroom.event.logging.api;

import event.logging.Query;
import stroom.util.shared.BaseCriteria;
import stroom.util.shared.BaseResultList;

public interface DocumentEventLog {
    void create(Object entity, Throwable ex);

    void create(String entityType, String entityName, Throwable ex);

    void copy(Object before, Object after, Throwable ex);

    void move(Object before, Object after, Throwable ex);

    void rename(Object before, Object after, Throwable ex);

    void delete(Object entity, Throwable ex);

    void update(Object before, Object after, Throwable ex);

    void view(Object entity, Throwable ex);

    void delete(BaseCriteria criteria, Query query, Long size, Throwable ex);

    void download(Object entity, Throwable ex);

    void search(BaseCriteria criteria, Query query, BaseResultList<?> results, Throwable ex);

    void searchSummary(BaseCriteria criteria, Query query, BaseResultList<?> results, Throwable ex);
}
