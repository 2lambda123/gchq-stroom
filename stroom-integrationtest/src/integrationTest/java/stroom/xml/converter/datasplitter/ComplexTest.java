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

package stroom.xml.converter.datasplitter;

import org.junit.Ignore;
import org.junit.Test;
import stroom.test.AbstractProcessIntegrationTest;
import stroom.util.spring.StroomBeanStore;

import javax.inject.Inject;

// FIXME : Fix this test.
@Ignore("Create a new test")
public class ComplexTest extends AbstractProcessIntegrationTest {
    private static final String INPUT = "ComplexTest/ComplexTestInput.nxml";
    private static final String OUTPUT = "ComplexTest/ComplexTestOutput.xml";
    private static final String SPLITTER = "ComplexTest/ComplexTestSplitter.xml";

    @Inject
    private StroomBeanStore beanStore;

    @Test
    public void test() throws Exception {
//		final Path expectedFile = StroomProcessTestFileUtil.getTestResourcesFile(OUTPUT);
//		final Path actualFile = getCurrentTestDir().resolve("ComplexTestOutput.xml");
//
//		// Get some input streams.
//		final Reader input = new InputStreamReader(StroomProcessTestFileUtil.getInputStream(INPUT));
//		final Reader splitter = new InputStreamReader(StroomProcessTestFileUtil.getInputStream(SPLITTER));
//
//		// Create the output stream.
//		final Writer actual = new BufferedWriter(new FileWriter(actualFile));
//
//		// Create the parser.
//		final LoggingErrorReceiver errorReceiver = new LoggingErrorReceiver();
//		final LocationFactory locationFactory = new DefaultLocationFactory();
//		final ErrorHandlerAdaptor errorHandler = new ErrorHandlerAdaptor("DataSplitterParserFactory", locationFactory,
//				errorReceiver);
//		final DataSplitterParserFactory factory = beanStore.getBean(DataSplitterParserFactory.class);
//		factory.configure(splitter, errorHandler);
//		final DataSplitterParser parser = new DataSplitterParser(factory);
//
//		// Create the output content handler.
//		final TransformerHandler th = XMLUtil.createTransformerHandler(true);
//		th.setResult(new StreamResult(actual));
//
//		// Assign the handler.
//		parser.setContentHandler(th);
//
//		// Parse the data.
//		parser.parse(new InputSource(input));
//
//		ComparisonHelper.compareFiles(expectedFile, actualFile);
    }
}
