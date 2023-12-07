package stroom.security.impl;

import stroom.security.shared.ApiKey;
import stroom.security.shared.FindApiKeyCriteria;
import stroom.util.NullSafe;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.ResultPage;
import stroom.util.string.Base58;
import stroom.util.string.StringUtil;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton // Due to shared SecureRandom
public class ApiKeyService {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ApiKeyService.class);

    // Stands for stroom-api-key
    private static final String API_KEY_PREFIX = "sak";
    private static final String API_KEY_SEPARATOR = "_";
    private static final Pattern API_KEY_SEPARATOR_PATTERN = Pattern.compile(API_KEY_SEPARATOR, Pattern.LITERAL);
    public static final int API_KEY_RANDOM_CODE_LENGTH = 96;
    public static final int TRUNCATED_HASH_LENGTH = 7;
    public static final int API_KEY_TOTAL_LENGTH = API_KEY_PREFIX.length()
            + (API_KEY_SEPARATOR.length() * 2)
            + API_KEY_RANDOM_CODE_LENGTH
            + TRUNCATED_HASH_LENGTH;

    private static final String BASE_58_ALPHABET = new String(Base58.ALPHABET);
    private static final String BASE_58_CHAR_CLASS = "[" + BASE_58_ALPHABET + "]";

    // A regex pattern that will match a full api key
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "^"
                    + Pattern.quote(API_KEY_PREFIX + API_KEY_SEPARATOR)
                    + BASE_58_CHAR_CLASS + "{" + API_KEY_RANDOM_CODE_LENGTH + "}"
                    + Pattern.quote(API_KEY_SEPARATOR)
                    + BASE_58_CHAR_CLASS + "{" + TRUNCATED_HASH_LENGTH + "}"
                    + "$");
    private static final Predicate<String> API_KEY_MATCH_PREDICATE = API_KEY_PATTERN.asMatchPredicate();

    private final ApiKeyDao apiKeyDao;
    private final SecureRandom secureRandom;

    @Inject
    public ApiKeyService(final ApiKeyDao apiKeyDao) {
        this.apiKeyDao = apiKeyDao;
        this.secureRandom = new SecureRandom();
        LOGGER.debug("API_KEY_PATTERN: '{}'", API_KEY_PATTERN);
    }

    public ResultPage<ApiKey> find(final FindApiKeyCriteria criteria) {

        throw new UnsupportedOperationException("TODO");
    }

    public Optional<String> fetchVerifiedIdentity(final String apiKey) {
        if (!isApiKey(apiKey)) {
            return Optional.empty();
        } else {
            // sha256 hash the whole key then
            return apiKeyDao.fetchVerifiedIdentity(apiKey);
        }
    }

    public Optional<ApiKey> fetch(final int id) {
        return apiKeyDao.fetch(id);
    }

    public ApiKey create(final ApiKey apiKey) {
        return apiKeyDao.create(apiKey);
    }

    public ApiKey update(final ApiKey apiKey) {
        return apiKeyDao.update(apiKey);
    }

    public boolean delete(final int id) {
        return apiKeyDao.delete(id);
    }

    /**
     * Generate a random API key of the form
     * <pre>{@code sak_<random code>_<hash>}</pre>
     * where:
     * <p>
     * {@code <random code>} is a string of random characters using the Base58 character set.
     * </p>
     * <p>
     * {@code <hash>} is the SHA1 hash of {@code <random code>} encoded in Base58 and truncated to the first
     * seven characters.
     * </p>
     *
     * <p>The following cyberchef recipe takes a full API key as input and highlights the hash part if it is valid</p>
     * <pre>
     * Register('sak_.*_(.*)$',true,false,false)
     * Regular_expression('User defined','sak_(.*)_.*$',true,true,false,false,false,false,'List capture groups')
     * SHA1(80)
     * To_Base58('123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz')
     * Regular_expression('User defined','^.{7}',true,true,false,false,false,false,'List matches')
     * Regular_expression('User defined','^$R0$',true,true,false,false,false,false,'Highlight matches')
     * </pre>
     */
    public String generateRandomApiKey() {
        // This is the meat of the API key. A random string of chars using the
        // base58 character set. THis is so we have a nice simple set of readable chars
        // without visually similar ones like '0OIl'
        final String randomCode = StringUtil.createRandomCode(
                secureRandom,
                API_KEY_RANDOM_CODE_LENGTH,
                StringUtil.ALLOWED_CHARS_BASE_58_STYLE);

        // Generate a short hash of the randomCode
        final String hash = computeHash(randomCode);
        return String.join(API_KEY_SEPARATOR, API_KEY_PREFIX, randomCode, hash);
    }

    private static String computeHash(final String randomStr) {
        // Now get a sha1 hash of our random string, encoded in base58 and truncated to 7 chars.
        // This part acts as a checksum of the random code part and provides a means to verify that
        // something that looks like a stroom API key is actually one, e.g. for spotting stroom API keys
        // left in the clear. We may never use this checksum, but you never know.
        // See https://github.blog/2021-04-05-behind-githubs-new-authentication-token-formats/
        // for the idea behind this.
        // This is the bitcoin style base58, not flickr or other flavours.
        final String sha1 = DigestUtils.sha1Hex(randomStr);
        return Base58.encode(sha1.getBytes(StandardCharsets.UTF_8))
                .substring(0, TRUNCATED_HASH_LENGTH);
    }

    /**
     * Asserts that the passed string matches the format of a stroom API key and that the hash part of
     * the key matches the hash computed from the random code part.
     * Note, this method is NOT to be used for authenticating
     * an API key, for that see {@link ApiKeyService#fetchVerifiedIdentity(String)}. It simply verifies
     * that a string is very likely to be a stroom API key (whether valid or not).
     * It can be used to see if a string passed in the {@code Authorization: Bearer ...} header is a stroom
     * API key before looking in the database to check its validity.
     *
     * @return True if the hash part of the API key matches a computed hash of the random
     * code part of the API key and the string matches the pattern for an API key.
     */
    public boolean isApiKey(final String apiKey) {
        LOGGER.debug("apiKey: '{}'", apiKey);
        if (NullSafe.isBlankString(apiKey)) {
            return false;
        } else {
            final String trimmedApiKey = apiKey.trim();
            if (trimmedApiKey.length() != API_KEY_TOTAL_LENGTH) {
                LOGGER.debug(() -> LogUtil.message("Invalid length: {}", trimmedApiKey.length()));
                return false;
            }
            if (!API_KEY_MATCH_PREDICATE.test(trimmedApiKey)) {
                LOGGER.debug("Doesn't match pattern");
                return false;
            }
            final String[] parts = API_KEY_SEPARATOR_PATTERN.split(apiKey.trim());
            if (parts.length != 3) {
                LOGGER.debug("Incorrect number of parts: '{}', parts:", (Object) parts);
                return false;
            }
            final String codePart = parts[1];
            final String hashPart = parts[2];
            final String computedHash = computeHash(codePart);
            if (!Objects.equals(hashPart, computedHash)) {
                LOGGER.debug("Hashes don't match, hashPart: '{}', computedHash: '{}'", hashPart, computedHash);
                return false;
            }
            return true;
        }
    }
}
