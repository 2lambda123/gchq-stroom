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

package stroom.pipeline.shared;

import stroom.docref.DocRef;
import stroom.pipeline.shared.data.PipelineData;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fusesource.restygwt.client.DirectRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "pipeline - /v1")
@Path("/pipeline" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PipelineResource extends RestResource, DirectRestService {

    @POST
    @Path("/read")
    @ApiOperation(
            value = "Get a pipeline doc",
            response = PipelineDoc.class)
    PipelineDoc read(@ApiParam("docRef") DocRef docRef);

    @PUT
    @Path("/update")
    @ApiOperation(
            value = "Update a pipeline doc",
            response = PipelineDoc.class)
    PipelineDoc update(@ApiParam("PipelineDoc") PipelineDoc pipelineDoc);

    @PUT
    @Path("/savePipelineXml")
    @ApiOperation(
            value = "Update a pipeline doc with XML directly",
            response = Boolean.class)
    Boolean savePipelineXml(@ApiParam("request") SavePipelineXmlRequest request);

    @POST
    @Path("/fetchPipelineXml")
    @ApiOperation(
            value = "Fetch the XML for a pipeline",
            response = FetchPipelineXmlResponse.class)
    FetchPipelineXmlResponse fetchPipelineXml(@ApiParam("pipeline") DocRef pipeline);

    @POST
    @Path("/fetchPipelineData")
    @ApiOperation(
            value = "Fetch data for a pipeline",
            response = List.class)
    List<PipelineData> fetchPipelineData(@ApiParam("pipeline") DocRef pipeline);

    @GET
    @Path("/propertyTypes")
    @ApiOperation(
            value = "Get pipeline property types",
            response = List.class)
    List<FetchPropertyTypesResult> getPropertyTypes();
}
