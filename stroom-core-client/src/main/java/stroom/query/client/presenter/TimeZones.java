/*
 * Copyright 2016 Crown Copyright
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

package stroom.query.client.presenter;

import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.query.shared.QueryResource;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class TimeZones {

    private static final QueryResource QUERY_RESOURCE = GWT.create(QueryResource.class);

    private String localTimeZoneId;
    private List<String> ids;

    @Inject
    public TimeZones(final RestFactory restFactory) {
        try {
            localTimeZoneId = getIntlTimeZone();
        } catch (final RuntimeException e) {
            localTimeZoneId = "Z";
        }

        final Rest<List<String>> rest = restFactory.create();
        rest
                .onSuccess(result -> ids = result)
                .call(QUERY_RESOURCE)
                .fetchTimeZones();
    }

    public String getLocalTimeZoneId() {
        return localTimeZoneId;
    }

    public List<String> getIds() {
        return ids;
    }

    /**
     * This javascript call attempts to get the time zone, e.g. 'Europe/London'
     * using the ECMAScript Internationalisation API Specification
     *
     * @return The browsers time zone, e.g. 'Europe/London' or
     * 'Australia/Sydney'.
     */
    private native String getIntlTimeZone()
    /*-{
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
    }-*/;
}
