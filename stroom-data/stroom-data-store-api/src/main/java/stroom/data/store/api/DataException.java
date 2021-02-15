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

package stroom.data.store.api;

/**
 * Unchecked error in the data store.
 */
public class DataException extends RuntimeException {

    private static final long serialVersionUID = 4306974171835279325L;

    public DataException(final String msg) {
        super(msg);
    }

    public DataException(final String msg, final Throwable throwable) {
        super(msg, throwable);
    }

    public DataException(final Throwable throwable) {
        super(throwable);
    }

}
