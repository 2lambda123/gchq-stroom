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

package stroom.search.db;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.util.RestResource;
import stroom.util.json.JsonUtil;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "searchable - /v2")
@Path("/searchable/v2")
@Produces(MediaType.APPLICATION_JSON)
public class SearchableResource implements RestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchableResource.class);

    private final SearchableService searchableService;

    @Inject
    public SearchableResource(final SearchableService searchableService) {
        this.searchableService = searchableService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/dataSource")
    @Timed
    @ApiOperation(
            value = "Submit a request for a data source definition, supplying the DocRef for the data source",
            response = DataSource.class)
    public DataSource getDataSource(@ApiParam("DocRef") final DocRef docRef) {
        if (LOGGER.isDebugEnabled()) {
            String json = JsonUtil.writeValueAsString(docRef);
            LOGGER.debug("/dataSource called with docRef:\n{}", json);
        }
        return searchableService.getDataSource(docRef);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search")
    @Timed
    @ApiOperation(
            value = "Submit a search request",
            response = SearchResponse.class)
    public SearchResponse search(@ApiParam("SearchRequest") final SearchRequest request) {
        if (LOGGER.isDebugEnabled()) {
            String json = JsonUtil.writeValueAsString(request);
            LOGGER.debug("/search called with searchRequest:\n{}", json);
        }

        return searchableService.search(request);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/destroy")
    @Timed
    @ApiOperation(
            value = "Destroy a running query",
            response = Boolean.class)
    public Boolean destroy(@ApiParam("QueryKey") final QueryKey queryKey) {
        if (LOGGER.isDebugEnabled()) {
            String json = JsonUtil.writeValueAsString(queryKey);
            LOGGER.debug("/destroy called with queryKey:\n{}", json);
        }
        return searchableService.destroy(queryKey);
    }
}