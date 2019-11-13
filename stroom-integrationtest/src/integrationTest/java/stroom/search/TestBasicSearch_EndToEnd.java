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

package stroom.search;

import org.junit.Assert;
import org.junit.Test;
import stroom.dashboard.shared.DataSourceFieldsMap;
import stroom.datasource.api.v2.DataSourceField;
import stroom.datasource.api.v2.DataSourceField.DataSourceFieldType;
import stroom.index.server.IndexService;
import stroom.index.server.IndexShardService;
import stroom.index.server.IndexShardUtil;
import stroom.index.shared.FindIndexCriteria;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.Index;
import stroom.index.shared.IndexShard;

import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.search.server.IndexDataSourceFieldUtil;
import stroom.test.AbstractCoreIntegrationTest;

import javax.annotation.Resource;
import java.util.List;

public class TestBasicSearch_EndToEnd extends AbstractCoreIntegrationTest {
    @Resource
    private IndexService indexService;
    @Resource
    private IndexShardService indexShardService;
    @Resource
    private CommonIndexingTest commonIndexingTest;

    @Override
    protected boolean doSingleSetup() {
        commonIndexingTest.setup();
        return true;
    }

    @Test
    public void testFindIndexedFields() {
        final Index index = indexService.find(new FindIndexCriteria()).getFirst();

        // Create a map of index fields keyed by name.
        final DataSourceFieldsMap dataSourceFieldsMap = new DataSourceFieldsMap(IndexDataSourceFieldUtil.getDataSourceFields(index, null));
        final DataSourceField actual = dataSourceFieldsMap.get("Action");

        final DataSourceField expected = new DataSourceField.Builder()
                .type(DataSourceFieldType.TEXT_FIELD)
                .name("Action")
                .queryable(true)
                .addConditions(actual.getConditions().toArray(new Condition[0]))
                .build();

        Assert.assertEquals("Expected to index action", expected, actual);
    }

    @Test
    public void testTermQuery() throws Exception {
        final ExpressionOperator.Builder expression = new ExpressionOperator.Builder();
        expression.addTerm("UserId", Condition.EQUALS, "user5");

        test(expression, 1, 5);
    }

    @Test
    public void testPhraseQuery() throws Exception {
        final String field = "Command";

        final ExpressionOperator.Builder expression = new ExpressionOperator.Builder();
        expression.addTerm(field, Condition.EQUALS, "service");
        expression.addTerm(field, Condition.EQUALS, "cwhp");
        expression.addTerm(field, Condition.EQUALS, "authorize");
        expression.addTerm(field, Condition.EQUALS, "deviceGroup");

        test(expression, 1, 23);
    }

    @Test
    public void testBooleanQuery() throws Exception {
        final String field = "Command";
        final ExpressionOperator.Builder expression = new ExpressionOperator.Builder()
                .addOperator(new ExpressionOperator.Builder(Op.AND)
                    .addTerm(field, Condition.EQUALS, "service")
                    .addTerm(field, Condition.EQUALS, "cwhp")
                    .addTerm(field, Condition.EQUALS, "authorize")
                    .addTerm(field, Condition.EQUALS, "deviceGroup")
                    .build())
                .addTerm("UserId", Condition.EQUALS, "user5");
        test(expression, 1, 5);
    }

    @Test
    public void testNestedBooleanQuery() throws Exception {
        // Create an or query.
        final ExpressionOperator.Builder orCondition = new ExpressionOperator.Builder(ExpressionOperator.Op.OR);
        orCondition.addTerm("UserId", Condition.EQUALS, "user6");

        final ExpressionOperator.Builder andCondition = orCondition.addOperator(new ExpressionOperator.Builder(Op.AND)
                .addTerm("UserId", Condition.EQUALS, "user1")
                .build());

        // Check there are 4 events.
        test(andCondition, 1, 4);

        // Create an and query.
        andCondition.addTerm("HostName", Condition.EQUALS, "e6sm01");

        // There should be two events.
        test(andCondition, 1, 2);

        // There should be two events.
        test(orCondition, 1, 2);

        // There should be four events.
        test(orCondition, 1, 4);
    }

    @Test
    public void testRangeQuery() throws Exception {
        final ExpressionOperator.Builder expression = new ExpressionOperator.Builder();
        expression.addTerm("EventTime", Condition.BETWEEN, "2007-08-18T13:21:48.000Z,2007-08-18T13:23:49.000Z");

        test(expression, 1, 2);
    }

    private void test(final ExpressionOperator.Builder expression, final long expectedStreams, final long expectedEvents)
            throws Exception {
        final Index index = indexService.find(new FindIndexCriteria()).getFirst();

        final List<IndexShard> list = indexShardService.find(new FindIndexShardCriteria());
        for (final IndexShard indexShard : list) {
            System.out.println("Using index " + IndexShardUtil.getIndexPath(indexShard));
        }
    }
}
