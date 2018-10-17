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

package stroom.util;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import stroom.entity.server.util.XMLUtil;
import stroom.pipeline.server.DefaultLocationFactory;
import stroom.pipeline.server.LocationFactory;
import stroom.pipeline.server.errorhandler.ErrorHandlerAdaptor;
import stroom.pipeline.server.errorhandler.FatalErrorReceiver;
import stroom.pipeline.server.errorhandler.ProcessException;
import stroom.pipeline.server.filter.SafeXMLFilter;
import stroom.pipeline.server.filter.XMLFilterContentHandlerAdaptor;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;
import stroom.util.xml.SAXParserFactoryFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Sanitiser {
    private static final SAXParserFactory PARSER_FACTORY;

    static {
        PARSER_FACTORY = SAXParserFactoryFactory.newInstance();
    }

    private Sanitiser(final Path in, final Path out) {
        process(in, out);
    }

    public static void main(final String[] args) {
        if (args.length != 2) {
            System.out.println("Bad arguments - provide input and output files.");
        }

        new Sanitiser(Paths.get(args[0]), Paths.get(args[1]));
    }

    private void process(final Path in, final Path out) {
        try (final Reader reader = Files.newBufferedReader(in, StreamUtil.DEFAULT_CHARSET);
             final Writer writer = Files.newBufferedWriter(out, StreamUtil.DEFAULT_CHARSET)) {
            final TransformerHandler th = XMLUtil.createTransformerHandler(true);
            th.setResult(new StreamResult(writer));

            SAXParser parser;
            try {
                parser = PARSER_FACTORY.newSAXParser();
            } catch (final ParserConfigurationException e) {
                throw ProcessException.wrap(e);
            }

            final SafeXMLFilter filter = new SafeXMLFilter();
            filter.setContentHandler(new XMLFilterContentHandlerAdaptor(th));

            final LocationFactory locationFactory = new DefaultLocationFactory();

            final XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler(filter);
            xmlReader.setErrorHandler(new ErrorHandlerAdaptor("XMLReader", locationFactory, new FatalErrorReceiver()));
            xmlReader.parse(new InputSource(reader));

        } catch (final Exception e) {
            System.out.println("Error processing file: " + FileUtil.getCanonicalPath(in));
            e.printStackTrace();
        }
    }
}
