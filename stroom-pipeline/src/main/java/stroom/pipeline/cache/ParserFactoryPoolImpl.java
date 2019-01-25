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

package stroom.pipeline.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import stroom.entity.shared.EntityEvent;
import stroom.entity.shared.EntityEventHandler;
import stroom.security.DocumentPermissionCache;
import stroom.pipeline.DefaultLocationFactory;
import stroom.pipeline.LocationFactory;
import stroom.pipeline.errorhandler.ErrorHandlerAdaptor;
import stroom.pipeline.errorhandler.StoredErrorReceiver;
import stroom.pipeline.shared.TextConverterDoc;
import stroom.pipeline.shared.TextConverterDoc.TextConverterType;
import stroom.security.Security;
import stroom.security.SecurityContext;
import stroom.cache.api.CacheManager;
import stroom.util.io.StreamUtil;
import stroom.util.shared.Severity;
import stroom.pipeline.xml.converter.ParserFactory;
import stroom.pipeline.xml.converter.xmlfragment.XMLFragmentParserFactory;
import stroom.xmlschema.shared.XmlSchemaDoc;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@EntityEventHandler(type = XmlSchemaDoc.DOCUMENT_TYPE)
class ParserFactoryPoolImpl
        extends AbstractDocPool<TextConverterDoc, StoredParserFactory>
        implements ParserFactoryPool, EntityEvent.Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserFactoryPool.class);

    private final DSChooser dsChooser;

    @Inject
    ParserFactoryPoolImpl(final CacheManager cacheManager,
                          final DocumentPermissionCache documentPermissionCache,
                          final Security security,
                          final SecurityContext securityContext,
                          final DSChooser dsChooser) {
        super(cacheManager, "Parser Factory Pool", documentPermissionCache, security, securityContext);
        this.dsChooser = dsChooser;
    }

    @Override
    protected StoredParserFactory createValue(final TextConverterDoc textConverter) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating parser factory: " + textConverter.toString());
        }

        final StoredErrorReceiver errorReceiver = new StoredErrorReceiver();
        final LocationFactory locationFactory = new DefaultLocationFactory();
        final ErrorHandler errorHandler = new ErrorHandlerAdaptor(getClass().getSimpleName(), locationFactory,
                errorReceiver);
        ParserFactory parserFactory = null;

        try {
            if (TextConverterType.DATA_SPLITTER.equals(textConverter.getConverterType())) {
                parserFactory = dsChooser.configure(textConverter.getData(), errorHandler);

                // } else if
                // (textConverter.getConverterType().equals(TextConverterType.JAVA_CC))
                // {
                // final JavaCCParserFactory javaCCParserFactory =
                // JavaCCParserFactory
                // .create(StreamUtil.stringToStream(textConverter.getData()),
                // errorHandler);
                //
                // parserFactory = javaCCParserFactory;

            } else if (textConverter.getConverterType().equals(TextConverterType.XML_FRAGMENT)) {
                parserFactory = XMLFragmentParserFactory.create(StreamUtil.stringToStream(textConverter.getData()), errorHandler);

            } else {
                parserFactory = dsChooser.configure(textConverter.getData(), errorHandler);

//                    final String message = "Unknown text converter type: " + textConverter.getConverterType().toString();
//                    throw new ProcessException(message);
            }

        } catch (final RuntimeException e) {
            LOGGER.debug(e.getMessage(), e);
            errorReceiver.log(Severity.FATAL_ERROR, null, getClass().getSimpleName(), e.getMessage(), e);
        }

        return new StoredParserFactory(parserFactory, errorReceiver);
    }

    @Override
    public void onChange(final EntityEvent event) {
        clear();
    }
}
