/*
 *
 *  * Copyright 2018 Crown Copyright
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package stroom.processor.impl;

import com.google.common.base.Strings;
import stroom.processor.shared.FindProcessorFilterCriteria;
import stroom.processor.shared.FindProcessorFilterTaskCriteria;
import stroom.util.shared.Sort;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;
import static stroom.util.shared.Sort.Direction.DESCENDING;

/**
 * Users can search for stream tasks using key words. This class defines what keywords are
 * available and maps them to options on the the criteria.
 */
public class SearchKeywords {
    private static final String DELIMITER = ":";

    private static final String IS = "is";
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private static final String COMPLETE = "complete";
    private static final String INCOMPLETE = "incomplete";

    private static final String SORT = "sort";
    private static final String NEXT = "next";

    static final String SORT_NEXT = SORT + DELIMITER + NEXT;

    static void addFiltering(String filter, FindProcessorFilterCriteria criteria) {
        if (filter != null) {
            String[] keywords = filter.split(" ");

            Arrays.stream(keywords)
                    .filter(keyword -> keyword.contains(DELIMITER))
                    .forEach(special -> add(special, criteria));

            String plainOldFilter = Arrays.stream(keywords)
                    .filter(keyword -> !keyword.contains(DELIMITER))
                    .collect(joining());

            if (!Strings.isNullOrEmpty(plainOldFilter)) {
                criteria.obtainPipelineUuidCriteria().setString(plainOldFilter);
            }
        }
    }

    private static void add(String special, FindProcessorFilterCriteria criteria) {
        String[] terms = special.split(DELIMITER);
        if (terms.length == 2) {
            if (terms[0].equalsIgnoreCase(IS)) {
                if (terms[1].equalsIgnoreCase(ENABLED)) {
                    criteria.setProcessorFilterEnabled(true);
                } else if (terms[1].equalsIgnoreCase(DISABLED)) {
                    criteria.setProcessorFilterEnabled(false);
                }
//                else if (terms[1].equalsIgnoreCase(COMPLETE)) {
//                    criteria.setStatus(ProcessorFilterTracker.COMPLETE);
//                } else if (terms[1].equalsIgnoreCase(INCOMPLETE)) {
//                    criteria.setStatus("");
//                }
            } else if (terms[0].equalsIgnoreCase(SORT)) {
                if (terms[1].equalsIgnoreCase(NEXT)) {
                    // We don't want any other sorts happening here, so we'll get rid of them.
                    criteria.removeSorts();
                    criteria.addSort(FindProcessorFilterTaskCriteria.FIELD_PRIORITY, DESCENDING, false);
                    criteria.addSort(FindProcessorFilterTaskCriteria.FIELD_POLL_AGE, Sort.Direction.ASCENDING, false);
                }
            }
        }
    }
}
