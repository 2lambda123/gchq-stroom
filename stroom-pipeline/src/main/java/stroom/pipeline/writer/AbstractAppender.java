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

package stroom.pipeline.writer;

import stroom.pipeline.destination.Destination;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.Severity;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractAppender extends AbstractDestinationProvider implements Destination {

    private final ErrorReceiverProxy errorReceiverProxy;

    private OutputStream outputStream;
    private byte[] header;
    private byte[] footer;
    private boolean writtenHeader;
    private String size;
    private Long sizeBytes = null;
    boolean splitAggregatedStreams;
    boolean splitRecords;

    AbstractAppender(final ErrorReceiverProxy errorReceiverProxy) {
        this.errorReceiverProxy = errorReceiverProxy;
    }

    @Override
    public void endProcessing() {
        writeFooter();
        endEntry();
        closeCurrentOutputStream();
        super.endProcessing();
    }

    @Override
    public void endStream() {
        if (splitAggregatedStreams) {
            writeFooter();
            endEntry();
        }
        super.endStream();
    }

    protected void startEntry() {

    }

    protected void endEntry() {
        closeCurrentOutputStream();
    }

    @Override
    public Destination borrowDestination() throws IOException {
        return this;
    }

    @Override
    public void returnDestination(final Destination destination) throws IOException {
        // We assume that the parent will write an entire segment when it borrows a destination so add a segment marker
        // here after a segment is written.

        // Writing a segment marker here ensures there is always a marker written before the footer regardless or
        // whether a footer is actually written. We do this because we always make an allowance for a footer for data
        // display purposes.
        insertSegmentMarker();

        if (splitRecords) {
            if (getCurrentOutputSize() > 0) {
                writeFooter();
                endEntry();
            }
        } else {
            final Long sizeBytes = getSizeBytes();
            if (sizeBytes > 0 && getCurrentOutputSize() >= sizeBytes) {
                writeFooter();
                endEntry();
                closeCurrentOutputStream();
            }
        }
    }

    void closeCurrentOutputStream() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (final IOException e) {
                error(e.getMessage(), e);
            }
            outputStream = null;
        }
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        return getOutputStream(null, null);
    }

    @Override
    public OutputStream getOutputStream(final byte[] header, final byte[] footer) throws IOException {
        this.header = header;
        this.footer = footer;

        if (outputStream == null) {
            outputStream = createOutputStream();
        }

        startEntry();

        // If we haven't written yet then create the output stream and
        // write a header if we have one.
        writeHeader();

        return outputStream;
    }

    /**
     * Method to allow subclasses to insert segment markers between records.
     */
    void insertSegmentMarker() throws IOException {
    }

    void writeHeader() throws IOException {
        if (!writtenHeader) {
            if (outputStream != null && header != null && header.length > 0) {
                try {
                    // Write the header.
                    write(header);
                } catch (final IOException e) {
                    error(e.getMessage(), e);
                }
            }

            // Insert a segment marker before we write the next record regardless of whether the header has actually
            // been written. This is because we always make an allowance for the existence of a header in a segmented
            // stream when viewing data.
            insertSegmentMarker();

            writtenHeader = true;
        }
    }

    void writeFooter() {
        if (writtenHeader) {
            if (outputStream != null && footer != null && footer.length > 0) {
                try {
                    // Write the footer.
                    write(footer);
                } catch (final IOException e) {
                    error(e.getMessage(), e);
                }
            }
            writtenHeader = false;
        }
    }

    private void write(final byte[] bytes) throws IOException {
        outputStream.write(bytes, 0, bytes.length);
    }

    protected abstract OutputStream createOutputStream() throws IOException;

    protected void error(final String message, final Exception e) {
        errorReceiverProxy.log(Severity.ERROR, null, getElementId(), message, e);
    }

    abstract long getCurrentOutputSize();

    private Long getSizeBytes() {
        if (sizeBytes == null) {
            sizeBytes = -1L;

            // Set the maximum number of bytes to write before creating a new stream.
            if (size != null && !size.trim().isEmpty()) {
                try {
                    sizeBytes = ModelStringUtil.parseIECByteSizeString(size);
                } catch (final RuntimeException e) {
                    errorReceiverProxy.log(Severity.ERROR, null, getElementId(), "Unable to parse size: " + size, null);
                }
            }
        }
        return sizeBytes;
    }

    protected void setRollSize(final String size) {
        this.size = size;
    }

    protected void setSplitAggregatedStreams(final boolean splitAggregatedStreams) {
        this.splitAggregatedStreams = splitAggregatedStreams;
    }

    protected void setSplitRecords(final boolean splitRecords) {
        this.splitRecords = splitRecords;
    }
}
