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

package stroom.meta.impl.db;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static stroom.data.meta.impl.db.stroom.tables.DataFeed.DATA_FEED;

@Singleton
class MetaFeedServiceImpl implements MetaFeedService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaFeedServiceImpl.class);

    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    private final ConnectionProvider connectionProvider;

    @Inject
    MetaFeedServiceImpl(final ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Integer getOrCreate(final String name) {
        Integer id = get(name);
        if (id == null) {
            // Try and create.
            id = create(name);
            if (id == null) {
                // Get again.
                id = get(name);
            }
        }

        return id;
    }

    @Override
    public List<String> list() {
        try (final Connection connection = connectionProvider.getConnection()) {
            final DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            return create
                    .select(DATA_FEED.NAME)
                    .from(DATA_FEED)
                    .fetch(DATA_FEED.NAME);

        } catch (final SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Integer get(final String name) {
        Integer id = cache.get(name);
        if (id != null) {
            return id;
        }

        try (final Connection connection = connectionProvider.getConnection()) {
            final DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            id = create
                    .select(DATA_FEED.ID)
                    .from(DATA_FEED)
                    .where(DATA_FEED.NAME.eq(name))
                    .fetchOne(DATA_FEED.ID);

        } catch (final SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        if (id != null) {
            cache.put(name, id);
        }

        return id;
    }

    private Integer create(final String name) {
        try (final Connection connection = connectionProvider.getConnection()) {
            final DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            final Integer id = create
                    .insertInto(DATA_FEED, DATA_FEED.NAME)
                    .values(name)
                    .returning(DATA_FEED.ID)
                    .fetchOne()
                    .getId();
            cache.put(name, id);
            return id;

        } catch (final SQLException | RuntimeException e) {
            // Expect errors in the case of duplicate names.
            LOGGER.debug(e.getMessage(), e);
        }

        return null;
    }

    void clear() {
        deleteAll();
        cache.clear();
    }

    int deleteAll() {
        try (final Connection connection = connectionProvider.getConnection()) {
            final DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            return create
                    .delete(DATA_FEED)
                    .execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
