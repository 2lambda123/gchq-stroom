package stroom.docstore.impl.fs;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.inject.Singleton;

@Singleton
public class FSPersistenceConfig {

    private String path;

    @JsonProperty
    public String getPath() {
        return path;
    }

    @JsonProperty
    public void setPath(final String path) {
        this.path = path;
    }
}
