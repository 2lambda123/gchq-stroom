package stroom.core.welcome;

import stroom.ui.config.shared.UiConfig;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;

import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "welcome - /v1")
@Path("/welcome" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WelcomeResource implements RestResource {

    private final UiConfig uiConfig;

    @Inject
    WelcomeResource(final UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    @GET
    public Response welcome() {
        Object response = new Object() {
            public final String html = uiConfig.getWelcomeHtml();
        };
        return Response.ok(response).build();
    }
}
