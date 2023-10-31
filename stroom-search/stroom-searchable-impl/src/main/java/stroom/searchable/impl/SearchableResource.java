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

package stroom.searchable.impl;

import stroom.datasource.api.v2.DataSource;
import stroom.datasource.api.v2.DataSourceResource;
import stroom.docref.DocRef;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "Searchable")
@Path("/searchable" + ResourcePaths.V2)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SearchableResource extends DataSourceResource, RestResource {

    @POST
    @Path("/dataSource")
    @Operation(
            summary = "Submit a request for a data source definition, supplying the DocRef for the data source",
            operationId = "getSearchableDataSource")
    DataSource getDataSource(@Parameter(description = "DocRef", required = true) DocRef docRef);

    @POST
    @Path("/search")
    @Operation(
            summary = "Submit a search request",
            operationId = "startSearchableQuery")
    SearchResponse search(@Parameter(description = "SearchRequest", required = true) SearchRequest request);

    @POST
    @Path("/destroy")
    @Operation(
            summary = "Destroy a running query",
            operationId = "destroySearchableQuery")
    Boolean destroy(@Parameter(description = "QueryKey", required = true) QueryKey queryKey);
}
