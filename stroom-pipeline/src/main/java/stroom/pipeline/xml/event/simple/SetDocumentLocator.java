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

package stroom.pipeline.xml.event.simple;

import stroom.pipeline.xml.event.BaseEvent;
import stroom.pipeline.xml.event.Event;

import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * A class used to store a setDocumentLocator SAX event.
 */
public final class SetDocumentLocator extends BaseEvent {

    private static final String SET_DOCUMENT_LOCATOR = "setDocumentLocator:";

    private final Locator locator;

    /**
     * Stores a setDocumentLocator SAX event.
     *
     * @param locator an object that can return the location of any SAX document
     *                event
     */
    public SetDocumentLocator(final Locator locator) {
        this.locator = locator;
    }

    /**
     * Fires a stored SAX event at the supplied content handler.
     *
     * @param handler The content handler to fire the SAX event at.
     * @throws SAXException Necessary to maintain the SAX event contract.
     * @see Event#fire(org.xml.sax.ContentHandler)
     */
    @Override
    public void fire(final ContentHandler handler) throws SAXException {
        handler.setDocumentLocator(locator);
    }

    @Override
    public String toString() {
        return SET_DOCUMENT_LOCATOR;
    }

    @Override
    public boolean isSetDocumentLocator() {
        return true;
    }
}
