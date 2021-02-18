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

package stroom.index.impl;

import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(tags = "Stroom-Index Queries")
@Path("/stroom-index" + ResourcePaths.V2)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface StroomIndexQueryResource extends RestResource {

    @POST
    @Path("/dataSource")
    @ApiOperation("Submit a request for a data source definition, supplying the DocRef for the data source")
    DataSource getDataSource(@ApiParam("DocRef") DocRef docRef);

    @POST
    @Path("/search")
    @ApiOperation("Submit a search request")
    SearchResponse search(@ApiParam("SearchRequest") SearchRequest request);

    @POST
    @Path("/destroy")
    @ApiOperation("Destroy a running query")
    Boolean destroy(@ApiParam("QueryKey") QueryKey queryKey);
}
