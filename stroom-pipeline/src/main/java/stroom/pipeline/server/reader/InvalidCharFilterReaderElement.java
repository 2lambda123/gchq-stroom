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
import stroom.pipeline.server.errorhandler.ErrorReceiver;
import stroom.pipeline.server.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.server.factory.ConfigurableElement;
import stroom.pipeline.server.factory.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import java.io.Reader;

@Component
@Scope("prototype")
@ConfigurableElement(type = "InvalidCharFilterReader",
        category = Category.READER,
        roles = {
                PipelineElementType.ROLE_HAS_TARGETS,
                PipelineElementType.ROLE_READER,
                PipelineElementType.ROLE_MUTATOR,
                PipelineElementType.VISABILITY_STEPPING},
        icon = ElementIcons.STREAM)
public class InvalidCharFilterReaderElement extends AbstractReaderElement {
    private final ErrorReceiver errorReceiver;

    private InvalidCharFilterReader invalidCharFilterReader;

    @Inject
    public InvalidCharFilterReaderElement(final ErrorReceiverProxy errorReceiver) {
        this.errorReceiver = errorReceiver;
    }

    @Override
    protected Reader insertFilter(final Reader reader) {
        invalidCharFilterReader = new InvalidCharFilterReader(reader);
        return invalidCharFilterReader;
    }

    @Override
    public void endStream() {
        if (invalidCharFilterReader.hasModifiedContent()) {
            errorReceiver.log(Severity.WARNING, null, getElementId(),
                    "Some illegal characters were removed from the input stream", null);
        }
    }
}
