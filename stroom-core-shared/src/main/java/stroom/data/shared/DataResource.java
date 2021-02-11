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

package stroom.data.shared;

import stroom.meta.shared.FindMetaCriteria;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourceKey;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fusesource.restygwt.client.DirectRestService;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(value = "data - /v1")
@Path("/data" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DataResource extends RestResource, DirectRestService {

    @POST
    @Path("download")
    @ApiOperation(
            value = "Download matching data",
            response = ResourceGeneration.class)
    ResourceGeneration download(@ApiParam("criteria") FindMetaCriteria criteria);

    @POST
    @Path("upload")
    @ApiOperation(
            value = "Upload data",
            response = ResourceGeneration.class)
    ResourceKey upload(@ApiParam("request") UploadDataRequest request);

    @GET
    @Path("info/{id}")
    @ApiOperation(
            value = "Find full info about a data item",
            response = DataInfoSection.class)
    List<DataInfoSection> info(@PathParam("id") long id);
}
