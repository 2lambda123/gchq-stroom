package stroom.index.impl;


import stroom.index.shared.IndexVolume;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(tags = "Stroom-Index Volumes (New UI)")
@Path("/stroom-index/volume" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
public interface NewUIIndexVolumeResource extends RestResource {

    @GET
    @ApiOperation(
            value = "Get all index volumes",
            response = IndexVolume.class,
            responseContainer = "List")
    Response getAll();

    @GET
    @Path("{id}")
    @ApiOperation(
            value = "Get an index volume identified by the supplied ID",
            response = IndexVolume.class)
    Response getById(@PathParam("id") int id);

    @POST
    @ApiOperation(
            value = "Create a new index volume",
            response = IndexVolume.class)
    Response create(@ApiParam("indexVolume") IndexVolume indexVolume);

    @PUT
    @ApiOperation(
            value = "Update an index volume",
            response = IndexVolume.class)
    Response update(@ApiParam("indexVolume") IndexVolume indexVolume);

    @DELETE
    @Path("{id}")
    @ApiOperation(
            value = "Delete an index volume identified by the supplied ID",
            response = IndexVolume.class)
    Response delete(@PathParam("id") int id);

//    /**
//     * Retrieve the list of volumes that are within a group
//     * @param groupName The name of the group
//     * @return The list of Index Volumes in that group
//     */
//    @GET
//    @Path("/inGroup/{groupName}")
//    Response getVolumesInGroup(@PathParam("groupName") String groupName);
//
//    /**
//     * Retrieve the list of groups that a given volume belongs to
//     * @param id The ID of the volume
//     * @return The list of Index Volume Groups for that volume
//     */
//    @GET
//    @Path("/groupsFor/{id}")
//    Response getGroupsForVolume(@PathParam("id") int id);
//
//    /**
//     * Add a volume to the membership of a group.
//     * @param volumeId The ID of the volume
//     * @param groupName the name of the group.
//     * @return Empty response if all went well
//     */
//    @POST
//    @Path("/inGroup/{volumeId}/{groupName}")
//    Response addVolumeToGroup(@PathParam("volumeId") int volumeId,
//                              @PathParam("groupName") String groupName);
//
//    /**
//     * Remove a volume from the membership of a group.
//     * @param volumeId The ID of the volume
//     * @param groupName the name of the group.
//     * @return Empty response if all went well
//     */
//    @DELETE
//    @Path("/inGroup/{volumeId}/{groupName}")
//    Response removeVolumeFromGroup(@PathParam("volumeId") int volumeId,
//                                   @PathParam("groupName") String groupName);
}
