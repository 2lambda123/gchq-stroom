package stroom.util.authentication;

/**
 * A set of hard-coded credentials for use ONLY in testing. They allow us to run
 * stroom and stroom-proxy such that API calls from proxy to stroom will pass
 * authentication out of the box.
 * <p>
 * The values used in the this class are generated by the main() method in
 * {@link stroom.authentication.oauth2.GenerateTestOpenIdDetails}
 * <p>
 * These default values will only be used if the following prop is set to true
 * {@link stroom.authentication.config.AuthenticationConfig#isUseDefaultOpenIdCredentials()}
 */
public class DefaultOpenIdCredentials {

    // Made with a public ctor so it is injectable and mockable, if required.
    public DefaultOpenIdCredentials() {
    }

    // ==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--
    // ALL the content between these dashed lines was generated inserted using
    // stroom.authentication.oauth2.GenerateTestOpenIdDetails at 2020-05-05T19:46:44.555896Z
    // The dashed lines are important, don't remove them!
    private static final String OAUTH2_CLIENT_ID = "NXCmrrLjPGeA1Sx5cDfAz9Ev87Wi3nptTo6Rw5fL.client-id.apps.stroom-idp";
    private static final String OAUTH2_CLIENT_NAME = "Stroom Client Internal (TEST ONLY)";
    private static final String OAUTH2_CLIENT_SECRET = "1rSyY1sYT2Az51LSRbWExt6qFrojbBYeiT8Nb2jV.client-secret.apps.stroom-idp";
    private static final String OAUTH2_CLIENT_URI_PATTERN = ".*";
    private static final String PUBLIC_KEY_ID = "8a3b58ca-96e7-4aa4-9f07-4400aaed1471";
    private static final String PUBLIC_KEY_JSON = "{\"kty\":\"RSA\",\"kid\":\"8a3b58ca-96e7-4aa4-9f07-4400aaed1471\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"qazmtYcnJxylWjpV9wpyhnm9VHdUHBtw7MvnNkoQpKxlTPGFMXsGYlMi7NOMPEztZ20KoIAI7JnDkq7iGSeNULwJKljWjGO3BGs5HyLirAOQEffgRVRBB8tE4timRga09M5uNvLhwG8PuA1fQHU-L2yNBrFFg-JPvHTYk4uH7G5lc7E8MEx7IgbW3g5ydGcHR7UG_ADEQ9gXQvvWJCgQgP_r1SPIuzVcsiyLRU_-4B5Ihoqi9Bl8DJ4wrCupEOXl-hRDktE63BKcmoA3lFW3HouCKMCvemEObr0PIhx1VRmsXMdGr5naJu2sCcUX-sNoE819eJnKA2CnwnWKFlUs9Q\",\"e\":\"AQAB\",\"d\":\"mqLJYIdcV3jz-ddQoWUUGxq4QlK-hR89O5JZllDJ-kpjyEwVZ5MHrnS2T8A8_nfdpXTrSntlprw8UWKxDNvPHtVARSfR4QC_u3c9B_NzQfG9S6Ls8kJnQgMvNM7eOtPB95prOAkDVa3iVYtaVBSGKBJ2NLM694c5xDd5an6v0fkNyy-ryGLeYh-J-fmW-sVKty94944BP2xtI9BTora1gpQSjC26cvqH0X4-nLoZw_fjHd9pDP3-HkiinymosPAr5sM_LWYsDmh3tS1AZxS5nzZYf_VrZwAIC28BiRpfsnF_Hqza0dig0U5h7515qi7u0bpALMc3CoGUZTBhRM9OAQ\",\"p\":\"4YATq5fjYqyZg7pWR9YkDzYrYnLsQlyBRA1SJuRr162u-kYCmJ0bbyqg03hXKUDPXNVvLL9etW4j97zKsfAKcnx1AE_9u20FurGv1Frp0E3dkMHraLyqNaQ2t9KIjZy_UbCDApCe1StHkbIZ1qvWCowOK8un2VEC5vasQz4Ia0k\",\"q\":\"wJ_kpl5UxH-V3u2qPbuPaNM5v8yZX30fXEuMyA-eN-TmSJU7Wy5mwctGP5sUK0iwEI_WGaRGa7OAdYYyXhYfA3lOLbmUXayLhNwbHPt5Vv28LI6d9EHzisTwlKcrN9UU8m8uavEE0IwTm3uJnMIINYv11H3M9ea490lDP589qE0\",\"dp\":\"s2EzxItFqYfNcpk261wwQR2BM4Zh5IQ7nAvQrvmDxAT4rUQl6osHeM37M12GUF2q1pk-H-V3jHG6EOdQgm2Fkf7o_7-iAoc-SH3ydcWAO2DFgNKR1jZGW_duQ5opYCUxl-UGwnKSttX_z6lSno8A7FDvMO2HbvrfiSTucWsWPWk\",\"dq\":\"HhOZRyTphXkKdLGVaGg13z9EAB-5PwCxed7OD5rosH-MOX8bsoQPRWkxAdN8FboWy6851e0hlcWEZFVc3fYER62UOG2Grg24Bzm6_g6CBsQ9spNeNHprxZo0mnFzvRcHW0di7w3NG6cTlK54cUKWt42wB8rQywCIlwwYdQGvRe0\",\"qi\":\"Aide1b1sgTUVlHZsY8zZKq9xF8uOJtoa1z2fUa9FmYmyD6MJvFUFDLBPoA5HAHqhd12MAF0sPIdhwyHr_4X7Vr_gp7wVZtYpES3-eWJnw5cuFRG1Ft19teBDb5qcZJi86I3vMQNuX1eVyQ95ClZ7nFGR3q6Kgcsf1_H4KSq2YVY\"}";
    private static final String API_KEY_USER_EMAIL = "default-test-only-api-key-user";
    private static final String API_KEY = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhhM2I1OGNhLTk2ZTctNGFhNC05ZjA3LTQ0MDBhYWVkMTQ3MSJ9.eyJleHAiOjIyMTk4NjAwMDQsInN1YiI6ImRlZmF1bHQtdGVzdC1vbmx5LWFwaS1rZXktdXNlciIsImlzcyI6InN0cm9vbSIsImF1ZCI6Ik5YQ21yckxqUEdlQTFTeDVjRGZBejlFdjg3V2kzbnB0VG82Unc1ZkwuY2xpZW50LWlkLmFwcHMuc3Ryb29tLWlkcCJ9.AXzncctK_hU22f1hiaAxN-JnjuSCsauDm0HLRmcuAKGO69FFECPvB3WlOMDznhdwMiZzevIWFgiSgVLma7-7d5Tjgd1Yq8azJz9RZ4V3h9VyN9t0N65LHA8kL0ron4HkNLKUXml2WHVpAvh_HtDclosPPmd9HL2Q4Po7z1bLu36sx5sfh6Pbo5k2y92vxBywyUIqHWF4peko5K_oRrS5MaGMm65YV975j8mCsmMbxeUCD1v1nXSboOmB3YLcHn2o1Id18v4PRk8zp16sfT12cTL-PgZ-xMLgewy1FWtVwsD8jRade9s2X_WjXDBDf6YNlCJHdpv-Mk9Ox1jzCPFNTQ";
    // ==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--==--

    public String getOauth2ClientId() {
        return OAUTH2_CLIENT_ID;
    }

    public String getOauth2ClientName() {
        return OAUTH2_CLIENT_NAME;
    }

    public String getOauth2ClientSecret() {
        return OAUTH2_CLIENT_SECRET;
    }

    public String getOauth2ClientUriPattern() {
        return OAUTH2_CLIENT_URI_PATTERN;
    }

    public String getPublicKeyId() {
        return PUBLIC_KEY_ID;
    }

    public String getPublicKeyJson() {
        return PUBLIC_KEY_JSON;
    }

    public String getApiKeyUserEmail() {
        return API_KEY_USER_EMAIL;
    }

    public String getApiKey() {
        return API_KEY;
    }
}
