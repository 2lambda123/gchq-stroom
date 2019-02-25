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

package stroom.dashboard;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import stroom.dashboard.shared.ComponentResultRequest;
import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.Search;
import stroom.dashboard.shared.TableResultRequest;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.ResultRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TestSearchRequestMapper {
//    @Mock
//    private VisualisationStore visualisationStore;

    @InjectMocks
    private SearchRequestMapper searchRequestMapper;

    private static void verify_Search_to_Query_mapping(Search search, Query query) {
        assertThat(search.getDataSourceRef()).isEqualTo(query.getDataSource());
        assertThat(search.getExpression()).isEqualTo(query.getExpression());
        assertThat(search.getParamMap().size()).isEqualTo(query.getParams().size());
        assertThat(search.getParamMap().get("param1")).isEqualTo(query.getParams().get(0).getValue());
        // 'param1' is the key from SearchRequestTestData.dashboardSearchRequest()
    }

    private static void verify_ComponentResultRequest_to_ResultRequest_mappings(
            Map<String, ComponentResultRequest> componentResultRequestMap,
            List<ResultRequest> resultRequests) {

        assertThat(componentResultRequestMap.size()).isEqualTo(resultRequests.size());
        assertThat("componentSettingsMapKey").isEqualTo(resultRequests.get(0).getComponentId());
        assertThat(componentResultRequestMap.get("componentSettingsMapKey").getFetch()).isEqualTo(resultRequests.get(0).getFetch());

        TableResultRequest tableResultRequest = ((TableResultRequest) componentResultRequestMap.get("componentSettingsMapKey"));
        ResultRequest resultRequest = resultRequests.get(0);
        assertThat(tableResultRequest.getRequestedRange().getOffset().toString()).isEqualTo(resultRequest.getRequestedRange().getOffset().toString());
        assertThat(tableResultRequest.getRequestedRange().getLength().toString()).isEqualTo(resultRequest.getRequestedRange().getLength().toString());
        assertThat(tableResultRequest.getOpenGroups()).isEqualTo(resultRequest.getOpenGroups());
        // No test data for this at the moment
        //TODO many more properties to check
    }

    @Test
    void testSearchRequestMapper() {
        // Given
        stroom.dashboard.shared.SearchRequest dashboardSearchRequest = SearchRequestTestData.dashboardSearchRequest();

        // When
        stroom.query.api.v2.SearchRequest mappedApiSearchRequest = searchRequestMapper.mapRequest(new DashboardQueryKey(), dashboardSearchRequest);

        // Then
        verify_Search_to_Query_mapping(
                dashboardSearchRequest.getSearch(),
                mappedApiSearchRequest.getQuery());
        verify_ComponentResultRequest_to_ResultRequest_mappings(
                dashboardSearchRequest.getComponentResultRequests(),
                mappedApiSearchRequest.getResultRequests());
        //TODO many more properties to check
    }

}