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

package stroom.statistics.impl.sql.search;

import stroom.datasource.api.v2.DataSourceResource;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "Sql Statistics Query")
@Path("/sqlstatistics" + ResourcePaths.V2)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SqlStatisticsQueryResource extends DataSourceResource, RestResource {

    @POST
    @Path("/search")
    @Operation(
            summary = "Submit a search request",
            operationId = "startSqlStatisticsQuery")
    SearchResponse search(@Parameter(description = "SearchRequest", required = true) SearchRequest request);

    @POST
    @Path("/destroy")
    @Operation(
            summary = "Destroy a running query",
            operationId = "destroySqlStatisticsQuery")
    Boolean destroy(@Parameter(description = "QueryKey", required = true) QueryKey queryKey);
}
