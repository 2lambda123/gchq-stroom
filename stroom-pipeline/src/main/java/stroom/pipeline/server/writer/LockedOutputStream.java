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

package stroom.pipeline.server.writer;

import stroom.util.io.FileUtil;
import stroom.util.io.WrappedOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class LockedOutputStream extends WrappedOutputStream {
    private static final String UNABLE_TO_RENAME_FILE = "Unable to rename file \"";
    private static final String TO = "\" to \"";
    private static final String QUOTE = "\"";

    final Path lockFile;
    final Path outFile;

    LockedOutputStream(final OutputStream outputStream, final Path lockFile, final Path outFile) {
        super(outputStream);
        this.lockFile = lockFile;
        this.outFile = outFile;
    }

    @Override
    public void close() throws IOException {
        super.flush();
        super.close();

        try {
            Files.move(lockFile, outFile);
        } catch (final IOException e) {
            final String message = UNABLE_TO_RENAME_FILE +
                    FileUtil.getCanonicalPath(lockFile) +
                    TO +
                    FileUtil.getCanonicalPath(outFile) +
                    QUOTE;
            throw new IOException(message);
        }
    }
}
