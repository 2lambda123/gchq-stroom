package stroom.security.shared;

import stroom.util.shared.FetchWithIntegerId;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.fusesource.restygwt.client.DirectRestService;

import java.util.Collection;

@Tag(name = "API Key")
@Path("/apikey" + ResourcePaths.V2)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ApiKeyResource extends RestResource, DirectRestService, FetchWithIntegerId<HashedApiKey> {

    @POST
    @Path("/")
    @Operation(
            summary = "Creates a new API key",
            operationId = "createApiKey")
    CreateHashedApiKeyResponse create(
            @Parameter(description = "request", required = true) final CreateHashedApiKeyRequest request);

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Fetch a dictionary doc by its UUID",
            operationId = "fetchApiKey")
    HashedApiKey fetch(@PathParam("id") Integer id);

    @PUT
    @Path("/{id}")
    @Operation(
            summary = "Update a dictionary doc",
            operationId = "updateApiKey")
    HashedApiKey update(@PathParam("id") final int id,
                        @Parameter(description = "apiKey", required = true) final HashedApiKey apiKey);

    @Operation(
            summary = "Delete an API key by ID.",
            operationId = "deleteApiKey")
    @DELETE
    @Path("/{id}")
    boolean delete(@PathParam("id") final int id);

    @Operation(
            summary = "Delete a batch of API keys by ID.",
            operationId = "deleteApiKey")
    @DELETE
    @Path("/deleteBatch")
    int deleteBatch(@Parameter(description = "ids", required = true) final Collection<Integer> ids);

    @POST
    @Path("/find")
    @Operation(
            summary = "Find the API keys matching the supplied criteria",
            operationId = "findApiKeysByCriteria")
    ApiKeyResultPage find(@Parameter(description = "criteria", required = true) FindApiKeyCriteria criteria);
}
