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

package stroom.meta.shared;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fusesource.restygwt.client.DirectRestService;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;
import stroom.util.shared.ResultPage;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "meta - /v1")
@Path("/meta" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MetaResource extends RestResource, DirectRestService {
    @PUT
    @Path("update/status")
    @ApiOperation(
            value = "Update status on matching meta data",
            response = Integer.class)
    Integer updateStatus(UpdateStatusRequest request);

    @POST
    @Path("find")
    @ApiOperation(
            value = "Find matching meta data",
            response = ResourceGeneration.class)
    ResultPage<MetaRow> findMetaRow(@ApiParam("criteria") FindMetaCriteria criteria);

    @POST
    @Path("getSelectionSummary")
    @ApiOperation(
            value = "Get a summary of the selected meta data",
            response = ResourceGeneration.class)
    SelectionSummary getSelectionSummary(@ApiParam("criteria") FindMetaCriteria criteria);

    @POST
    @Path("getReprocessSelectionSummary")
    @ApiOperation(
            value = "Get a summary of the parent items of the selected meta data",
            response = ResourceGeneration.class)
    SelectionSummary getReprocessSelectionSummary(@ApiParam("criteria") FindMetaCriteria criteria);

    @GET
    @Path("info/{id}")
    @ApiOperation(
            value = "Find full info about some meta item",
            response = MetaInfoSection.class)
    List<MetaInfoSection> fetchFullMetaInfo(@PathParam("id") long id);
}
