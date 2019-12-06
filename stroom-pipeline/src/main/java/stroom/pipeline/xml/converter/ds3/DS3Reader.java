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

package stroom.pipeline.xml.converter.ds3;

import org.xml.sax.Locator;

import java.io.IOException;
import java.io.Reader;

public class DS3Reader extends CharBuffer implements DSLocator {
    private final int initialSize;
    private final int capacity;
    private final int halfCapacity;

    private Reader reader;
    private boolean eof;
    private int lineNo = 1;
    private int colNo = 0;

    private int currentLineNo = 1;
    private int currentColNo = 0;

    public DS3Reader(final Reader reader, final int initialSize, final int capacity) {
        super(initialSize);
        setReader(reader);

        if (initialSize < 8) {
            throw new IllegalStateException("The initial size must be greater than or equal to 8");
        }
        if (capacity < initialSize) {
            throw new IllegalStateException("Capacity must be greater or equal to " + initialSize);
        }
        if (initialSize > capacity) {
            throw new IllegalStateException("The initial size cannot be greater than the capacity");
        }

        this.capacity = capacity;
        this.initialSize = initialSize;
        this.halfCapacity = capacity / 2;
    }

    public void setReader(final Reader reader) {
        this.reader = reader;
        offset = 0;
        length = 0;
        lineNo = 1;
        colNo = 0;
        currentLineNo = 1;
        currentColNo = 0;
        eof = false;
    }

    public void fillBuffer() throws IOException {
        // Only fill the buffer if we haven't reached the end of the file.
        if (!eof) {
            int i = 0;

            // Only fill the buffer if the length of remaining characters is
            // less than half the capacity.
            if (length < halfCapacity) {
                // If we processed more than half of the capacity then we need
                // to shift the buffer up so that the offset becomes 0. This
                // will give us
                // room to fill more of the buffer.
                if (offset >= halfCapacity) {
                    System.arraycopy(buffer, offset, buffer, 0, length);
                    offset = 0;
                }

                // Now fill any remaining capacity.
                int maxLength = capacity - offset;
                while (maxLength > length && (i = reader.read()) != -1) {
                    final char c = (char) i;

                    // Strip out carriage returns and other control characters.
                    if (c >= ' ' || c == '\n' || c == '\t') {
                        // If we have filled the buffer then double the size of
                        // it up to the maximum capacity.
                        if (buffer.length == offset + length) {
                            int newLen = buffer.length * 2;
                            if (newLen == 0) {
                                newLen = initialSize;
                            } else if (newLen > capacity) {
                                newLen = capacity;
                            }
                            final char[] tmp = new char[newLen];
                            System.arraycopy(buffer, offset, tmp, 0, length);
                            offset = 0;
                            buffer = tmp;

                            // Now the offset has been reset to 0 we should set
                            // the max length to be the maximum capacity.
                            maxLength = capacity;
                        }

                        buffer[offset + length] = c;
                        length++;
                    }
                }
            }

            // Determine if we reached the end of the file.
            eof = i == -1;
        }
    }

    public void close() throws IOException {
        reader.close();
    }

    public boolean isEof() {
        return eof;
    }

    @Override
    public void move(final int increment) {
        for (int i = 0; i < increment; i++) {
            final char c = buffer[offset + i];
            if (c == '\n') {
                currentLineNo++;
                currentColNo = 0;
            } else {
                currentColNo++;
            }
        }

        super.move(increment);
    }

    /**
     * Changes the current location of the reader so that any errors or warnings
     * can be linked to the current location.
     */
    public void changeLocation() {
        lineNo = currentLineNo;
        colNo = currentColNo;
    }

    @Override
    public int getLineNumber() {
        return lineNo;
    }

    @Override
    public int getColumnNumber() {
        return colNo;
    }

    public Locator getRecordEndLocator() {
        return new DefaultLocator() {
            @Override
            public int getLineNumber() {
                return currentLineNo;
            }

            @Override
            public int getColumnNumber() {
                return currentColNo;
            }
        };
    }
}
