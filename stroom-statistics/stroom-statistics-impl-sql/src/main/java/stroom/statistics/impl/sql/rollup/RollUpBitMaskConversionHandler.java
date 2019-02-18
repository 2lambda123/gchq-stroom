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

package stroom.statistics.impl.sql.rollup;

import stroom.util.shared.BaseResultList;
import stroom.util.shared.ResultList;
import stroom.statistics.impl.sql.shared.CustomRollUpMaskFields;
import stroom.statistics.impl.sql.shared.RollUpBitMaskConversionAction;
import stroom.task.api.AbstractTaskHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


class RollUpBitMaskConversionHandler
        extends AbstractTaskHandler<RollUpBitMaskConversionAction, ResultList<CustomRollUpMaskFields>> {
    @Override
    public BaseResultList<CustomRollUpMaskFields> exec(final RollUpBitMaskConversionAction action) {
        final List<CustomRollUpMaskFields> customRollUpMaskFieldsList = new ArrayList<>();

        int id = 0;
        for (final Short maskValue : action.getMaskValues()) {
            final Set<Integer> tagPositions = RollUpBitMask.fromShort(maskValue).getTagPositions();

            customRollUpMaskFieldsList.add(new CustomRollUpMaskFields(id++, maskValue, tagPositions));
        }

        Collections.sort(customRollUpMaskFieldsList);

        return BaseResultList.createUnboundedList(customRollUpMaskFieldsList);
    }
}
