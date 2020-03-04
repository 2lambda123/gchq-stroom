package stroom.statistics.impl.sql.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.SearchRequest;
import stroom.query.common.v2.DateExpressionParser;
import stroom.statistics.impl.sql.rollup.RollUpBitMask;
import stroom.statistics.impl.sql.shared.StatisticRollUpType;
import stroom.statistics.impl.sql.shared.StatisticStoreDoc;
import stroom.util.Period;
import stroom.util.shared.Range;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatStoreCriteriaBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatStoreCriteriaBuilder.class);

    private static final List<ExpressionTerm.Condition> SUPPORTED_DATE_CONDITIONS = Collections.singletonList(ExpressionTerm.Condition.BETWEEN);

    public static FindEventCriteria buildCriteria(final SearchRequest search, final StatisticStoreDoc dataSource) {

        LOGGER.trace(String.format("buildCriteria called for statistic %s", dataSource.getName()));

        // Get the current time in millis since epoch.
        final long nowEpochMilli = System.currentTimeMillis();

        // object looks a bit like this
        // AND
        // Date Time between 2014-10-22T23:00:00.000Z,2014-10-23T23:00:00.000Z

        final ExpressionOperator topLevelExpressionOperator = search.getQuery().getExpression();

        if (topLevelExpressionOperator == null || topLevelExpressionOperator.getOp() == null) {
            throw new IllegalArgumentException(
                    "The top level operator for the query must be one of [" + ExpressionOperator.Op.values() + "]");
        }

        final List<ExpressionItem> childExpressions = topLevelExpressionOperator.getChildren();
        int validDateTermsFound = 0;
        int dateTermsFound = 0;

        // Identify the date term in the search criteria. Currently we must have
        // a exactly one BETWEEN operator on the
        // datetime
        // field to be able to search. This is because of the way the search in
        // hbase is done, ie. by start/stop row
        // key.
        // It may be possible to expand the capability to make multiple searches
        // but that is currently not in place
        ExpressionTerm dateTerm = null;
        if (childExpressions != null) {
            for (final ExpressionItem expressionItem : childExpressions) {
                if (expressionItem.isEnabled()) {
                    if (expressionItem instanceof ExpressionTerm) {
                        final ExpressionTerm expressionTerm = (ExpressionTerm) expressionItem;

                        if (expressionTerm.getField() == null) {
                            throw new IllegalArgumentException("Expression term does not have a field specified");
                        }

                        if (expressionTerm.getField().equals(StatisticStoreDoc.FIELD_NAME_DATE_TIME)) {
                            dateTermsFound++;

                            if (SUPPORTED_DATE_CONDITIONS.contains(expressionTerm.getCondition())) {
                                dateTerm = expressionTerm;
                                validDateTermsFound++;
                            }
                        }
                    } else if (expressionItem instanceof ExpressionOperator) {
                        if (((ExpressionOperator) expressionItem).getOp() == null) {
                            throw new IllegalArgumentException(
                                "An operator in the query is missing a type, it should be one of " + ExpressionOperator.Op.values());
                        }
                    }
                }
            }
        }

        // ensure we have a date term
        if (dateTermsFound != 1 || validDateTermsFound != 1) {
            throw new UnsupportedOperationException(
                    "Search queries on the statistic store must contain one term using the '"
                            + StatisticStoreDoc.FIELD_NAME_DATE_TIME
                            + "' field with one of the following condtitions [" + SUPPORTED_DATE_CONDITIONS.toString()
                            + "].  Please amend the query");
        }

        // ensure the value field is not used in the query terms
        if (contains(topLevelExpressionOperator, StatisticStoreDoc.FIELD_NAME_VALUE)) {
            throw new UnsupportedOperationException("Search queries containing the field '"
                    + StatisticStoreDoc.FIELD_NAME_VALUE + "' are not supported.  Please remove it from the query");
        }

        // if we have got here then we have a single BETWEEN date term, so parse
        // it.
        final Range<Long> range = extractRange(dateTerm, search.getDateTimeLocale(), nowEpochMilli);

        final List<ExpressionTerm> termNodesInFilter = new ArrayList<>();
        findAllTermNodes(topLevelExpressionOperator, termNodesInFilter);

        final Set<String> rolledUpFieldNames = new HashSet<>();

        for (final ExpressionTerm term : termNodesInFilter) {
            // add any fields that use the roll up marker to the black list. If
            // somebody has said user=* then we do not
            // want that in the filter as it will slow it down. The fact that
            // they have said user=* means it will use
            // the statistic name appropriate for that rollup, meaning the
            // filtering is built into the stat name.
            if (term.getValue().equals(RollUpBitMask.ROLL_UP_TAG_VALUE)) {
                rolledUpFieldNames.add(term.getField());
            }
        }

        if (!rolledUpFieldNames.isEmpty()) {
            if (dataSource.getRollUpType().equals(StatisticRollUpType.NONE)) {
                throw new UnsupportedOperationException(
                        "Query contains rolled up terms but the Statistic Data Source does not support any roll-ups");
            } else if (dataSource.getRollUpType().equals(StatisticRollUpType.CUSTOM)) {
                if (!dataSource.isRollUpCombinationSupported(rolledUpFieldNames)) {
                    throw new UnsupportedOperationException(String.format(
                            "The query contains a combination of rolled up fields %s that is not in the list of custom roll-ups for the statistic data source",
                            rolledUpFieldNames));
                }
            }
        }

        // Date Time is handled spearately to the the filter tree so ignore it
        // in the conversion
        final Set<String> blackListedFieldNames = new HashSet<>();
        blackListedFieldNames.addAll(rolledUpFieldNames);
        blackListedFieldNames.add(StatisticStoreDoc.FIELD_NAME_DATE_TIME);

        final FilterTermsTree filterTermsTree = FilterTermsTreeBuilder
                .convertExpresionItemsTree(topLevelExpressionOperator, blackListedFieldNames);

        final FindEventCriteria criteria = FindEventCriteria.instance(new Period(range.getFrom(), range.getTo()),
                dataSource.getName(), filterTermsTree, rolledUpFieldNames);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Searching statistics store with criteria: %s", criteria.toString()));
        }

        return criteria;
    }

    /**
     * Recursive method to populates the passed list with all enabled
     * {@link ExpressionTerm} nodes found in the tree.
     */
    private static void findAllTermNodes(final ExpressionItem node, final List<ExpressionTerm> termsFound) {
        // Don't go any further down this branch if this node is disabled.
        if (node.isEnabled()) {
            if (node instanceof ExpressionTerm) {
                final ExpressionTerm termNode = (ExpressionTerm) node;

                termsFound.add(termNode);

            } else if (node instanceof ExpressionOperator) {
                for (final ExpressionItem childNode : ((ExpressionOperator) node).getChildren()) {
                    findAllTermNodes(childNode, termsFound);
                }
            }
        }
    }

    private static boolean contains(final ExpressionItem expressionItem, final String fieldToFind) {
        boolean hasBeenFound = false;

        if (expressionItem instanceof ExpressionOperator) {
            if (((ExpressionOperator) expressionItem).getChildren() != null) {
                for (final ExpressionItem item : ((ExpressionOperator) expressionItem).getChildren()) {
                    hasBeenFound = contains(item, fieldToFind);
                    if (hasBeenFound) {
                        break;
                    }
                }
            }
        } else {
            if (((ExpressionTerm) expressionItem).getField() != null) {
                hasBeenFound = ((ExpressionTerm) expressionItem).getField().equals(fieldToFind);
            }
        }

        return hasBeenFound;
    }

    private static Range<Long> extractRange(final ExpressionTerm dateTerm, final String timeZoneId, final long nowEpochMilli) {
        long rangeFrom = 0;
        long rangeTo = Long.MAX_VALUE;

        final String[] dateArr = dateTerm.getValue().split(",");

        if (dateArr.length != 2) {
            throw new RuntimeException("DateTime term is not a valid format, term: " + dateTerm.toString());
        }

        rangeFrom = parseDateTime("from", dateArr[0], timeZoneId, nowEpochMilli);
        // add one to make it exclusive
        rangeTo = parseDateTime("to", dateArr[1], timeZoneId, nowEpochMilli) + 1;

        final Range<Long> range = new Range<>(rangeFrom, rangeTo);

        return range;
    }

    private static long parseDateTime(final String type, final String value, final String timeZoneId, final long nowEpochMilli) {
        final ZonedDateTime dateTime;
        try {
            dateTime = DateExpressionParser.parse(value, timeZoneId, nowEpochMilli)
                    .orElseThrow(() -> new RuntimeException(
                            "DateTime term has an invalid '" + type + "' value of '" + value + "'"));
        } catch (final Exception e) {
            throw new RuntimeException("DateTime term has an invalid '" + type + "' value of '" + value + "'");
        }

        return dateTime.toInstant().toEpochMilli();
    }
}
