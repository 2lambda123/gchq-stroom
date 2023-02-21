package stroom.processor.impl.db.migration;

import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

public class TestV07_00_21_005 extends AbstractProcessorMigrationTest {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TestV07_00_21_005.class);

    @Override
    protected String getTestDataScript() {
        final StringBuilder sb = new StringBuilder("""
                insert into processor (
                    version,
                    create_time_ms,
                    create_user,
                    update_time_ms,
                    update_user,
                    uuid,
                    pipeline_uuid)
                values (
                    1,
                    1,
                    "user1",
                    1,
                    "user1",
                    "myUUID",
                    "pipeUUID");
                SET @procId = (SELECT LAST_INSERT_ID());

                insert into processor_filter_tracker (
                    version,
                    min_meta_id,
                    min_event_id)
                values (
                    1,
                    1,
                    1);
                SET @procFilterTrackerId = (SELECT LAST_INSERT_ID());

                insert into processor_filter (
                    version,
                    create_time_ms,
                    create_user,
                    update_time_ms,
                    update_user,
                    uuid,
                    fk_processor_id,
                    fk_processor_filter_tracker_id,
                    data,
                    priority)
                values (
                    1,
                    1,
                    "user1",
                    1,
                    "user1",
                    "myUUID",
                    @procId,
                    @procFilterTrackerId,
                    "my data",
                    10);
                SET @procFilterId = (SELECT LAST_INSERT_ID());

                insert into processor_node (name)
                values ("node1");
                SET @nodeId = (SELECT LAST_INSERT_ID());
                """);

        appendProcTaskSql(sb, "@nodeId", 2); // assigned
        appendProcTaskSql(sb, "@nodeId", 99); // deleted
        appendProcTaskSql(sb, "null", 1); // unprocessed with no node
        appendProcTaskSql(sb, "@nodeId", 1); // unprocessed with node

        return sb.toString();
    }

    private void appendProcTaskSql(final StringBuilder sb,
                                   final String nodeId,
                                   final int taskStatus) {

        sb.append(LogUtil.message("""
                insert into processor_task (
                    version,
                    fk_processor_filter_id,
                    fk_processor_node_id,
                    status,
                    meta_id)
                values (
                    1,
                    @procFilterId,
                    {},
                    {},
                    1);
                """, nodeId, taskStatus));
    }

    @Test
    void test() {

        // By this point the database will have been migrated up to the target version
        // with the test data having been applied prior to the target migration running.
        // If it fails before you get here then there is an exception in the migration
        // or the test data.

        // If you get here then you probably want to assert something about the migrated
        // state.

        // no change to row count
        Assertions.assertThat(getTableCount("processor_task"))
                .isEqualTo(4);

        // Read the affected rows
        final List<Row> rows = getRows("""
                        select
                            fk_processor_node_id,
                            status
                        from processor_task
                        """,
                rec -> new Row(
                        rec.get("fk_processor_node_id", Integer.class),
                        rec.get("status", int.class)));

        final int nodeId = getSingleValue(
                """
                        select distinct id
                        from processor_node""",
                int.class);

        Assertions.assertThat(rows)
                .containsExactlyInAnyOrder(
                        new Row(nodeId, 99), // no change
                        new Row(nodeId, 2), // no change
                        new Row(null, 0), // changed from nodeId|1
                        new Row(null, 0)); // changed from null|1
    }

    // TODO: 06/02/2023 convert to java record in j17
    private static class Row {

        private final Integer nodeId;
        private final int status;

        private Row(final Integer nodeId, final int status) {
            this.nodeId = nodeId;
            this.status = status;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Row row = (Row) o;
            return status == row.status && Objects.equals(nodeId, row.nodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, status);
        }

        @Override
        public String toString() {
            return "Row{" +
                    "nodeId=" + nodeId +
                    ", status=" + status +
                    '}';
        }
    }
}
