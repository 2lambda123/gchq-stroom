/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.expression.matcher;

import stroom.collection.api.CollectionService;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.DocRefField;
import stroom.datasource.api.v2.FieldTypes;
import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.query.common.v2.DateExpressionParser;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ExpressionMatcher {
    private static final String DELIMITER = ",";

    private final Map<String, AbstractField> fieldMap;
    private final WordListProvider wordListProvider;
    private final CollectionService collectionService;
    private final Map<DocRef, String[]> wordMap = new HashMap<>();
    private final Map<String, Pattern> patternMap = new HashMap<>();
    private final String timeZoneId;
    private final long nowEpochMilli;

    public ExpressionMatcher(final Map<String, AbstractField> fieldMap) {
        this.fieldMap = fieldMap;
        this.wordListProvider = null;
        this.collectionService = null;
        this.timeZoneId = ZoneOffset.UTC.getId();
        this.nowEpochMilli = System.currentTimeMillis();
    }

    public ExpressionMatcher(final Map<String, AbstractField> fieldMap,
                             final WordListProvider wordListProvider,
                             final CollectionService collectionService,
                             final String timeZoneId,
                             final long nowEpochMilli) {
        this.fieldMap = fieldMap;
        this.wordListProvider = wordListProvider;
        this.collectionService = collectionService;
        this.timeZoneId = timeZoneId;
        this.nowEpochMilli = nowEpochMilli;
    }

    public boolean match(final Map<String, Object> attributeMap, final ExpressionItem item) {
        // If the initial item is null or not enabled then don't match.
        if (item == null || !item.enabled()) {
            return false;
        }
        return matchItem(attributeMap, item);
    }

    private boolean matchItem(final Map<String, Object> attributeMap, final ExpressionItem item) {
        if (!item.enabled()) {
            // If the child item is not enabled then return and keep trying to match with other parts of the expression.
            return true;
        }

        if (item instanceof ExpressionOperator) {
            return matchOperator(attributeMap, (ExpressionOperator) item);
        } else if (item instanceof ExpressionTerm) {
            return matchTerm(attributeMap, (ExpressionTerm) item);
        } else {
            throw new MatchException("Unexpected item type");
        }
    }

    private boolean matchOperator(final Map<String, Object> attributeMap, final ExpressionOperator operator) {
        if (operator.getChildren() == null || operator.getChildren().size() == 0) {
            return true;
        }

        switch (operator.op()) {
            case AND:
                for (final ExpressionItem child : operator.getChildren()) {
                    if (!matchItem(attributeMap, child)) {
                        return false;
                    }
                }
                return true;
            case OR:
                for (final ExpressionItem child : operator.getChildren()) {
                    if (matchItem(attributeMap, child)) {
                        return true;
                    }
                }
                return false;
            case NOT:
                return operator.getChildren().size() == 1 && !matchItem(attributeMap, operator.getChildren().get(0));
            default:
                throw new MatchException("Unexpected operator type");
        }
    }

    private boolean matchTerm(final Map<String, Object> attributeMap, final ExpressionTerm term) {
        String termField = term.getField();
        final Condition condition = term.getCondition();
        String termValue = term.getValue();
        final DocRef docRef = term.getDocRef();

        // Clean strings to remove unwanted whitespace that the user may have
        // added accidentally.
        if (termField != null) {
            termField = termField.trim();
        }
        if (termValue != null) {
            termValue = termValue.trim();
        }

        // Try and find the referenced field.
        if (termField == null || termField.length() == 0) {
            throw new MatchException("Field not set");
        }
        final AbstractField field = fieldMap.get(termField);
        if (field == null) {
            throw new MatchException("Field not found in index: " + termField);
        }
        final String fieldName = field.getName();

        // Ensure an appropriate termValue has been provided for the condition type.
        if (Condition.IN_DICTIONARY.equals(condition) ||
                Condition.IN_FOLDER.equals(condition) ||
                Condition.IS_DOC_REF.equals(condition)) {
            if (docRef == null || docRef.getUuid() == null) {
                throw new MatchException("DocRef not set for field: " + termField);
            }
        } else {
            if (termValue == null || termValue.length() == 0) {
                throw new MatchException("Value not set");
            }
        }

        final Object attribute = attributeMap.get(term.getField());

        // Perform null/not null equality if required.
        if (Condition.IS_NULL.equals(condition)) {
            return attribute == null;
        } else if (Condition.IS_NOT_NULL.equals(condition)) {
            return attribute != null;
        }

        if (attribute == null) {
            throw new MatchException("Attribute '" + term.getField() + "' not found");
        }

        // Create a query based on the field type and condition.
        if (field.isNumeric()) {
            switch (condition) {
                case EQUALS: {
                    final long num1 = getNumber(fieldName, attribute);
                    final long num2 = getNumber(fieldName, termValue);
                    return num1 == num2;
                }
                case CONTAINS: {
                    final long num1 = getNumber(fieldName, attribute);
                    final long num2 = getNumber(fieldName, termValue);
                    return num1 == num2;
                }
                case GREATER_THAN: {
                    final long num1 = getNumber(fieldName, attribute);
                    final long num2 = getNumber(fieldName, termValue);
                    return num1 > num2;
                }
                case GREATER_THAN_OR_EQUAL_TO: {
                    final long num1 = getNumber(fieldName, attribute);
                    final long num2 = getNumber(fieldName, termValue);
                    return num1 >= num2;
                }
                case LESS_THAN: {
                    final long num1 = getNumber(fieldName, attribute);
                    final long num2 = getNumber(fieldName, termValue);
                    return num1 < num2;
                }
                case LESS_THAN_OR_EQUAL_TO: {
                    final long num1 = getNumber(fieldName, attribute);
                    final long num2 = getNumber(fieldName, termValue);
                    return num1 <= num2;
                }
                case BETWEEN: {
                    final long[] between = getNumbers(fieldName, termValue);
                    if (between.length != 2) {
                        throw new MatchException("2 numbers needed for between query");
                    }
                    if (between[0] >= between[1]) {
                        throw new MatchException("From number must lower than to number");
                    }
                    final long num = getNumber(fieldName, attribute);
                    return num >= between[0] && num <= between[1];
                }
                case IN:
                    return isNumericIn(fieldName, termValue, attribute);
                case IN_DICTIONARY:
                    return isInDictionary(fieldName, docRef, field, attribute);
                case IN_FOLDER:
                    return isInFolder(fieldName, docRef, field, attribute);
                default:
                    throw new MatchException("Unexpected condition '" + condition.getDisplayValue() + "' for "
                            + field.getType() + " field type");
            }
        } else if (FieldTypes.DATE.equals(field.getType())) {
            switch (condition) {
                case EQUALS: {
                    final long date1 = getDate(fieldName, attribute);
                    final long date2 = getDate(fieldName, termValue);
                    return date1 == date2;
                }
                case CONTAINS: {
                    final long date1 = getDate(fieldName, attribute);
                    final long date2 = getDate(fieldName, termValue);
                    return date1 == date2;
                }
                case GREATER_THAN: {
                    final long date1 = getDate(fieldName, attribute);
                    final long date2 = getDate(fieldName, termValue);
                    return date1 > date2;
                }
                case GREATER_THAN_OR_EQUAL_TO: {
                    final long date1 = getDate(fieldName, attribute);
                    final long date2 = getDate(fieldName, termValue);
                    return date1 >= date2;
                }
                case LESS_THAN: {
                    final long date1 = getDate(fieldName, attribute);
                    final long date2 = getDate(fieldName, termValue);
                    return date1 < date2;
                }
                case LESS_THAN_OR_EQUAL_TO: {
                    final long date1 = getDate(fieldName, attribute);
                    final long date2 = getDate(fieldName, termValue);
                    return date1 <= date2;
                }
                case BETWEEN: {
                    final long[] between = getDates(fieldName, termValue);
                    if (between.length != 2) {
                        throw new MatchException("2 dates needed for between query");
                    }
                    if (between[0] >= between[1]) {
                        throw new MatchException("From date must occur before to date");
                    }
                    final long num = getDate(fieldName, attribute);
                    return num >= between[0] && num <= between[1];
                }
                case IN:
                    return isDateIn(fieldName, termValue, attribute);
                case IN_DICTIONARY:
                    return isInDictionary(fieldName, docRef, field, attribute);
                case IN_FOLDER:
                    return isInFolder(fieldName, docRef, field, attribute);
                default:
                    throw new MatchException("Unexpected condition '" + condition.getDisplayValue() + "' for "
                            + field.getType() + " field type");
            }
        } else {
            switch (condition) {
                case EQUALS:
                    return isStringMatch(termValue, attribute);
                case CONTAINS:
                    return isStringMatch(termValue, attribute);
                case IN:
                    return isIn(fieldName, termValue, attribute);
                case IN_DICTIONARY:
                    return isInDictionary(fieldName, docRef, field, attribute);
                case IN_FOLDER:
                    return isInFolder(fieldName, docRef, field, attribute);
                case IS_DOC_REF:
                    return isDocRef(fieldName, docRef, field, attribute);
                default:
                    throw new MatchException("Unexpected condition '" + condition.getDisplayValue() + "' for "
                            + field.getType() + " field type");
            }
        }
    }

    private boolean isNumericIn(final String fieldName, final Object termValue, final Object attribute) {
        final long num = getNumber(fieldName, attribute);
        final long[] in = getNumbers(fieldName, termValue);
        if (in != null) {
            for (final long n : in) {
                if (n == num) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isDateIn(final String fieldName, final Object termValue, final Object attribute) {
        final long num = getDate(fieldName, attribute);
        final long[] in = getDates(fieldName, termValue);
        if (in != null) {
            for (final long n : in) {
                if (n == num) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isIn(final String fieldName, final Object termValue, final Object attribute) {
        final String[] termValues = termValue.toString().split(" ");
        for (final String tv : termValues) {
            if (isStringMatch(tv, attribute)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStringMatch(final String termValue, final Object attribute) {
        final Pattern pattern = patternMap.computeIfAbsent(termValue, t -> Pattern.compile(t.replaceAll("\\*", ".*")));

        if (attribute instanceof DocRef) {
            final DocRef docRef = (DocRef) attribute;
            if (pattern.matcher(docRef.getUuid()).matches()) {
                return true;
            }
            return pattern.matcher(docRef.getName()).matches();
        }
        return pattern.matcher(attribute.toString()).matches();
    }

    private boolean isInDictionary(final String fieldName, final DocRef docRef,
                                   final AbstractField field, final Object attribute) {
        final String[] lines = loadWords(docRef);
        if (lines != null) {
            for (final String line : lines) {
                if (field.isNumeric()) {
                    if (isNumericIn(fieldName, line, attribute)) {
                        return true;
                    }
                } else if (FieldTypes.DATE.equals(field.getType())) {
                    if (isDateIn(fieldName, line, attribute)) {
                        return true;
                    }
                } else {
                    if (isIn(fieldName, line, attribute)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isInFolder(final String fieldName, final DocRef docRef,
                               final AbstractField field, final Object attribute) {
        if (field instanceof DocRefField) {
            final String type = ((DocRefField) field).getDocRefType();
            if (type != null && collectionService != null) {
                final Set<DocRef> descendants = collectionService.getDescendants(docRef, type);
                if (descendants != null && descendants.size() > 0) {
                    if (attribute instanceof DocRef) {
                        final String uuid = ((DocRef) attribute).getUuid();
                        if (uuid != null) {
                            for (final DocRef descendant : descendants) {
                                if (uuid.equals(descendant.getUuid())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isDocRef(final String fieldName, final DocRef docRef,
                             final AbstractField field, final Object attribute) {
        if (attribute instanceof DocRef) {
            final String uuid = ((DocRef) attribute).getUuid();
            return (null != uuid && uuid.equals(docRef.getUuid()));
        }

        return false;
    }

    private String[] loadWords(final DocRef docRef) {
        if (wordListProvider == null) {
            return null;
        }

        return wordMap.computeIfAbsent(docRef, k -> {
            final String[] words = wordListProvider.getWords(docRef);
            if (words != null) {
                return words;
            }

            return null;
        });
    }

    private long getDate(final String fieldName, final Object value) {
        try {
            if (value instanceof Long) {
                return (Long) value;
            }

            //empty optional will be caught below
            return DateExpressionParser.parse(value.toString(), timeZoneId, nowEpochMilli).get().toInstant().toEpochMilli();
        } catch (final Exception e) {
            throw new MatchException("Expected a standard date value for field \"" + fieldName
                    + "\" but was given string \"" + value + "\"");
        }
    }

    private long[] getDates(final String fieldName, final Object value) {
        final String[] values = value.toString().split(DELIMITER);
        final long[] dates = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            dates[i] = getDate(fieldName, values[i].trim());
        }

        return dates;
    }

    private long getNumber(final String fieldName, final Object value) {
        try {
            if (value instanceof Long) {
                return (Long) value;
            }
            return Long.parseLong(value.toString());
        } catch (final NumberFormatException e) {
            throw new MatchException(
                    "Expected a numeric value for field \"" + fieldName + "\" but was given string \"" + value + "\"");
        }
    }

    private long[] getNumbers(final String fieldName, final Object value) {
        final String[] values = value.toString().split(DELIMITER);
        final long[] numbers = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            numbers[i] = getNumber(fieldName, values[i].trim());
        }

        return numbers;
    }

    private static class MatchException extends RuntimeException {
        MatchException(final String message) {
            super(message);
        }
    }
}
