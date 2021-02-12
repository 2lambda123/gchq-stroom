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

package stroom.receive.rules.shared;

import stroom.docref.DocRef;
import stroom.importexport.shared.Base64EncodedDocumentData;
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
import java.util.Set;

@Api(tags = "Rule Set")
@Path(ReceiveDataRuleSetResource.BASE_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ReceiveDataRuleSetResource extends RestResource, DirectRestService {

    String BASE_RESOURCE_PATH = "/ruleset" + ResourcePaths.V2;

    @POST
    @Path("/read")
    @ApiOperation("Get a rules doc")
    ReceiveDataRules read(@ApiParam("docRef") DocRef docRef);

    @PUT
    @Path("/update")
    @ApiOperation("Update a rules doc")
    ReceiveDataRules update(@ApiParam("receiveDataRules") ReceiveDataRules receiveDataRules);

    @GET
    @Path("/list")
    @ApiOperation("Submit a request for a list of doc refs held by this service")
    Set<DocRef> listDocuments();

    @POST
    @Path("/import")
    @ApiOperation("Submit an import request")
    DocRef importDocument(@ApiParam("DocumentData") Base64EncodedDocumentData documentData);

    @POST
    @Path("/export")
    @ApiOperation("Submit an export request")
    Base64EncodedDocumentData exportDocument(@ApiParam("DocRef") DocRef docRef);
}