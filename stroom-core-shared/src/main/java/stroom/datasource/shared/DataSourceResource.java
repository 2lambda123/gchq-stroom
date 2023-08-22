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

package stroom.datasource.shared;

import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.docstore.shared.Documentation;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fusesource.restygwt.client.DirectRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "Data Sources")
@Path("/dataSource" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DataSourceResource extends RestResource, DirectRestService {

    @POST
    @Path("/fetchFields")
    @Operation(
            summary = "Fetch data source fields",
            operationId = "fetchDataSourceFields")
    DataSource fetch(@Parameter(description = "dataSourceRef", required = true) DocRef dataSourceRef);

    @POST
    @Path("/fetchFieldsFromQuery")
    @Operation(
            summary = "Fetch data source fields",
            operationId = "fetchDataSourceFieldsFromQuery")
    DataSource fetchFromQuery(@Parameter(description = "query", required = true) String query);

    @POST
    @Path("/fetchDocumentation")
    @Operation(
            summary = "Fetch documentation for a data source",
            operationId = "fetchDocumentation")
    Documentation fetchDocumentation(@Parameter(description = "docRef", required = true) DocRef docRef);

}
