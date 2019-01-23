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

package stroom.data.store.impl.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.data.meta.api.AttributeMap;
import stroom.data.meta.api.Data;
import stroom.data.meta.api.DataStatus;
import stroom.data.store.api.CompoundInputStream;
import stroom.data.store.api.NestedInputStream;
import stroom.data.store.api.SegmentInputStream;
import stroom.data.store.api.StreamSource;
import stroom.data.store.api.StreamSourceInputStreamProvider;
import stroom.data.meta.api.AttributeMapUtil;
import stroom.io.BasicStreamCloser;
import stroom.io.StreamCloser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A file system implementation of StreamSource.
 */
final class FileSystemStreamSource implements StreamSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStreamSource.class);

    private final FileSystemStreamPathHelper fileSystemStreamPathHelper;
    private final StreamCloser streamCloser = new BasicStreamCloser();
    private Data stream;
    private String rootPath;
    private String streamType;
    private AttributeMap attributeMap;
    private InputStream inputStream;
    private Path file;
    private FileSystemStreamSource parent;

    private FileSystemStreamSource(final FileSystemStreamPathHelper fileSystemStreamPathHelper,
                                   final Data stream,
                                   final String rootPath,
                                   final String streamType) {
        this.fileSystemStreamPathHelper = fileSystemStreamPathHelper;
        this.stream = stream;
        this.rootPath = rootPath;
        this.streamType = streamType;

        validate();
    }

    private FileSystemStreamSource(final FileSystemStreamPathHelper fileSystemStreamPathHelper,
                                   final FileSystemStreamSource parent,
                                   final String streamType,
                                   final Path file) {
        this.fileSystemStreamPathHelper = fileSystemStreamPathHelper;
        this.stream = parent.stream;
        this.rootPath = parent.rootPath;
        this.parent = parent;
        this.streamType = streamType;
        this.file = file;
        validate();
    }

    /**
     * Creates a new file system stream source.
     *
     * @return A new file system stream source or null if a file cannot be
     * created.
     */
    static FileSystemStreamSource create(final FileSystemStreamPathHelper fileSystemStreamPathHelper,
                                         final Data stream,
                                         final String rootPath,
                                         final String streamType) {
        return new FileSystemStreamSource(fileSystemStreamPathHelper, stream, rootPath, streamType);
    }

    private void validate() {
        if (streamType == null) {
            throw new IllegalStateException("Must have a stream type");
        }
    }

    @Override
    public void close() throws IOException {
        streamCloser.close();
    }

    public Path getFile() {
        if (file == null) {
            if (parent == null) {
                file = fileSystemStreamPathHelper.createRootStreamFile(rootPath, stream, getStreamTypeName());
            } else {
                file = fileSystemStreamPathHelper.createChildStreamFile(parent.getFile(), getStreamTypeName());
            }
        }
        return file;
    }

    @Override
    public InputStream getInputStream() {
        // First Call?
        if (inputStream == null) {
            try {
                inputStream = fileSystemStreamPathHelper.getInputStream(streamType, getFile());
                streamCloser.add(inputStream);
            } catch (IOException ioEx) {
                // Don't log this as an error if we expect this stream to have been deleted or be locked.
                if (stream == null || DataStatus.UNLOCKED.equals(stream.getStatus())) {
                    LOGGER.error("getInputStream", ioEx);
                }

                throw new RuntimeException(ioEx);
            }
        }
        return inputStream;
    }

    @Override
    public NestedInputStream getNestedInputStream() throws IOException {
        final InputStream data = getInputStream();
        final InputStream boundaryIndex = getChildStream(InternalStreamTypeNames.BOUNDARY_INDEX).getInputStream();
        return new RANestedInputStream(data, boundaryIndex);
    }

    @Override
    public SegmentInputStream getSegmentInputStream() throws IOException {
        final InputStream data = getInputStream();
        final InputStream segmentIndex = getChildStream(InternalStreamTypeNames.SEGMENT_INDEX).getInputStream();
        return new RASegmentInputStream(data, segmentIndex);
    }

    @Override
    public CompoundInputStream getCompoundInputStream() throws IOException {
        final InputStream data = getInputStream();
        final InputStream boundaryIndex = getChildStream(InternalStreamTypeNames.BOUNDARY_INDEX).getInputStream();
        final InputStream segmentIndex = getChildStream(InternalStreamTypeNames.SEGMENT_INDEX).getInputStream();
        final RANestedInputStream nestedInputStream = new RANestedInputStream(data, boundaryIndex);
        return new RACompoundInputStream(nestedInputStream, segmentIndex);
    }

    @Override
    public StreamSourceInputStreamProvider getInputStreamProvider() {
        return new StreamSourceInputStreamProviderImpl(this);
    }

    @Override
    public Data getStream() {
        return stream;
    }

    public void setStream(final Data stream) {
        this.stream = stream;
    }

    @Override
    public String getStreamTypeName() {
        return streamType;
    }

    @Override
    public StreamSource getChildStream(final String streamTypeName) {
        Path childFile = fileSystemStreamPathHelper.createChildStreamFile(getFile(), streamTypeName);
        boolean lazy = fileSystemStreamPathHelper.isStreamTypeLazy(streamTypeName);
        boolean isFile = Files.isRegularFile(childFile);
        if (lazy || isFile) {
            final FileSystemStreamSource child = new FileSystemStreamSource(fileSystemStreamPathHelper, this, streamTypeName, childFile);
            streamCloser.add(child);
            return child;
        } else {
            return null;
        }
    }

    @Override
    public AttributeMap getAttributes() {
        if (parent != null) {
            return parent.getAttributes();
        }
        if (attributeMap == null) {
            attributeMap = new AttributeMap();
            try {
                final StreamSource streamSource = getChildStream(InternalStreamTypeNames.MANIFEST);
                if (streamSource != null) {
                    AttributeMapUtil.read(streamSource.getInputStream(), true, attributeMap);
                }
            } catch (final RuntimeException | IOException e) {
                LOGGER.error("getAttributes()", e);
            }
        }
        return attributeMap;
    }

    StreamSource getParent() {
        return parent;
    }
}
