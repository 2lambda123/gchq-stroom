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

package stroom.statistics.impl.sql.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StatisticsDataSourceFieldChangeRequest {
    @JsonProperty
    private final StatisticsDataSourceData oldStatisticsDataSourceData;
    @JsonProperty
    private final StatisticsDataSourceData newStatisticsDataSourceData;

    @JsonCreator
    public StatisticsDataSourceFieldChangeRequest(@JsonProperty("oldStatisticsDataSourceData") final StatisticsDataSourceData oldStatisticsDataSourceData,
                                                  @JsonProperty("newStatisticsDataSourceData") final StatisticsDataSourceData newStatisticsDataSourceData) {
        this.oldStatisticsDataSourceData = oldStatisticsDataSourceData;
        this.newStatisticsDataSourceData = newStatisticsDataSourceData;
    }

    public StatisticsDataSourceData getOldStatisticsDataSourceData() {
        return oldStatisticsDataSourceData;
    }

    public StatisticsDataSourceData getNewStatisticsDataSourceData() {
        return newStatisticsDataSourceData;
    }
}
