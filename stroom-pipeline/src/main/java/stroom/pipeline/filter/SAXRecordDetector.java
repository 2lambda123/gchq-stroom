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

package stroom.pipeline.filter;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import stroom.pipeline.stepping.RecordDetector;
import stroom.pipeline.stepping.SteppingController;

public class SAXRecordDetector extends AbstractXMLFilter implements RecordDetector {
    private SteppingController controller;

    private long currentStepNo;

    @Override
    public void startStream() {
        if (controller != null) {
            currentStepNo = 0;
            controller.resetSourceLocation();
        }
        super.startStream();
    }

    @Override
    public void startDocument() throws SAXException {
        currentStepNo++;
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();

        // Tell the controller that this is the end of a record.
        if (controller != null && controller.endRecord(currentStepNo)) {
            throw new ExitSteppingException();
        }
    }

    @Override
    public void setController(final SteppingController controller) {
        this.controller = controller;
    }
}
