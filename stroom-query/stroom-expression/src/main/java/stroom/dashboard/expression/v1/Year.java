/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.dashboard.expression.v1;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = Year.NAME,
        commonCategory = FunctionCategory.DATE,
        commonSubCategories = "Variable",
        commonReturnType = ValDate.class,
        commonReturnDescription = "The current date and time truncated to the start of the year.",
        signatures = @FunctionSignature(
                description = "Returns the current date and time truncated to the start of the year.",
                args = {}))
class Year extends AbstractTimeFunction {

    static final String NAME = "year";

    private final Generator generator;

    public Year(final String name) {
        super(name);
        final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        final ZonedDateTime time = ZonedDateTime.of(now.getYear(), 1, 1, 0, 0, 0, 0, now.getZone());
        generator = new StaticValueGen(ValDate.create(time.toInstant().toEpochMilli()));
    }

    @Override
    public Generator createGenerator() {
        return generator;
    }
}
