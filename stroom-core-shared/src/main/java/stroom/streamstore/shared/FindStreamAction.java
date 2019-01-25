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

package stroom.streamstore.shared;

import stroom.task.shared.Action;
import stroom.entity.shared.ResultList;
import stroom.data.meta.shared.FindDataCriteria;
import stroom.data.meta.shared.DataRow;

public class FindStreamAction extends Action<ResultList<DataRow>> {
    private static final long serialVersionUID = -3560107233301674555L;

    private FindDataCriteria criteria;

    public FindStreamAction() {
    }

    public FindStreamAction(final FindDataCriteria criteria) {
        this.criteria = criteria;
    }

    public FindDataCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(FindDataCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public String getTaskName() {
        return "Find Stream Action";
    }
}
