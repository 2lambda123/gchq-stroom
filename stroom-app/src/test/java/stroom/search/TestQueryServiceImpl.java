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


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.dashboard.DashboardStore;
import stroom.dashboard.QueryHistoryCleanExecutor;
import stroom.dashboard.QueryService;
import stroom.dashboard.shared.DashboardDoc;
import stroom.dashboard.shared.FindQueryCriteria;
import stroom.dashboard.shared.QueryEntity;
import stroom.docref.DocRef;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.Sort.Direction;
import stroom.entity.util.BaseEntityDeProxyProcessor;
import stroom.index.IndexStore;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.query.api.v2.Query;
import stroom.test.AbstractCoreIntegrationTest;

import javax.inject.Inject;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TestQueryServiceImpl extends AbstractCoreIntegrationTest {
    private static final String QUERY_COMPONENT = "Test Component";
    private static Logger LOGGER = LoggerFactory.getLogger(TestQueryServiceImpl.class);

    @Inject
    private DashboardStore dashboardStore;
    @Inject
    private QueryService queryService;
    @Inject
    private IndexStore indexStore;
    @Inject
    private QueryHistoryCleanExecutor queryHistoryCleanExecutor;

    private DashboardDoc dashboard;
    private QueryEntity testQuery;
    private QueryEntity refQuery;

    @Override
    protected void onBefore() {
        // need an explicit teardown and setup of the DB before each test method
        clean();

        final DocRef dashboardRef = dashboardStore.createDocument("Test");
        dashboard = dashboardStore.readDocument(dashboardRef);

        final DocRef indexRef = indexStore.createDocument("Test index");

        refQuery = queryService.create("Ref query");
        refQuery.setDashboardUuid(dashboard.getUuid());
        refQuery.setQueryId(QUERY_COMPONENT);
        refQuery.setQuery(new Query(indexRef, new ExpressionOperator(null, Op.AND, Collections.emptyList())));
        queryService.save(refQuery);

        final ExpressionOperator.Builder root = new ExpressionOperator.Builder(Op.OR);
        root.addTerm("Some field", Condition.CONTAINS, "Some value");

        LOGGER.info(root.toString());

        testQuery = queryService.create("Test query");
        testQuery.setDashboardUuid(dashboard.getUuid());
        testQuery.setQueryId(QUERY_COMPONENT);
        testQuery.setQuery(new Query(indexRef, root.build()));
        testQuery = queryService.save(testQuery);

        LOGGER.info(testQuery.getQuery().toString());
    }

    @Test
    void testQueryRetrieval() {
        final FindQueryCriteria criteria = new FindQueryCriteria();
        criteria.setDashboardUuid(dashboard.getUuid());
        criteria.setQueryId(QUERY_COMPONENT);
        criteria.setSort(FindQueryCriteria.FIELD_TIME, Direction.DESCENDING, false);

        final BaseResultList<QueryEntity> list = queryService.find(criteria);

        assertThat(list.size()).isEqualTo(2);

        final QueryEntity query = list.get(0);

        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("Test query");
        assertThat(query.getData()).isNotNull();

        final ExpressionOperator root = query.getQuery().getExpression();

        assertThat(root.getChildren().size()).isEqualTo(1);

        final StringBuilder sb = new StringBuilder();
        sb.append("<expression>\n");
        sb.append("    <op>OR</op>\n");
        sb.append("    <children>\n");
        sb.append("        <term>\n");
        sb.append("            <field>Some field</field>\n");
        sb.append("            <condition>CONTAINS</condition>\n");
        sb.append("            <value>Some value</value>\n");
        sb.append("        </term>\n");
        sb.append("    </children>\n");
        sb.append("</expression>\n");

        String actual = query.getData();
        actual = actual.replaceAll("\\s*", "");
        String expected = sb.toString();
        expected = expected.replaceAll("\\s*", "");
        assertThat(actual.contains(expected)).isTrue();
    }

    @Test
    void testOldHistoryDeletion() {
        final FindQueryCriteria criteria = new FindQueryCriteria();
        criteria.setDashboardUuid(dashboard.getUuid());
        criteria.setQueryId(QUERY_COMPONENT);
        criteria.setSort(FindQueryCriteria.FIELD_TIME, Direction.DESCENDING, false);

        BaseResultList<QueryEntity> list = queryService.find(criteria);
        assertThat(list.size()).isEqualTo(2);

        QueryEntity query = list.get(0);

        // Now insert the same query over 100 times.
        for (int i = 0; i < 120; i++) {
            final QueryEntity newQuery = queryService.create("History");
            newQuery.setDashboardUuid(query.getDashboardUuid());
            newQuery.setQueryId(query.getQueryId());
            newQuery.setFavourite(false);
            newQuery.setQuery(query.getQuery());
            newQuery.setData(query.getData());
            queryService.save(newQuery);
        }

        // Clean the history.
        queryHistoryCleanExecutor.clean(false);

        list = queryService.find(criteria);
        assertThat(list.size()).isEqualTo(100);
    }

    @Test
    void testLoad() {
        QueryEntity query = new QueryEntity();
        query.setId(testQuery.getId());
        query = queryService.load(query);

        assertThat(query).isNotNull();
        assertThat(query.getName()).isEqualTo("Test query");
        assertThat(query.getData()).isNotNull();
        final ExpressionOperator root = query.getQuery().getExpression();
        assertThat(root.getChildren().size()).isEqualTo(1);
    }

//    @Test
//    public void testLoadById() {
//        final QueryEntity query = queryService.loadById(testQuery.getId());
//
//        assertThat(query).isNotNull();
//        assertThat(query.getName()).isEqualTo("Test query");
//        assertThat(query.getMeta()).isNotNull();
//        final ExpressionOperator root = query.getQuery().getExpression();
//        assertThat(root.getChildren().size()).isEqualTo(1);
//    }

    @Test
    void testClientSideStuff1() {
        QueryEntity query = queryService.loadByUuid(refQuery.getUuid());
        query = ((QueryEntity) new BaseEntityDeProxyProcessor(true).process(query));
        queryService.save(query);
    }

    @Test
    void testClientSideStuff2() {
        QueryEntity query = queryService.loadByUuid(testQuery.getUuid());
        query = ((QueryEntity) new BaseEntityDeProxyProcessor(true).process(query));
        queryService.save(query);
    }

//    @Test
//    public void testDeleteKids() {
//        QueryEntity query = queryService.loadByUuid(testQuery.getUuid());
//        ExpressionOperator root = query.getQuery().getExpression();
//        root.remove(0);
//        queryService.save(query);
//
//        query = queryService.loadByUuid(testQuery.getUuid());
//
//        assertThat(query.getName()).isEqualTo("Test query");
//        root = query.getQuery().getExpression();
//        assertThat(root.getChildren()).isNull();
//    }
}
