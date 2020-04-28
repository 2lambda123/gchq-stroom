package stroom.app.guice;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

class WebTargetProxy implements WebTarget {
    private WebTarget webTarget;

    WebTargetProxy(final WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    @Override
    public URI getUri() {
        return webTarget.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return webTarget.getUriBuilder();
    }

    @Override
    public WebTarget path(final String path) {
        webTarget = webTarget.path(path);
        return this;
    }

    @Override
    public WebTarget resolveTemplate(final String name, final Object value) {
        webTarget = webTarget.resolveTemplate(name, value);
        return this;
    }

    @Override
    public WebTarget resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
        webTarget = webTarget.resolveTemplate(name, value, encodeSlashInPath);
        return this;
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(final String name, final Object value) {
        webTarget = webTarget.resolveTemplateFromEncoded(name, value);
        return this;
    }

    @Override
    public WebTarget resolveTemplates(final Map<String, Object> templateValues) {
        webTarget = webTarget.resolveTemplates(templateValues);
        return this;
    }

    @Override
    public WebTarget resolveTemplates(final Map<String, Object> templateValues, final boolean encodeSlashInPath) {
        webTarget = webTarget.resolveTemplates(templateValues, encodeSlashInPath);
        return this;
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(final Map<String, Object> templateValues) {
        webTarget = webTarget.resolveTemplatesFromEncoded(templateValues);
        return this;
    }

    @Override
    public WebTarget matrixParam(final String name, final Object... values) {
        webTarget = webTarget.matrixParam(name, values);
        return this;
    }

    @Override
    public WebTarget queryParam(final String name, final Object... values) {
        webTarget = webTarget.queryParam(name, values);
        return this;
    }

    @Override
    public Builder request() {
        return webTarget.request();
    }

    @Override
    public Builder request(final String... acceptedResponseTypes) {
        return webTarget.request(acceptedResponseTypes);
    }

    @Override
    public Builder request(final MediaType... acceptedResponseTypes) {
        return webTarget.request(acceptedResponseTypes);
    }

    @Override
    public Configuration getConfiguration() {
        return webTarget.getConfiguration();
    }

    @Override
    public WebTarget property(final String name, final Object value) {
        webTarget = webTarget.property(name, value);
        return this;
    }

    @Override
    public WebTarget register(final Class<?> componentClass) {
        webTarget = webTarget.register(componentClass);
        return this;
    }

    @Override
    public WebTarget register(final Class<?> componentClass, final int priority) {
        webTarget = webTarget.register(componentClass, priority);
        return this;
    }

    @Override
    public WebTarget register(final Class<?> componentClass, final Class<?>... contracts) {
        webTarget = webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
        webTarget = webTarget.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(final Object component) {
        webTarget = webTarget.register(component);
        return this;
    }

    @Override
    public WebTarget register(final Object component, final int priority) {
        webTarget = webTarget.register(component, priority);
        return this;
    }

    @Override
    public WebTarget register(final Object component, final Class<?>... contracts) {
        webTarget = webTarget.register(component, contracts);
        return this;
    }

    @Override
    public WebTarget register(final Object component, final Map<Class<?>, Integer> contracts) {
        webTarget = webTarget.register(component, contracts);
        return this;
    }
}