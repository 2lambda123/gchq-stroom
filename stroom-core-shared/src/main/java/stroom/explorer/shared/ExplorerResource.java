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

package stroom.explorer.shared;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fusesource.restygwt.client.DirectRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Api(tags = "Explorer (v2)")
@Path("/explorer" + ResourcePaths.V2)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExplorerResource extends RestResource, DirectRestService {

    @POST
    @Path("/create")
    @ApiOperation(
            value = "Create explorer item",
            response = DocRef.class)
    DocRef create(@ApiParam("request") ExplorerServiceCreateRequest request);

    @DELETE
    @Path("/delete")
    @ApiOperation(
            value = "Delete explorer items",
            response = BulkActionResult.class)
    BulkActionResult delete(@ApiParam("request") ExplorerServiceDeleteRequest request);

    @POST
    @Path("/copy")
    @ApiOperation(
            value = "Copy explorer items",
            response = BulkActionResult.class)
    BulkActionResult copy(@ApiParam("request") ExplorerServiceCopyRequest request);

    @PUT
    @Path("/move")
    @ApiOperation(
            value = "Move explorer items",
            response = BulkActionResult.class)
    BulkActionResult move(@ApiParam("request") ExplorerServiceMoveRequest request);

    @PUT
    @Path("/rename")
    @ApiOperation(
            value = "Rename explorer items",
            response = DocRef.class)
    DocRef rename(@ApiParam("request") ExplorerServiceRenameRequest request);

    @POST
    @Path("/info")
    @ApiOperation(
            value = "Get document info",
            response = DocRefInfo.class)
    DocRefInfo info(@ApiParam("docRef") DocRef docRef);

    @POST
    @Path("/fetchDocRefs")
    @ApiOperation(
            value = "Fetch document references",
            response = Set.class)
    Set<DocRef> fetchDocRefs(@ApiParam("docRefs") Set<DocRef> docRefs);

    @GET
    @Path("/fetchDocumentTypes")
    @ApiOperation(
            value = "Fetch document types",
            response = DocumentTypes.class)
    DocumentTypes fetchDocumentTypes();

    @POST
    @Path("/fetchExplorerPermissions")
    @ApiOperation(
            value = "Fetch permissions for explorer items",
            response = Map.class)
    Set<ExplorerNodePermissions> fetchExplorerPermissions(@ApiParam("explorerNodes") List<ExplorerNode> explorerNodes);

    @POST
    @Path("/fetchExplorerNodes")
    @ApiOperation(
            value = "Fetch explorer nodes",
            response = FetchExplorerNodeResult.class)
    FetchExplorerNodeResult fetch(@ApiParam("request") FindExplorerNodeCriteria request);
}
