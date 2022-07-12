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

package stroom.task.shared;

import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fusesource.restygwt.client.DirectRestService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Tag(name = "Tasks")
@Path(TaskResource.BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TaskResource extends RestResource, DirectRestService {

    String BASE_PATH = "/task" + ResourcePaths.V1;
    String LIST_PATH_PART = "/list";
    String FIND_PATH_PART = "/find";
    String USER_PATH_PART = "/user";
    String TERMINATE_PATH_PART = "/terminate";
    String NODE_NAME_PATH_PARAM = "/{nodeName}";

    @GET
    @Path(LIST_PATH_PART + NODE_NAME_PATH_PARAM)
    @Operation(
            summary = "Lists tasks for a node",
            operationId = "listTasks")
    TaskProgressResponse list(@PathParam("memberUuid") String memberUuid);

    @POST
    @Path(FIND_PATH_PART + NODE_NAME_PATH_PARAM)
    @Operation(
            summary = "Finds tasks for a node",
            operationId = "findTasks")
    TaskProgressResponse find(@PathParam("memberUuid") String memberUuid,
                              @Parameter(description = "request", required = true) FindTaskProgressRequest request);

    @GET
    @Path(USER_PATH_PART + NODE_NAME_PATH_PARAM)
    @Operation(
            summary = "Lists tasks for a node",
            operationId = "listUserTasks")
    TaskProgressResponse userTasks(@PathParam("memberUuid") String memberUuid);

    @POST
    @Path(TERMINATE_PATH_PART + NODE_NAME_PATH_PARAM)
    @Operation(
            summary = "Terminates tasks for a node",
            operationId = "terminateTasks")
    Boolean terminate(@PathParam("memberUuid") String memberUuid,
                      @Parameter(description = "request", required = true) TerminateTaskProgressRequest request);
}
