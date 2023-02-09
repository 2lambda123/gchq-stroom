package stroom.index;

import stroom.app.App;
import stroom.config.app.Config;
import stroom.docref.DocRef;
import stroom.query.api.v2.DateTimeSettings;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.servicediscovery.api.RegisteredService;
import stroom.util.shared.ResourcePaths;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is not currently a test. It is a way of exercising the query api, i.e. it is support for manual testing.
 */
//@ExtendWith(DropwizardExtensionsSupport.class)
class TestStroomIndexViewResource {

    // local.yml is not in source control and is created using local.yml.sh
    public static final DropwizardAppExtension<Config> RULE = new DropwizardAppExtension<>(App.class, "../local.yml");

    public static final String SEARCH_TARGET = "http://localhost:8080" +
            ResourcePaths.ROOT_PATH +
            ResourcePaths.API_ROOT_PATH +
            RegisteredService.INDEX_V2.getVersionedPath() +
            "/search";

    private String jwtToken;

    private static SearchRequest getSearchRequest() {
        final QueryKey queryKey = new QueryKey("Some UUID");
        final Query query = Query.builder()
                .dataSource(new DocRef("docRefType", "docRefUuid", "docRefName"))
                .expression(ExpressionOperator.builder()
                        .addTerm("field1", ExpressionTerm.Condition.EQUALS, "value1")
                        .addTerm("field2", ExpressionTerm.Condition.BETWEEN, "value2")
                        .build())
                .build();

        List<ResultRequest> resultRequestList = new ArrayList<>();
        final DateTimeSettings dateTimeSettings = DateTimeSettings.builder().build();
        boolean incremental = false;
        SearchRequest searchRequest = new SearchRequest(
                null,
                queryKey,
                query,
                resultRequestList,
                dateTimeSettings,
                incremental);
        return searchRequest;
    }

    private static ObjectMapper getMapper(final boolean indent) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
        mapper.setSerializationInclusion(Include.NON_NULL);
        // Enabling default typing adds type information where it would otherwise be ambiguous, i.e. for abstract
        // classes
//        mapper.enableDefaultTyping();
        return mapper;
    }

    private static String serialiseSearchRequest(SearchRequest searchRequest) throws JsonProcessingException {
        ObjectMapper objectMapper = getMapper(true);
        return objectMapper.writeValueAsString(searchRequest);
    }

    @Disabled
    // if this is re-enabled then un-comment the DropwizardExtensionSupport class extension above, else test takes
    // ages to run no tests
    @Test
    void testSavedFromFile() throws IOException {
        // Given
        String searchRequestJson = new String(Files.readAllBytes(Paths.get(
                "src/test/resources/searchRequest.json")));
        ObjectMapper objectMapper = new ObjectMapper();
        SearchRequest searchRequest = objectMapper.readValue(searchRequestJson, SearchRequest.class);
        Client client = ClientBuilder.newClient(new ClientConfig().register(ClientResponse.class));

        // When
        Response response = client
                .target(SEARCH_TARGET)
                .request()
                .header("Authorization", "Bearer " + jwtToken)
                .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
                .post(Entity.json(searchRequest));
        SearchResponse searchResponse = response.readEntity(SearchResponse.class);

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(searchResponse.getResults().size()).isEqualTo(5);

        System.out.println(response.toString());
    }

    @Disabled
    // if this is re-enabled then un-comment the DropwizardExtensionSupport class extension above, else test takes
    // ages to run no tests
    @Test
    void test() throws JsonProcessingException {
        // Given
        SearchRequest searchRequest = getSearchRequest();

        // When
        Client client = ClientBuilder.newClient(new ClientConfig().register(ClientResponse.class));
        Response response = client
                .target(SEARCH_TARGET)
                .request()
                .header("Authorization", "Bearer " + jwtToken)
                .accept(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON)
                .post(Entity.json(searchRequest));

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        System.out.println(response.toString());
    }
}
