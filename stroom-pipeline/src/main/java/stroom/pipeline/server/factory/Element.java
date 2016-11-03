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

package stroom.pipeline.server.factory;

import java.util.List;

public interface Element extends HasElementId {
    /**
     * Called just before a pipeline begins processing.
     */
    void startProcessing();

    /**
     * Called just after a pipeline has finished processing.
     */
    void endProcessing();

    /**
     * This method tells filters that a stream is about to be parsed so that
     * they can complete any setup necessary.
     */
    void startStream();

    /**
     * This method tells filters that a stream has finished parsing so cleanup
     * can be performed.
     */
    void endStream();

    /**
     * Create any processors required to process the current stream.
     *
     * @return A list of processors.
     */
    List<Processor> createProcessors();
}
