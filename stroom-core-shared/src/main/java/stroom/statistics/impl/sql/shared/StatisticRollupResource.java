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
 */

package stroom.statistics.impl.sql.shared;

import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fusesource.restygwt.client.DirectRestService;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "SQL Statistics RollUps")
@Path("/statistic/rollUp" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface StatisticRollupResource extends RestResource, DirectRestService {

    @POST
    @Path("/bitMaskPermGeneration")
    @Operation(
            summary = "Create rollup bit mask",
            operationId = "statisticBitMaskPermGeneration")
    List<CustomRollUpMask> bitMaskPermGeneration(
            @Parameter(description = "fieldCount", required = true) Integer fieldCount);

    @POST
    @Path("/bitMaskConversion")
    @Operation(
            summary = "Get rollup bit mask",
            operationId = "statisticBitMaskConversion")
    List<CustomRollUpMaskFields> bitMaskConversion(
            @Parameter(description = "maskValues", required = true) List<Short> maskValues);

    @POST
    @Path("/dataSourceFieldChange")
    @Operation(
            summary = "Change fields",
            operationId = "statisticFieldChange")
    StatisticsDataSourceData fieldChange(
            @Parameter(description = "request", required = true) StatisticsDataSourceFieldChangeRequest request);
}
