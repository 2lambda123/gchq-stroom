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

package stroom.util.shared;


import stroom.util.shared.StringCriteria.MatchStyle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestStringCriteria {

    @Test
    void testSimple() {
        final StringCriteria criteria = new StringCriteria();
        assertThat(criteria.isConstrained()).isFalse();
        criteria.setString("");
        assertThat(criteria.isConstrained()).isTrue();
        criteria.setMatchStyle(MatchStyle.WildEnd);
        assertThat(criteria.isConstrained()).isFalse();
        criteria.setString("X");
        assertThat(criteria.isConstrained()).isTrue();

    }

    @Test
    void testIsMatch() {
        final StringCriteria criteria = new StringCriteria();
        criteria.setString("XYZ");

        assertThat(criteria.isMatch("XY")).isFalse();
        assertThat(criteria.isMatch("XYZ")).isTrue();

        criteria.setMatchStyle(MatchStyle.WildEnd);

        assertThat(criteria.isMatch("XYZ123")).isTrue();
        assertThat(criteria.isMatch("XYZ")).isTrue();
        assertThat(criteria.isMatch("123XYZ123")).isFalse();

        criteria.setMatchStyle(MatchStyle.WildStartAndEnd);

        assertThat(criteria.isMatch("XYZ123")).isTrue();
        assertThat(criteria.isMatch("123XYZ123")).isTrue();

    }

}
