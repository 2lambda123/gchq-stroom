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
 */

package stroom.legacy.model_6_1;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

@Entity
@Table(name = "QUERY")
public class QueryEntity extends DocumentEntity {
    public static final String ENTITY_TYPE = "Query";
    public static final String TABLE_NAME = SQLNameConstants.QUERY;
    public static final String FOREIGN_KEY = FK_PREFIX + TABLE_NAME + ID_SUFFIX;
    public static final String DASHBOARD_ID = SQLNameConstants.DASHBOARD + ID_SUFFIX;
    public static final String QUERY_ID = SQLNameConstants.QUERY + ID_SUFFIX;
    public static final String FAVOURITE = SQLNameConstants.FAVOURITE;

    private static final long serialVersionUID = 3598996730392094523L;

    private Long dashboardId;
    private String queryId;
    private String data;
    private Query query;
    private boolean favourite;

    public QueryEntity() {
        // Default constructor necessary for GWT serialisation.
    }

    @Column(name = DASHBOARD_ID)
    public Long getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(final Long dashboardId) {
        this.dashboardId = dashboardId;
    }

    @Column(name = QUERY_ID)
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(final String queryId) {
        this.queryId = queryId;
    }

    @Lob
    @Column(name = SQLNameConstants.DATA, length = Integer.MAX_VALUE)
    @ExternalFile
    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    @Transient
    @XmlTransient
    public Query getQuery() {
        return query;
    }

    public void setQuery(final Query query) {
        this.query = query;
    }

    @Column(name = FAVOURITE, nullable = false)
    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(final boolean favourite) {
        this.favourite = favourite;
    }

    @Override
    protected void toString(final StringBuilder sb) {
        super.toString(sb);
        if (data != null) {
            sb.append(data);
        }
    }

    @Transient
    @Override
    public final String getType() {
        return ENTITY_TYPE;
    }
}
