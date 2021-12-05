package stroom.util.io;

import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsProxyConfig;
import stroom.util.shared.IsStroomConfig;

public abstract class PathConfig extends AbstractConfig implements IsProxyConfig, IsStroomConfig {

    private String home;
    private String temp;

    public String getHome() {
        return home;
    }

    public void setHome(final String home) {
        this.home = home;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(final String temp) {
        this.temp = temp;
    }

    @Override
    public String toString() {
        return "PathConfig{" +
                "home='" + home + '\'' +
                ", temp='" + temp + '\'' +
                '}';
    }
}
