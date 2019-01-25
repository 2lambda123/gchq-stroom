/*
 * Copyright 2018 Crown Copyright
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
 *
 */

package stroom.pipeline.refdata;

import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.sun.xml.fastinfoset.sax.SAXDocumentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.fastinfoset.FastInfosetException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import stroom.entity.shared.Range;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.FatalErrorReceiver;
import stroom.pipeline.filter.TestFilter;
import stroom.pipeline.filter.TestSAXEventFilter;
import stroom.pipeline.util.ProcessorUtil;
import stroom.pipeline.refdata.store.FastInfosetValue;
import stroom.pipeline.refdata.store.RefDataLoader;
import stroom.pipeline.refdata.store.RefDataValue;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.refdata.store.StringValue;
import stroom.pipeline.refdata.util.ByteBufferPool;
import stroom.pipeline.refdata.util.PooledByteBufferOutputStream;
import stroom.test.StroomPipelineTestFileUtil;
import stroom.util.test.StroomUnitTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestReferenceDataFilter extends StroomUnitTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestReferenceDataFilter.class);

    private static final String BASE_PATH = "TestReferenceDataFilter/";
    private static final String INPUT_STRING_VALUE_1 = BASE_PATH + "input_StringValue_1.xml";
    private static final String INPUT_STRING_VALUE_2 = BASE_PATH + "input_StringValue_2.xml";
    private static final String INPUT_FAST_INFOSET_VALUE_1 = BASE_PATH + "input_FastInfosetValue_1.xml";
    private static final String INPUT_FAST_INFOSET_VALUE_2 = BASE_PATH + "input_FastInfosetValue_2.xml";
    private static final String INPUT_FAST_INFOSET_VALUE_3 = BASE_PATH + "input_FastInfosetValue_3.xml";

    private static final int BUF_SIZE = 4096;

    @Mock
    private RefDataLoader refDataLoader;

    @Test
    void testStringKeyValues() {

        LoadedRefDataValues loadedRefDataValues = doTest(INPUT_STRING_VALUE_1, null);

        assertThat(loadedRefDataValues.keyValueValues).hasOnlyElementsOfType(StringValue.class);
        assertThat(loadedRefDataValues.rangeValueValues).isEmpty();

        assertThat(loadedRefDataValues.keyValueKeys)
                .containsExactly("key11", "key12", "key13", "key21", "key22", "key23");

        assertThat(
                loadedRefDataValues.keyValueValues.stream()
                        .map(refDataValue -> (StringValue) refDataValue)
                        .map(stringValue -> stringValue.getValue()))
                .containsExactly("value11", "value12", "value13", "value21", "value22", "value23");
    }

    @Test
    void testStringRangeValues() {

        LoadedRefDataValues loadedRefDataValues = doTest(INPUT_STRING_VALUE_2, null);

        assertThat(loadedRefDataValues.keyValueValues).isEmpty();
        assertThat(loadedRefDataValues.rangeValueValues).hasOnlyElementsOfType(StringValue.class);

        assertThat(
                loadedRefDataValues.rangeValueKeys)
                .containsExactly(
                        new Range<>(1L, 11L),
                        new Range<>(11L, 21L),
                        new Range<>(21L, 31L),
                        new Range<>(1L, 11L),
                        new Range<>(11L, 21L),
                        new Range<>(21L, 31L));
        assertThat(
                loadedRefDataValues.rangeValueValues.stream()
                        .map(refDataValue -> (StringValue) refDataValue)
                        .map(stringValue -> stringValue.getValue()))
                .containsExactly("value11", "value12", "value13", "value21", "value22", "value23");
    }

    @Test
    void testFastInfosetKeyValues() {

        LoadedRefDataValues loadedRefDataValues = doTest(INPUT_FAST_INFOSET_VALUE_1, null);

        assertThat(loadedRefDataValues.keyValueValues).hasOnlyElementsOfType(FastInfosetValue.class);
        assertThat(loadedRefDataValues.keyValueValues).hasSize(6);
        assertThat(loadedRefDataValues.rangeValueValues).isEmpty();

        loadedRefDataValues.keyValueValues.stream()
                .map(refDataValue -> (FastInfosetValue) refDataValue)
                .map(this::deserialise)
                .forEach(str -> {
                    LOGGER.info("Dumping deserialised output");
                    System.out.println(str);
                });
        Pattern pattern = Pattern.compile("room[0-9]+");

        List<String> roomList = loadedRefDataValues.keyValueValues.stream()
                .map(refDataValue -> (FastInfosetValue) refDataValue)
                .map(this::deserialise)
                .map(str -> {
                    Matcher matcher = pattern.matcher(str);
                    assertThat(matcher.find()).isTrue();
                    return matcher.group();
                })
                .collect(Collectors.toList());

        assertThat(roomList)
                .containsExactly("room11", "room12", "room13", "room21", "room22", "room23");
    }

    @Test
    void testFastInfosetKeyValues_localPrefixes() {

        LoadedRefDataValues loadedRefDataValues = doTest(INPUT_FAST_INFOSET_VALUE_2, null);

        assertThat(loadedRefDataValues.keyValueValues).hasOnlyElementsOfType(FastInfosetValue.class);
        assertThat(loadedRefDataValues.keyValueValues).hasSize(6);
        assertThat(loadedRefDataValues.rangeValueValues).isEmpty();

        loadedRefDataValues.keyValueValues.stream()
                .map(refDataValue -> (FastInfosetValue) refDataValue)
                .map(this::deserialise)
                .forEach(str -> {
                    LOGGER.info("Dumping deserialised output");
                    System.out.println(str);
                });
        Pattern pattern = Pattern.compile("room[0-9]+");

        List<String> roomList = loadedRefDataValues.keyValueValues.stream()
                .map(refDataValue -> (FastInfosetValue) refDataValue)
                .map(this::deserialise)
                .map(str -> {
                    Matcher matcher = pattern.matcher(str);
                    assertThat(matcher.find()).isTrue();
                    return matcher.group();
                })
                .collect(Collectors.toList());

        assertThat(roomList)
                .containsExactly("room11", "room12", "room13", "room21", "room22", "room23");
    }

    @Test
    void testFastInfosetRangeValues() {

        LoadedRefDataValues loadedRefDataValues = doTest(INPUT_FAST_INFOSET_VALUE_3, null);

        assertThat(loadedRefDataValues.keyValueValues).isEmpty();
        assertThat(loadedRefDataValues.rangeValueValues).hasOnlyElementsOfType(FastInfosetValue.class);
        assertThat(loadedRefDataValues.rangeValueValues).hasSize(6);

        loadedRefDataValues.rangeValueValues.stream()
                .map(refDataValue -> (FastInfosetValue) refDataValue)
                .map(this::deserialise)
                .forEach(str -> {
                    LOGGER.info("Dumping deserialised output");
                    System.out.println(str);
                });

        assertThat(
                loadedRefDataValues.rangeValueKeys)
                .containsExactly(
                        new Range<>(1L, 11L),
                        new Range<>(11L, 21L),
                        new Range<>(21L, 31L),
                        new Range<>(1L, 11L),
                        new Range<>(11L, 21L),
                        new Range<>(21L, 31L));

        Pattern pattern = Pattern.compile("room[0-9]+");

        List<String> roomList = loadedRefDataValues.rangeValueValues.stream()
                .map(refDataValue -> (FastInfosetValue) refDataValue)
                .map(this::deserialise)
                .map(str -> {
                    Matcher matcher = pattern.matcher(str);
                    assertThat(matcher.find()).isTrue();
                    return matcher.group();
                })
                .collect(Collectors.toList());

        assertThat(roomList)
                .containsExactly("room11", "room12", "room13", "room21", "room22", "room23");
    }

    private LoadedRefDataValues doTest(String inputPath, String expectedOutputPath) {
        final LoadedRefDataValues loadedRefDataValues = new LoadedRefDataValues();

        Mockito.when(refDataLoader.getRefStreamDefinition())
                .thenReturn(buildUniqueRefStreamDefinition());

        Mockito.when(refDataLoader.initialise(Mockito.anyBoolean()))
                .thenReturn(true);

        // capture the args passed to the two put methods. Have to use doAnswer
        // so we can copy the buffer that is reused and therefore mutates.
        Mockito.doAnswer(invocation -> {
            loadedRefDataValues.addKeyValue(
                    invocation.getArgument(1),
                    invocation.getArgument(2));
            return true;
        }).when(refDataLoader).put(
                Mockito.any(),
                Mockito.any(String.class),
                Mockito.any(RefDataValue.class));

        Mockito.doAnswer(invocation -> {
            loadedRefDataValues.addRangeValue(
                    invocation.getArgument(1), // mockito can infer the type
                    invocation.getArgument(2));
            return true;
        }).when(refDataLoader).put(
                Mockito.any(),
                Mockito.<Range<Long>>any(),
                Mockito.any(RefDataValue.class));

        final ByteArrayInputStream input = new ByteArrayInputStream(getString(inputPath).getBytes());

        ErrorReceiverProxy errorReceiverProxy = new ErrorReceiverProxy(new FatalErrorReceiver());
        RefDataLoaderHolder refDataLoaderHolder = new RefDataLoaderHolder();
        refDataLoaderHolder.setRefDataLoader(refDataLoader);

        final ReferenceDataFilter referenceDataFilter = new ReferenceDataFilter(
                errorReceiverProxy,
                refDataLoaderHolder,
                cap ->
                        new PooledByteBufferOutputStream(new ByteBufferPool(), cap));

        final TestFilter testFilter = new TestFilter(null, null);
        final TestSAXEventFilter testSAXEventFilter = new TestSAXEventFilter();

        referenceDataFilter.setTarget(testFilter);
        testFilter.setTarget(testSAXEventFilter);

        ProcessorUtil.processXml(
                input,
                new ErrorReceiverProxy(new FatalErrorReceiver()),
                referenceDataFilter,
                new LocationFactoryProxy());

        final List<String> actualXmlList = testFilter.getOutputs()
                .stream()
                .map(String::trim)
                .map(s -> s.replaceAll("\r", ""))
                .collect(Collectors.toList());

        actualXmlList.forEach(System.out::println);


        final String actualSax = testSAXEventFilter.getOutput().trim();

        LOGGER.info("Actual SAX: \n {}", actualSax);

        return loadedRefDataValues;
    }

    private String getString(final String resourceName) {
        try {
            final InputStream is = StroomPipelineTestFileUtil.getInputStream(resourceName);

            final byte[] buffer = new byte[BUF_SIZE];
            int len;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            String str = baos.toString();
            str = str.replaceAll("\r", "");
            return str.trim();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    private String deserialise(final FastInfosetValue fastInfosetValue) {
        TestSAXEventFilter testSAXEventFilter = new TestSAXEventFilter();
        TestFilter testFilter = new TestFilter(new ErrorReceiverProxy(), new LocationFactoryProxy());
        testFilter.setContentHandler(new MyContentHandler());

        SAXDocumentParser saxDocumentParser = new SAXDocumentParser();
        // it may be possible to only deal with fragments but can't seem to serialise without calling startDocument
//        saxDocumentParser.setParseFragments(true);
        saxDocumentParser.setContentHandler(testSAXEventFilter);
        try {
            saxDocumentParser.parse(new ByteBufferInputStream(fastInfosetValue.getByteBuffer()));
        } catch (IOException | FastInfosetException | SAXException e) {
            throw new RuntimeException(e);
        }
        // flip the buffer now we have read it so it can be read again if required
        fastInfosetValue.getByteBuffer().flip();

        return testSAXEventFilter.getOutput();
    }

    private RefStreamDefinition buildUniqueRefStreamDefinition() {
        return new RefStreamDefinition(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                123456L);
    }


    private static class MyContentHandler implements ContentHandler {

        @Override
        public void setDocumentLocator(final Locator locator) {

        }

        @Override
        public void startDocument() throws SAXException {

        }

        @Override
        public void endDocument() throws SAXException {

        }

        @Override
        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {

        }

        @Override
        public void endPrefixMapping(final String prefix) throws SAXException {

        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {

        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {

        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {

        }

        @Override
        public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {

        }

        @Override
        public void processingInstruction(final String target, final String data) throws SAXException {

        }

        @Override
        public void skippedEntity(final String name) throws SAXException {

        }
    }

    private static class LoadedRefDataValues {
        List<String> keyValueKeys;
        List<RefDataValue> keyValueValues;
        List<Range<Long>> rangeValueKeys;
        List<RefDataValue> rangeValueValues;

        LoadedRefDataValues() {
            this.keyValueKeys = new ArrayList<>();
            this.keyValueValues = new ArrayList<>();
            this.rangeValueValues = new ArrayList<>();
            this.rangeValueKeys = new ArrayList<>();
        }

        void addKeyValue(final String key, final RefDataValue value) {
            LOGGER.info("Adding keyValue {} {}", key, value);
            keyValueKeys.add(key);
            if (value instanceof FastInfosetValue) {
                FastInfosetValue fastInfosetValue = (FastInfosetValue) value;
                assertThat(fastInfosetValue.getByteBuffer().position()).isEqualTo(0);
                RefDataValue valueCopy = fastInfosetValue.copy(
                        () -> ByteBuffer.allocateDirect(fastInfosetValue.size()));
                assertThat(((FastInfosetValue) valueCopy).getByteBuffer().position()).isEqualTo(0);
                keyValueValues.add(valueCopy);
            } else {
                keyValueValues.add(value);
            }
        }

        void addRangeValue(final Range<Long> range, final RefDataValue value) {
            LOGGER.info("Adding rangeValue {} {}", range, value);
            rangeValueKeys.add(range);
            if (value instanceof FastInfosetValue) {
                FastInfosetValue fastInfosetValue = (FastInfosetValue) value;
                assertThat(fastInfosetValue.getByteBuffer().position()).isEqualTo(0);
                RefDataValue valueCopy = fastInfosetValue.copy(
                        () -> ByteBuffer.allocateDirect(fastInfosetValue.size()));
                assertThat(((FastInfosetValue) valueCopy).getByteBuffer().position()).isEqualTo(0);
                rangeValueValues.add(valueCopy);
            } else {
                rangeValueValues.add(value);
            }
        }
    }
}