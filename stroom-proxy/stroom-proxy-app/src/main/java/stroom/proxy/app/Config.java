package stroom.proxy.app;

import io.dropwizard.Configuration;

public class Config extends Configuration {

    private ProxyConfig proxyConfig;

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(final ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
}
