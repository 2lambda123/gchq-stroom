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

package stroom.pipeline.server.reader;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.pipeline.server.task.RecordDetector;
import stroom.pipeline.server.task.SteppingController;

import java.io.InputStream;

@Component
@Scope("prototype")
public class InputStreamRecordDetectorElement extends AbstractInputElement implements RecordDetector {
    private SteppingController controller;

    @Override
    protected InputStream insertFilter(final InputStream inputStream, final String encoding) {
        if (controller == null) {
            return inputStream;
        }
        return new InputStreamRecordDetector(inputStream, controller);
    }

    @Override
    public void setController(final SteppingController controller) {
        this.controller = controller;
    }
}
