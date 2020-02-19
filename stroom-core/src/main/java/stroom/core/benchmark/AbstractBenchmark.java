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

package stroom.core.benchmark;

import stroom.data.shared.StreamTypeNames;
import stroom.data.store.api.Source;
import stroom.data.store.api.SourceUtil;
import stroom.data.store.api.Store;
import stroom.data.store.api.Target;
import stroom.data.store.api.TargetUtil;
import stroom.feed.shared.FeedDoc;
import stroom.meta.shared.FindMetaCriteria;
import stroom.meta.shared.Meta;
import stroom.meta.shared.MetaExpressionUtil;
import stroom.meta.shared.MetaFields;
import stroom.meta.shared.MetaProperties;
import stroom.meta.api.MetaService;
import stroom.meta.shared.Status;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.task.api.TaskContext;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.ResultPage;
import stroom.util.xml.XMLUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

public abstract class AbstractBenchmark {
    // FIXME : Do something with this....

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(AbstractBenchmark.class);

    private final Store streamStore;
    private final MetaService metaService;
    private final TaskContext taskContext;

    AbstractBenchmark(final Store streamStore,
                      final MetaService metaService,
                      final TaskContext taskContext) {
        this.streamStore = streamStore;
        this.metaService = metaService;
        this.taskContext = taskContext;
    }

    public static int getRandomSkewed() {
        return (int) (Math.exp(Math.random() * 10));
    }

    protected boolean isTerminated() {
        return Thread.currentThread().isInterrupted();
    }

    public void abortDueToTimeout() {
        Thread.currentThread().interrupt();
    }

    protected void info(final Supplier<String> messageSupplier) {
        taskContext.info(messageSupplier);
        LOGGER.info(messageSupplier);
    }

    protected void infoInterval(final Supplier<String> messageSupplier) {
        taskContext.info(messageSupplier);
        LOGGER.info(messageSupplier);
    }

    protected Meta writeData(final String feedName, final String streamTypeName, final String data) {
        // Add the associated data to the stream store.
        final MetaProperties metaProperties = new MetaProperties.Builder()
                .feedName(feedName)
                .typeName(streamTypeName)
                .build();

        try (final Target dataTarget = streamStore.openTarget(metaProperties)) {
            TargetUtil.write(dataTarget, data);
            return dataTarget.getMeta();
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String readData(final long streamId) throws IOException {
        String data = null;
        try (final Source source = streamStore.openSource(streamId)) {
            data = SourceUtil.readString(source);
        }
        return data;
    }

    protected String stripChangingContent(final String s) {
        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.startsWith("<Event Id=\"", i)) {
                output.append("<Event Id=\"XXXXX\">");
                i = s.indexOf('>', i);
                continue;
            }
            if (s.startsWith("<TimeCreated>", i)) {
                output.append("<TimeCreated>XXXXX</TimeCreated>");
                i = s.indexOf("</TimeCreated>", i) + "</TimeCreated>".length() - 1;
                continue;
            }
            if (s.startsWith("<Id>", i)) {
                output.append("<Id>XXXXX</Id>");
                i = s.indexOf("</Id>", i) + "</Id>".length() - 1;
                continue;
            }

            output.append(s.charAt(i));
        }
        return output.toString();
    }

    protected void verifyData(final FeedDoc feed, final String verificationString) throws IOException {
        final ExpressionOperator.Builder builder = new ExpressionOperator.Builder(Op.AND);
        builder.addTerm(MetaFields.FEED_NAME, Condition.EQUALS, feed.getName());
        if (feed.isReference()) {
            builder.addTerm(MetaFields.TYPE_NAME, Condition.EQUALS, StreamTypeNames.REFERENCE);
        } else {
            builder.addTerm(MetaFields.TYPE_NAME, Condition.EQUALS, StreamTypeNames.EVENTS);
        }
        final FindMetaCriteria criteria = new FindMetaCriteria();
        criteria.setExpression(builder.build());
        final ResultPage<Meta> list = metaService.find(criteria);
        final Meta targetMeta = list.getFirst();

        // Get back translated result.
        try (final Source source = streamStore.openSource(targetMeta.getId())) {
            String xml = SourceUtil.readString(source);

            // Pretty print the xml.
            xml = XMLUtil.prettyPrintXML(xml);

            // Get rid of event ids.
            xml = stripChangingContent(xml);

            if (!verificationString.equals(xml)) {
                throw new RuntimeException("Data verification failure!");
            }
        }
    }

    protected void deleteData(final String... feedNames) {
        final FindMetaCriteria criteria = new FindMetaCriteria();
        criteria.setExpression(MetaExpressionUtil.createFeedsExpression(feedNames));
        metaService.updateStatus(criteria, Status.DELETED);
    }

    protected String createReferenceData(final int recordCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("FileNo,Country,Site,Building,Floor,Room,Desk\n");

        for (int i = 0; i < recordCount && !Thread.currentThread().isInterrupted(); i++) {
            sb.append(i);
            sb.append(",UK,Site ");
            sb.append(i);
            sb.append(",Main,1,A,100\n");
        }

        return sb.toString();
    }

    protected String createEventData(final int recordCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Date,Time,FileNo,LineNo,User,Message\n");

        final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy,HH:mm:ss");

        for (int i = 0; i < recordCount && !Thread.currentThread().isInterrupted(); i++) {
            sb.append(df.format(new Date()));
            sb.append(",");
            sb.append(i);
            sb.append(",1,user");
            sb.append(getRandomSkewed());
            sb.append(",Some message 1\n");
        }

        return sb.toString();
    }

    protected String createReferenceVerificationData(final int recordCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<referenceData xmlns:evt=\"event-logging:3\"\n");
        sb.append("               xmlns:stroom=\"stroom\"\n");
        sb.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("               xmlns=\"reference-data:2\"\n");
        sb.append(
                "               xsi:schemaLocation=\"reference-data:2 file://reference-data-v2.0.1.xsd event-logging:3 file://event-logging-v3.0.0.xsd\"\n");
        sb.append("               Version=\"2.4.2\">\n");

        for (int i = 0; i < recordCount; i++) {
            sb.append("   <reference>\n");
            sb.append("      <map>FILENO_TO_LOCATION_MAP</map>\n");
            sb.append("      <key>");
            sb.append(i);
            sb.append("</key>\n");
            sb.append("      <value>\n");
            sb.append("         <evt:Location>\n");
            sb.append("            <evt:Country>UK</evt:Country>\n");
            sb.append("            <evt:Site>Site ");
            sb.append(i);
            sb.append("</evt:Site>\n");
            sb.append("            <evt:Building>Main</evt:Building>\n");
            sb.append("            <evt:Floor>1</evt:Floor>\n");
            sb.append("            <evt:Room>A</evt:Room>\n");
            sb.append("            <evt:Desk>100</evt:Desk>\n");
            sb.append("         </evt:Location>\n");
            sb.append("      </value>\n");
            sb.append("   </reference>\n");
        }

        sb.append("</referenceData>");

        return sb.toString();
    }

    protected String createEventVerificationData(final int recordCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<Events xpath-default-namespace=\"records:2\"\n");
        sb.append("        xmlns:stroom=\"stroom\"\n");
        sb.append("        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("        xmlns=\"event-logging:3\"\n");
        sb.append(
                "        xsi:schemaLocation=\"event-logging:3 file://event-logging-v3.0.0.xsd\"\n");
        sb.append("        Version=\"2.4.2\">\n");

        for (int i = 0; i < recordCount; i++) {
            sb.append("   <Event Id=\"XXXXX\">\n");
            sb.append("      <EventTime>\n");
            sb.append("         <TimeCreated>XXXXX</TimeCreated>\n");
            sb.append("      </EventTime>\n");
            sb.append("      <EventSource>\n");
            sb.append("         <Generator>CSV</Generator>\n");
            sb.append("         <Device>\n");
            sb.append("            <IPAddress>1.1.1.1</IPAddress>\n");
            sb.append("            <MACAddress>00-00-00-00-00-00</MACAddress>\n");
            sb.append("            <Location>\n");
            sb.append("               <Country>UK</Country>\n");
            sb.append("               <Site>Site ");
            sb.append(i);
            sb.append("</Site>\n");
            sb.append("               <Building>Main</Building>\n");
            sb.append("               <Floor>1</Floor>\n");
            sb.append("               <Room>A</Room>\n");
            sb.append("               <Desk>100</Desk>\n");
            sb.append("            </Location>\n");
            sb.append("         </Device>\n");
            sb.append("         <User>\n");
            sb.append("            <Id>XXXXX</Id>\n");
            sb.append("         </User>\n");
            sb.append("      </EventSource>\n");
            sb.append("      <EventDetail>\n");
            sb.append("         <Description>Some message 1</Description>\n");
            sb.append("         <Authenticate>\n");
            sb.append("            <Action>Logon</Action>\n");
            sb.append("            <LogonType>Interactive</LogonType>\n");
            sb.append("            <data name=\"FileNo\" value=\"");
            sb.append(i);
            sb.append("\"/>\n");
            sb.append("            <data name=\"LineNo\" value=\"1\"/>\n");
            sb.append("         </Authenticate>\n");
            sb.append("      </EventDetail>\n");
            sb.append("   </Event>\n");
        }

        sb.append("</Events>");

        return sb.toString();
    }
}
