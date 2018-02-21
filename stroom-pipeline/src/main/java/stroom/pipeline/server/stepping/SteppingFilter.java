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

package stroom.pipeline.server.stepping;

/**
 * A filter used to test if the current step in stepping mode matches against
 * the step filter provided by the user.
 */
public interface SteppingFilter {
    /**
     * Tests to see if a filter has been applied
     */
    boolean isFilterApplied();

    /**
     * Test to see if the filter has matched the current record.
     */
    boolean filterMatches(long currentRecordNo);
}
