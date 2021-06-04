package stroom.security.identity.token;

import stroom.security.api.SecurityContext;
import stroom.security.api.TokenException;
import stroom.security.api.TokenVerifier;
import stroom.security.identity.account.Account;
import stroom.security.identity.account.AccountDao;
import stroom.security.identity.account.AccountService;
import stroom.security.identity.config.TokenConfig;
import stroom.security.identity.exceptions.NoSuchUserException;
import stroom.security.openid.api.OpenIdClientFactory;
import stroom.security.openid.api.PublicJsonWebKeyProvider;
import stroom.security.shared.PermissionNames;
import stroom.util.HasHealthCheck;
import stroom.util.rest.RestUtil;
import stroom.util.shared.PermissionException;
import stroom.util.shared.ResultPage;

import com.codahale.metrics.health.HealthCheck;
import org.jose4j.jwk.JsonWebKey;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

public class TokenServiceImpl implements TokenService, HasHealthCheck {

    private final PublicJsonWebKeyProvider publicJsonWebKeyProvider;
    private final TokenDao tokenDao;
    private final AccountDao accountDao;
    private final SecurityContext securityContext;
    private final AccountService accountService;
    private final TokenBuilderFactory tokenBuilderFactory;
    private final TokenConfig tokenConfig;
    private final OpenIdClientFactory openIdClientDetailsFactory;
    private final TokenVerifier tokenVerifier;

    @Inject
    TokenServiceImpl(final PublicJsonWebKeyProvider publicJsonWebKeyProvider,
                     final TokenDao tokenDao,
                     final AccountDao accountDao,
                     final SecurityContext securityContext,
                     final AccountService accountService,
                     final TokenBuilderFactory tokenBuilderFactory,
                     final TokenConfig tokenConfig,
                     final OpenIdClientFactory openIdClientDetailsFactory,
                     final TokenVerifier tokenVerifier) {
        this.publicJsonWebKeyProvider = publicJsonWebKeyProvider;
        this.tokenDao = tokenDao;
        this.accountDao = accountDao;
        this.securityContext = securityContext;
        this.accountService = accountService;
        this.tokenBuilderFactory = tokenBuilderFactory;
        this.tokenConfig = tokenConfig;
        this.openIdClientDetailsFactory = openIdClientDetailsFactory;
        this.tokenVerifier = tokenVerifier;
    }

    static Optional<TokenType> getParsedTokenType(final String tokenType) {

        try {
            if (tokenType == null) {
                return Optional.empty();
            } else {
                return Optional.of(TokenType.fromText(tokenType));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public TokenResultPage list() {
        checkPermission();
        return tokenDao.list();
    }

    @Override
    public TokenResultPage search(final SearchTokenRequest request) {
        checkPermission();
        return tokenDao.search(request);

//        // Validate filters
//        if (searchRequest.getFilters() != null) {
//            for (String key : searchRequest.getFilters().keySet()) {
//                switch (key) {
//                    case "expiresOn":
//                    case "issuedOn":
//                    case "updatedOn":
//                        throw RestUtil.badRequest("Filtering by date is not supported.");
//                }
//            }
//        }
//        return tokenDao.searchTokens(searchRequest);
    }

    @Override
    public Token create(final CreateTokenRequest createTokenRequest) {
        checkPermission();

        final String userId = securityContext.getUserId();

        final Optional<Integer> optionalAccountId = accountDao.getId(createTokenRequest.getUserId());
        final Integer accountId = optionalAccountId.orElseThrow(() ->
                new NoSuchUserException("Cannot find user to associate with this API key!"));

        // Parse and validate tokenType
        final Optional<TokenType> optionalTokenType = getParsedTokenType(createTokenRequest.getTokenType());
        final TokenType tokenType = optionalTokenType.orElseThrow(() ->
                RestUtil.badRequest("Unknown token type:" + createTokenRequest.getTokenType()));

        final Instant expiryInstant = createTokenRequest.getExpiresOnMs() == null
                ? null
                : Instant.ofEpochMilli(createTokenRequest.getExpiresOnMs());

        final long now = System.currentTimeMillis();

        // TODO This assumes we have only one clientId. In theory we may have multiple
        //   and then the UI would need to manage the client IDs in use
        final String clientId = openIdClientDetailsFactory.getClient().getClientId();

        final TokenBuilder tokenBuilder = tokenBuilderFactory
                .expiryDateForApiKeys(expiryInstant)
                .newBuilder(tokenType)
                .clientId(clientId)
                .subject(createTokenRequest.getUserId());

        final Instant actualExpiryDate = tokenBuilder.getExpiryDate();
        final String data = tokenBuilder.build();

        final Token token = new Token();
        token.setCreateTimeMs(now);
        token.setCreateUser(userId);
        token.setUpdateTimeMs(now);
        token.setUpdateUser(userId);
        token.setUserId(createTokenRequest.getUserId());
        token.setTokenType(tokenType.getText());
        token.setData(data);
        token.setExpiresOnMs(actualExpiryDate.toEpochMilli());
        token.setComments(createTokenRequest.getComments());
        token.setEnabled(createTokenRequest.isEnabled());

        return tokenDao.create(accountId, token);
    }

    @Override
    public Token createResetEmailToken(final Account account, final String clientId) {
        final TokenType tokenType = TokenType.EMAIL_RESET;
        final TokenBuilder tokenBuilder = tokenBuilderFactory
                .expiryDateForApiKeys(Instant.now()
                        .plus(tokenConfig.getTimeUntilExpirationForEmailResetToken()))
                .newBuilder(tokenType)
                .clientId(clientId);

        final Instant actualExpiryDate = tokenBuilder.getExpiryDate();
        final String idToken = tokenBuilder.build();

        final String userId = securityContext.getUserId();

        final long now = System.currentTimeMillis();
        final Token token = new Token();
        token.setCreateTimeMs(now);
        token.setUpdateTimeMs(now);
        token.setCreateUser(userId);
        token.setUpdateUser(userId);
        token.setUserId(userId);
        token.setUserEmail(account.getEmail());
        token.setTokenType(tokenType.getText().toLowerCase());
        token.setData(idToken);
        token.setExpiresOnMs(actualExpiryDate.toEpochMilli());
        token.setComments("Created for password reset");
        token.setEnabled(true);

        return tokenDao.create(account.getId(), token);
    }

    @Override
    public int deleteAll() {
        checkPermission();

        return tokenDao.deleteAllTokensExceptAdmins();
    }

    @Override
    public int delete(int tokenId) {
        checkPermission();

        return tokenDao.deleteTokenById(tokenId);
    }

    @Override
    public int delete(String token) {
        checkPermission();

        return tokenDao.deleteTokenByTokenString(token);
    }

    @Override
    public Optional<Token> read(String token) {
        checkPermission();

        return tokenDao.readByToken(token);
    }

    @Override
    public Optional<Token> read(int tokenId) {
        checkPermission();

        return tokenDao.readById(tokenId);
    }

//    @Override
//    public Optional<String> verifyToken(String token) {
////        Optional<Token> tokenRecord = dao.readByToken(token);
////        if (!tokenRecord.isPresent()) {
////            return Optional.empty();
////        }
//        return tokenVerifier.verifyToken(token);
//    }

    @Override
    public int toggleEnabled(int tokenId, boolean isEnabled) {
        checkPermission();
        final String userId = securityContext.getUserId();

        Optional<Account> updatingUser = accountService.read(userId);

        return updatingUser
                .map(account -> tokenDao.enableOrDisableToken(tokenId, isEnabled, account))
                .orElse(0);
    }

    @Override
    public String getPublicKey() {
        return publicJsonWebKeyProvider
                .getFirst()
                .toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
    }

    private void checkPermission() {
        if (!securityContext.hasAppPermission(PermissionNames.MANAGE_USERS_PERMISSION)) {
            throw new PermissionException(securityContext.getUserId(), "You do not have permission to manage users");
        }
    }

    // It could be argued that the validity of the token should be a prop in Token
    // and the API Keys page could display the validity.
    @Override
    public HealthCheck.Result getHealth() {
        // Check all our enabled tokens are valid
        final ResultPage<Token> resultPage = tokenDao.list();

        final HealthCheck.ResultBuilder builder = HealthCheck.Result.builder();

        boolean isHealthy = true;

        final Map<String, Object> invalidTokenDetails = new HashMap<>();
        for (final Token token : resultPage.getValues()) {
            if (token.isEnabled()) {
                try {
                    tokenVerifier.verifyToken(
                            token.getData(),
                            openIdClientDetailsFactory.getClient().getClientId());
                } catch (TokenException e) {
                    isHealthy = false;
                    final Map<String, String> details = new HashMap<>();
                    details.put("expiry", token.getExpiresOnMs() != null
                            ? Instant.ofEpochMilli(token.getExpiresOnMs()).toString()
                            : null);
                    details.put("error", e.getMessage());
                    invalidTokenDetails.put(token.getId().toString(), details);
                }
            }
        }

        if (isHealthy) {
            builder
                    .healthy()
                    .withMessage("All enabled API key tokens are valid");
        } else {
            builder
                    .unhealthy()
                    .withMessage("Some enabled API key tokens are invalid")
                    .withDetail("invalidTokens", invalidTokenDetails);
        }

        return builder.build();
    }

    @Override
    public TokenConfig fetchTokenConfig() {
        return tokenConfig;
    }
}
