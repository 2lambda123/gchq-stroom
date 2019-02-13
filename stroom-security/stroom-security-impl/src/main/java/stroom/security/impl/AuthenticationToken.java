package stroom.security.impl;

public class AuthenticationToken {
    private final String userId;
    private final String jws;

    public AuthenticationToken(final String userId, final String jws) {
        this.userId = userId;
        this.jws = jws;
    }

    public String getUserId() {
        return userId;
    }

    public String getJws() {
        return jws;
    }
}