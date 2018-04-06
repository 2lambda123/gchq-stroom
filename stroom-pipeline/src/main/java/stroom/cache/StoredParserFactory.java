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

package stroom.cache;

import stroom.pipeline.errorhandler.StoredErrorReceiver;
import stroom.xml.converter.ParserFactory;

public class StoredParserFactory {
    private final ParserFactory parserFactory;
    private final StoredErrorReceiver errorReceiver;

    public StoredParserFactory(final ParserFactory parserFactory, final StoredErrorReceiver errorReceiver) {
        this.parserFactory = parserFactory;
        this.errorReceiver = errorReceiver;
    }

    public ParserFactory getParserFactory() {
        return parserFactory;
    }

    public StoredErrorReceiver getErrorReceiver() {
        return errorReceiver;
    }
}
