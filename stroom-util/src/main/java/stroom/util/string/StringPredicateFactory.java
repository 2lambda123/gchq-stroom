package stroom.util.string;

import stroom.util.ConsoleColour;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Useful methods to create various {@link Predicate<String>}
 */
public class StringPredicateFactory {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(StringPredicateFactory.class);

    // Treat brackets as word separators, e.g. "Events (XML)"
    private static final Pattern DEFAULT_SEPARATOR_CHAR_CLASS = Pattern.compile("[ _\\-()\\[\\].]");

    private static final Pattern CASE_INSENS_WORD_LETTER_CHAR_CLASS = Pattern.compile("[a-z0-9]");

    // Matches a whole string that is lowerCamelCase or UpperCamelCase
    // It is debatable if we should instead look for the absence of a separator as that may
    // be easier
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile(
            "^([A-Z]+)?[a-z0-9]+(?:(?:\\d)|(?:[A-Z0-9]+[a-z0-9]+))*(?:[A-Z]+)?$");

    // Matches positions in (C|c)amelCase to split into individual words
    // Doesn't cope with abbreviations at the beginning/middle of the string,
    // e.g. SQLScript or SomeSQLScript
    // Pattern also splits on a space to allow us to pre-split the string a bit
    private static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile(
            "((?<=[a-z])(?=[A-Z])|(?<=[0-9])(?=[A-Z])|(?<=[a-zA-Z])(?=[0-9])| |\\.)");
    private static final Pattern CAMEL_CASE_ABBREVIATIONS_PATTERN = Pattern.compile("([A-Z]+)([A-Z][a-z0-9])");

    public static final String WILDCARD_STR = "*";
    public static final char NOT_OPERATOR_CHAR = '!';
    public static final String NOT_OPERATOR_STR = Character.toString(NOT_OPERATOR_CHAR);

    // Static util methods only
    private StringPredicateFactory() {
    }

    /**
     * @see StringPredicateFactory#createFuzzyMatchPredicate(String, Pattern)
     */
    public static Predicate<String> createFuzzyMatchPredicate(final String userInput) {
        return createFuzzyMatchPredicate(userInput, DEFAULT_SEPARATOR_CHAR_CLASS);
    }

    public static <T> Predicate<T> createFuzzyMatchPredicate(final String userInput,
                                                             final Function<T, String> valueExtractor) {

        final Predicate<String> stringPredicate = createFuzzyMatchPredicate(userInput);
        return toNullSafePredicate(false,
                (T obj) -> {
                    final String valueUnderTest = valueExtractor.apply(obj);
                    if (valueUnderTest == null) {
                        return false;
                    } else {
                        return stringPredicate.test(valueUnderTest);
                    }
                });
    }

    /**
     * Creates a fuzzy match {@link Predicate<String>} for userInput.
     * Null userInput results in an always true predicate.
     * Broadly it has five match modes:
     * Regex match: "/(wo|^)man" matches "a woman", "manly"
     * Word boundary match: "?OTheiM" matches "on the mat" in "the cat sat on their mat", but not
     * "the cat sat on there mat"
     * Starts with: "^prefix" matches "PrefixToSomeText" (case insensitive)
     * Ends with "suffix$" matches "TextWithSuffix" (case insensitive)
     * Exact match: "^sometext$" matches "sometext" (case insensitive)
     * Chars anywhere (in order): "aid" matches "A big dog" (case insensitive)
     * See TestStringPredicateFactory for more examples of how the
     * matching works.
     *
     * @param separatorCharacterClass A regex character class, e.g. [ \-_] that defines the separators
     *                                between words in the string(s) under test.
     */
    public static Predicate<String> createFuzzyMatchPredicate(final String userInput,
                                                              final Pattern separatorCharacterClass) {
        // TODO should we trim the input to remove leading/trailing spaces?

        LOGGER.trace("Creating predicate for userInput [{}] and separators {}", userInput, separatorCharacterClass);

        String modifiedInput = userInput;
        boolean isNegated = false;

        Predicate<String> predicate;
        if (modifiedInput == null || modifiedInput.isEmpty()) {
            LOGGER.trace("Creating null input predicate");
            // No input so get everything
            predicate = stringUnderTest -> true;
        } else {
            if (modifiedInput.startsWith(NOT_OPERATOR_STR)) {
                modifiedInput = modifiedInput.substring(1);
                LOGGER.debug("Input after NOT operator removal [{}]", modifiedInput);
                isNegated = true;
            }

            if (modifiedInput.isEmpty()) {
                LOGGER.trace("Creating null input predicate");
                // No input so get everything
                predicate = stringUnderTest -> true;
            } else if (modifiedInput.startsWith("/")) {
                // We must test for this prefix first as you might have '/foobar$' which could be confused
                // with ends with matching
                // remove the / marker char from the beginning
                predicate = createRegexPredicate(modifiedInput.substring(1));
            } else if (modifiedInput.startsWith("~")) {
                // remove the ~ marker char from the beginning
                predicate = createCharsAnywherePredicate(modifiedInput.substring(1));
            } else if (modifiedInput.startsWith("?")) {
                // remove the ? marker char from the beginning
                predicate = createWordBoundaryPredicate(modifiedInput.substring(1), separatorCharacterClass);
            } else if (modifiedInput.startsWith("^") && modifiedInput.endsWith("$")) {
                predicate = createCaseInsensitiveExactMatchPredicate(modifiedInput);
            } else if (modifiedInput.endsWith("$")) {
                // remove the $ marker char from the end
                predicate = createCaseInsensitiveEndsWithPredicate(modifiedInput.substring(0,
                        modifiedInput.length() - 1));
            } else if (modifiedInput.startsWith("^")) {
                // remove the ^ marker char from the beginning
                predicate = createCaseInsensitiveStartsWithPredicate(modifiedInput.substring(1));
            } else if (modifiedInput.contains(WILDCARD_STR)) {
                // Think this is for feed name input fields where the user is allowed to enter the feed
                // name in wild carded form, such that the processor filter will apply to any feeds matching
                // that wildcarded form at runtime. This predicate makes the suggestion dropdown show the user
                // what the term would match at that point.
                // Not ideal as it is complete match and case sens which is inconsistent with the rest and a bit
                // magic.
                predicate = createWildCardedPredicate(modifiedInput);
            } else {
                // Would be nice to use chars anywhere for the default but that needs ranked matches which
                // we can't do when filtering the trees
                predicate = createCaseInsensitiveContainsPredicate(modifiedInput);
            }
        }

        if (isNegated) {
            predicate = predicate.negate();
            LOGGER.debug("Negating predicate");
        }

        if (LOGGER.isTraceEnabled()) {
            return toLoggingPredicate(predicate);
        } else {
            return predicate;
        }
    }

    /**
     * Wraps the passed {@link Predicate} with one that returns result
     * if the value under test is null
     */
    public static <T> Predicate<T> toNullSafePredicate(final boolean resultIfNull,
                                                       final Predicate<T> predicate) {
        return obj -> {
            if (obj == null) {
                return resultIfNull;
            } else {
                return predicate.test(obj);
            }
        };
    }

    public static Predicate<String> toLoggingPredicate(final Predicate<String> predicate) {
        return str -> {
            boolean result = predicate.test(str);
            final ConsoleColour colour = result
                    ? ConsoleColour.GREEN
                    : ConsoleColour.RED;

            String msg = ConsoleColour.colourise(LogUtil.message("String under test [{}], result: {}",
                    str, result), colour);
            LOGGER.trace(msg);
            return result;
        };
    }

    @NotNull
    public static Predicate<String> createCaseInsensitiveStartsWithPredicate(final String userInput) {
        LOGGER.trace("Creating case insensitive starts with predicate");
        // remove the ^ marker char
        final String lowerCaseInput = userInput.toLowerCase();
        return toNullSafePredicate(false, stringUnderTest ->
                stringUnderTest.toLowerCase().startsWith(lowerCaseInput));
    }

    @NotNull
    public static Predicate<String> createCaseInsensitiveEndsWithPredicate(final String userInput) {
        LOGGER.trace("Creating case insensitive ends with predicate");
        final String lowerCaseInput = userInput.toLowerCase();
        return toNullSafePredicate(false, stringUnderTest ->
                stringUnderTest.toLowerCase().endsWith(lowerCaseInput));
    }

    public static Predicate<String> createCaseInsensitiveContainsPredicate(final String userInput) {
        if (userInput == null) {
            return stringUnderTest -> true;
        } else {
            final String lowerCaseInput = userInput.toLowerCase();
            return toNullSafePredicate(false, stringUnderTest ->
                    stringUnderTest.toLowerCase().contains(lowerCaseInput));
        }
    }

    public static Predicate<String> createRegexPredicate(final String userInput) {
        LOGGER.trace("Creating regex predicate for {}", userInput);
        Pattern pattern;
        try {
            pattern = Pattern.compile(userInput, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            LOGGER.trace(() ->
                    LogUtil.message("Invalid pattern {}, due to {}", userInput, e.getMessage()));
            // Bad pattern, can't really raise an exception as the user may have just mis-typed
            // so just return a false predicate
            return str -> false;
        }

        final Predicate<String> predicate;
        try {
            predicate = pattern.asPredicate();
        } catch (Exception e) {
            LOGGER.trace(() ->
                    LogUtil.message("Error converting pattern {} to predicate, due to {}", userInput, e.getMessage()));
            return str -> false;
        }
        return toNullSafePredicate(false, predicate);
    }

    @NotNull
    private static Predicate<String> createWordBoundaryPredicate(
            final String userInput,
            final Pattern separatorCharacterClass) {

        LOGGER.trace("creating word boundary predicate");
        // Has some uppercase so use word boundaries
        // An upper case letter means the start of a word
        // a lower case letter means the continuation of a word

        // We can use the separator based predicate for both camel case and separated
        // strings as long as we modify the camel case ones first.

        final Predicate<String> separatorPredicate = createSeparatedWordBoundaryPredicate(
                userInput, separatorCharacterClass);

        return toNullSafePredicate(false, stringUnderTest -> {

            // First split the string being tested on the separators, then see if any of the
            // parts are camel case and if so add spaces to split the camel case parts.
            // e.g. stroom.someProp.maxFileSize => "stroom some prop max file size"
            // This allows us to deal with strings that are a mix of delimited and camel case.
            final String[] separatedParts = separatorCharacterClass.split(stringUnderTest);
            final String cleanedString = Arrays.stream(separatedParts)
                    .map(StringPredicateFactory::cleanStringForWordBoundaryMatching)
                    .collect(Collectors.joining(" "));

            LOGGER.trace("cleaned stringUnderTest [{}] => [{}] has word separators",
                    stringUnderTest, cleanedString);
            return separatorPredicate.test(cleanedString);
        });
    }

    private static String cleanStringForWordBoundaryMatching(final String str) {
        if (CAMEL_CASE_PATTERN.matcher(str).matches()) {
            LOGGER.trace("str [{}] is (C|c)amelCase", str);

            // replace stuff like SQLScript with "SQL Script"
            String separatedStr = CAMEL_CASE_ABBREVIATIONS_PATTERN
                    .matcher(str)
                    .replaceAll("$1 $2");

            LOGGER.trace("separatedStr: [{}]", separatedStr);

            // Now split on camel case word boundaries (or spaces added above)
            separatedStr = CAMEL_CASE_SPLIT_PATTERN
                    .matcher(separatedStr)
                    .replaceAll(" ");

            LOGGER.trace("separatedStr: [{}]", separatedStr);

            return separatedStr;
        } else {
            return str;
        }
    }

    @NotNull
    private static Predicate<String> createSeparatedWordBoundaryPredicate(
            final String userInput,
            final Pattern separatorCharacterClass) {

        // Has some uppercase so use word boundaries
        // An upper case letter means the start of a word
        // a lower case letter means the continuation of a word
        // A digit after a letter means the start of a word
        // A digit after a digit means the continuation of a word.

        final StringBuilder patternBuilder = new StringBuilder();
        char lastChr = 0;
        for (int i = 0; i < userInput.length(); i++) {
            char chr = userInput.charAt(i);

            if (Character.isUpperCase(chr)
                    || (Character.isDigit(chr) && Character.isLetter(lastChr))) {
                if (i == 0) {
                    // First letter so is either preceded by ^ or by a separator
                    patternBuilder
                            .append("(?:^|") // non-capturing
                            .append(separatorCharacterClass)
                            .append(")");
                } else {
                    // Not the first letter so need the end of the previous word
                    // and a word separator
                    patternBuilder
                            .append(CASE_INSENS_WORD_LETTER_CHAR_CLASS)
                            .append("*")
                            .append(separatorCharacterClass)
                            .append("+"); // one of more separators
                }
                // Start of a word
            }

            if (Character.isLetterOrDigit(chr)) {
                patternBuilder.append(chr);
            } else {
                // Might be a special char so escape it
                patternBuilder.append(Pattern.quote(String.valueOf(chr)));
            }
            lastChr = chr;
        }
        final Pattern pattern = Pattern.compile(
                patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
        LOGGER.trace("Using separated word pattern: {} with separators {}",
                pattern, separatorCharacterClass);

        return pattern.asPredicate();
    }

    @NotNull
    private static Predicate<String> createCamelCaseWordBoundaryPredicate(
            final String userInput) {

        // Has some uppercase so use word boundaries
        // An upper case letter means the start of a word
        // a lower case letter means the continuation of a word

        final StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < userInput.length(); i++) {
            char chr = userInput.charAt(i);

            if (Character.isUpperCase(chr)) {
                if (i == 0) {
                    // First letter so is either preceded by ^ or
                    // by the end of the previous word
                    patternBuilder
                            .append("(?:^|[a-z0-9]") // non-capturing, assume numbers part of prev word
                            .append(")");
                } else {
                    // Not the first letter so need the end of the previous word
                    // and a word separator
                    patternBuilder
                            .append("[a-z0-9]")
                            .append("*");
                }
                // Start of a word
            }

            if (Character.isLetterOrDigit(chr)) {
                patternBuilder.append(chr);
            } else {
                // Might be a special char so escape it
                patternBuilder.append(Pattern.quote(String.valueOf(chr)));
            }
        }
        final Pattern pattern = Pattern.compile(patternBuilder.toString());
        LOGGER.trace("Using (C|c)amelCase separated pattern: {}", pattern);
        return toNullSafePredicate(false, pattern.asPredicate());
    }

    @NotNull
    private static Predicate<String> createCharsAnywherePredicate(final String userInput) {
        LOGGER.trace("Creating chars appear anywhere in correct order predicate");
        // All lower case so match on each char appearing somewhere in the text
        // in the correct order
        final String lowerCaseInput = userInput.toLowerCase();
        final StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < lowerCaseInput.length(); i++) {
            patternBuilder.append(".*?"); // no-greedy match all

            char chr = userInput.charAt(i);
            if (chr == '*') {
                patternBuilder.append(".*?"); // no-greedy match all
            } else if (Character.isLetterOrDigit(chr)) {
                patternBuilder.append(chr);
            } else {
                // Might be a special char so escape it
                patternBuilder.append(Pattern.quote(String.valueOf(chr)));
            }
        }
        patternBuilder.append(".*?");
        final Pattern pattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
        LOGGER.trace("Using case insensitive pattern: {}", pattern);
        return toNullSafePredicate(false, pattern.asPredicate());
    }

    @NotNull
    private static Predicate<String> createWildCardedPredicate(final String userInput) {
        LOGGER.trace("Creating case sensitive wild-carded predicate");
        // Like a case sensitive exact match but with wildcards
        final StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < userInput.length(); i++) {

            char chr = userInput.charAt(i);
            if (chr == '*') {
                patternBuilder.append(".*?"); // no-greedy match all
            } else if (Character.isLetterOrDigit(chr)) {
                patternBuilder.append(chr);
            } else {
                // Might be a special char so escape it
                patternBuilder.append(Pattern.quote(String.valueOf(chr)));
            }
        }
        final Pattern pattern = Pattern.compile(patternBuilder.toString());
        LOGGER.trace("Using pattern: {}", pattern);
        // Use asMatchPredicate rather than asPredicate so we match on the full string
        return toNullSafePredicate(false, pattern.asMatchPredicate());
    }


    @NotNull
    private static Predicate<String> createCaseInsensitiveExactMatchPredicate(final String userInput) {
        LOGGER.trace("Creating case insensitive exact match predicate");
        final String lowerCaseInput = userInput.substring(1)
                .substring(0, userInput.length() - 2);
        return toNullSafePredicate(false, stringUnderTest ->
                stringUnderTest.toLowerCase().equalsIgnoreCase(lowerCaseInput));
    }

    private static boolean isAllLowerCase(final String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
