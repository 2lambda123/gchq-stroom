package stroom.pipeline.server.xsltfunctions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.EmptyAtomicSequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.node.server.StroomPropertyService;
import stroom.node.shared.ClientProperties;
import stroom.util.spring.StroomScope;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Component
@Scope(StroomScope.PROTOTYPE)
class FetchJson extends StroomExtensionFunctionCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchJson.class);

    private final Map<String, String> namedUrls;

    @Inject
    FetchJson(final StroomPropertyService propertyService) {
        namedUrls = propertyService.getLookupTable(ClientProperties.URL_LIST, ClientProperties.URL_BASE);
    }

    @Override
    protected Sequence call(String functionName, XPathContext context, Sequence[] arguments) throws XPathException {
        Sequence sequence = EmptyAtomicSequence.getInstance();

        final String urlName = getSafeString(functionName, context, arguments, 0);
        final String path = getSafeString(functionName, context, arguments, 1);

        LOGGER.trace(String.format("Looking Up JSON at %s - %s", urlName, path));

        try {
            final String namedUrl = namedUrls.get(urlName);

            if (null != namedUrl) {
                final String completeUrl = namedUrl + path;
                final URL ann = new URL(completeUrl);
                final HttpURLConnection yc = (HttpURLConnection) ann.openConnection();

                switch (yc.getResponseCode()) {
                    case 200: { // OK
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                                yc.getInputStream()))) {
                            final String json = in.lines().reduce("", String::concat);

                            sequence = JsonToXml.jsonToXml(context, json);
                            LOGGER.trace(String.format("Found Data %s: %s", completeUrl, json));
                        }
                        break;
                    }
                    case 404: // NOT_FOUND
                        // this is an expected failure condition
                        break;
                    default:
                        LOGGER.warn("Could not make request to Annotations Service: " + yc.getResponseCode());
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not make request to Annotations Service: " + e.getLocalizedMessage());
        }

        return sequence;
    }
}
