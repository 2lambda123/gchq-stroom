/*
 * Copyright 2018 Crown Copyright
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

package stroom.policy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.datasource.api.v2.DataSourceField;
import stroom.dictionary.server.DictionaryStore;
import stroom.entity.server.util.PreparedStatementUtil;
import stroom.entity.server.util.SqlBuilder;
import stroom.entity.shared.Period;
import stroom.entity.shared.Range;
import stroom.feed.shared.Feed;
import stroom.pipeline.shared.PipelineEntity;
import stroom.policy.server.DataRetentionExecutor.ActiveRules;
import stroom.policy.server.DataRetentionExecutor.Progress;
import stroom.ruleset.shared.DataRetentionRule;
import stroom.streamstore.server.ExpressionMatcher;
import stroom.streamstore.shared.Stream;
import stroom.streamstore.shared.StreamDataSource;
import stroom.streamstore.shared.StreamStatus;
import stroom.streamstore.shared.StreamType;
import stroom.streamtask.shared.StreamProcessor;
import stroom.util.task.TaskMonitor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataRetentionStreamFinder implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataRetentionStreamFinder.class);

    private final Connection connection;
    private final DictionaryStore dictionaryStore;

    private PreparedStatement preparedStatement;

    public DataRetentionStreamFinder(final Connection connection, final DictionaryStore dictionaryStore) {
        Objects.requireNonNull(connection, "No connection");
        this.connection = connection;
        this.dictionaryStore = dictionaryStore;
    }

    public long getRowCount(final Period ageRange, final Set<String> fieldSet) throws SQLException {
        long rowCount = 0;
        final SqlBuilder sql = getSelectSql(ageRange, null, fieldSet, true, null);
        try (final PreparedStatement preparedStatement = connection.prepareStatement(sql.toString(),
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT)) {
            PreparedStatementUtil.setArguments(preparedStatement, sql.getArgs());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing SQL '" + sql.toString() + "'\n\twith arguments " + sql.getArgs().toString());
            }

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rowCount = resultSet.getLong(1);
                }
            }
        }
        return rowCount;
    }

    public boolean findMatches(final Period ageRange, final Range<Long> streamIdRange, final long batchSize, final ActiveRules activeRules, final Map<DataRetentionRule, Optional<Long>> ageMap, final TaskMonitor taskMonitor, final Progress progress, final List<Long> matches) throws SQLException {
        boolean more = false;

        final ExpressionMatcher expressionMatcher = new ExpressionMatcher(StreamDataSource.getFieldMap(), dictionaryStore);

        final SqlBuilder sqlBuilder = getSelectSql(ageRange, streamIdRange, activeRules.getFieldSet(), false, batchSize);
        final String sql = sqlBuilder.toString();

        // Reuse this prepared statement as the SQL will not change between calls.
        if (preparedStatement == null) {
            preparedStatement = connection.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY,
                    ResultSet.CLOSE_CURSORS_AT_COMMIT);
        }

        preparedStatement.clearParameters();
        PreparedStatementUtil.setArguments(preparedStatement, sqlBuilder.getArgs());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing SQL '" + sql + "'\n\twith arguments " + sqlBuilder.getArgs().toString());
        }

        try (final ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next() && !taskMonitor.isTerminated()) {
                final Map<String, Object> attributeMap = createAttributeMap(resultSet, activeRules.getFieldSet());
                final Long streamId = (Long) attributeMap.get(StreamDataSource.STREAM_ID);
                final Long createMs = (Long) attributeMap.get(StreamDataSource.CREATE_TIME);
                try {
                    more = true;
                    progress.nextStream(streamId, createMs);
                    final String streamInfo = progress.toString();
                    info(taskMonitor, "Examining stream " + streamInfo);

                    final DataRetentionRule matchingRule = findMatchingRule(expressionMatcher, attributeMap, activeRules.getActiveRules());
                    if (matchingRule != null) {
                        ageMap.get(matchingRule).ifPresent(age -> {
                            if (createMs < age) {
                                LOGGER.debug("Adding match " + streamId);
                                matches.add(streamId);
                            }
                        });
                    }
                } catch (final Exception e) {
                    LOGGER.error("An error occurred processing stream " + streamId, e);
                }
            }
        }

        return more;
    }

    private void info(final TaskMonitor taskMonitor, final String message) {
        LOGGER.debug(message);
        taskMonitor.info(message);
    }

    private SqlBuilder getSelectSql(final Period ageRange, final Range<Long> streamIdRange, final Set<String> fieldSet, final boolean count, final Long limit) {
        final SqlBuilder sql = new SqlBuilder();
        sql.append("SELECT");

        final boolean includeStream = addFieldsToQuery(StreamDataSource.getStreamFields(), fieldSet, sql, "S");
        final boolean includeFeed = addFieldsToQuery(StreamDataSource.getFeedFields(), fieldSet, sql, "F");
        final boolean includeStreamType = addFieldsToQuery(StreamDataSource.getStreamTypeFields(), fieldSet, sql, "ST");
        final boolean includePipeline = addFieldsToQuery(StreamDataSource.getPipelineFields(), fieldSet, sql, "P");

        if (count) {
            sql.setLength(0);
            sql.append("SELECT COUNT(*)");
        } else {
            // Remove last comma from field list.
            sql.setLength(sql.length() - 1);
        }

        sql.append(" FROM ");
        sql.append(Stream.TABLE_NAME);
        sql.append(" S");
        sql.append(" USE INDEX (PRIMARY)");

        if (includeFeed) {
            sql.join(Feed.TABLE_NAME, "F", "S", Feed.FOREIGN_KEY, "F", Feed.ID);
        }
        if (includeStreamType) {
            sql.join(StreamType.TABLE_NAME, "ST", "S", StreamType.FOREIGN_KEY, "ST", StreamType.ID);
        }
        if (includePipeline) {
            sql.leftOuterJoin(StreamProcessor.TABLE_NAME, "SP", "S", StreamProcessor.FOREIGN_KEY, "SP", StreamProcessor.ID);
            sql.leftOuterJoin(PipelineEntity.TABLE_NAME, "p", "SP", PipelineEntity.FOREIGN_KEY, "p", PipelineEntity.ID);
        }

        sql.append(" WHERE 1=1");
        sql.appendRangeQuery("S." + Stream.CREATE_MS, ageRange);
        sql.appendRangeQuery("S." + Stream.ID, streamIdRange);
        sql.appendValueQuery("S." + Stream.STATUS, StreamStatus.UNLOCKED.getPrimitiveValue());
        sql.append(" ORDER BY S." + Stream.ID);
        if (limit != null) {
            sql.append(" LIMIT ");
            sql.arg(limit);
        }

        return sql;
    }

    private DataRetentionRule findMatchingRule(final ExpressionMatcher expressionMatcher, final Map<String, Object> attributeMap, final List<DataRetentionRule> activeRules) {
        try {
            for (final DataRetentionRule rule : activeRules) {
                if (expressionMatcher.match(attributeMap, rule.getExpression())) {
                    return rule;
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
        }

        return null;
    }

    private Map<String, Object> createAttributeMap(final ResultSet resultSet, final Set<String> fieldSet) {
        final Map<String, Object> attributeMap = new HashMap<>();
        fieldSet.forEach(fieldName -> {
            try {
                final DataSourceField field = StreamDataSource.getFieldMap().get(fieldName);
                switch (field.getType()) {
                    case FIELD:
                        final String string = resultSet.getString(fieldName);
                        attributeMap.put(fieldName, string);
                        break;
                    case DOC_REF:
                        final String name = resultSet.getString(fieldName);
                        attributeMap.put(fieldName, name);
                        break;
                    case DATE_FIELD:
                    case ID:
                    case NUMERIC_FIELD:
                        final long number = resultSet.getLong(fieldName);
                        attributeMap.put(fieldName, number);
                        break;

                }
            } catch (final SQLException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
        return attributeMap;
    }

    private boolean addFieldsToQuery(final Map<String, String> fieldMap, final Set<String> fieldSet, final SqlBuilder sql, final String alias) {
        final AtomicBoolean used = new AtomicBoolean();

        fieldMap.forEach((k, v) -> {
            if (fieldSet.contains(k)) {
                sql.append(" ");
                sql.append(alias);
                sql.append(".");
                sql.append(v);
                sql.append(" AS ");
                sql.append("'" + k + "'");
                sql.append(",");

                used.set(true);
            }
        });

        return used.get();
    }

    private void closePreparedStatement() {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                LOGGER.error("Error closing preparedStatement", e);
            }
            preparedStatement = null;
        }
    }

    @Override
    public void close() {
        closePreparedStatement();
    }
}