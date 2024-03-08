/*
 * Copyright 2017 Crown Copyright
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

package stroom.receive.rules.impl;

import stroom.docref.DocRef;
import stroom.expression.matcher.ExpressionMatcherFactory;
import stroom.receive.common.AttributeMapFilter;
import stroom.receive.common.DataReceiptPolicyAttributeMapFilterFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DataReceiptPolicyAttributeMapFilterFactoryImpl implements DataReceiptPolicyAttributeMapFilterFactory {

    private final ReceiveDataRuleSetService ruleSetService;
    private final ExpressionMatcherFactory expressionMatcherFactory;

    @Inject
    public DataReceiptPolicyAttributeMapFilterFactoryImpl(final ReceiveDataRuleSetService ruleSetService,
                                                          final ExpressionMatcherFactory expressionMatcherFactory) {
        this.ruleSetService = ruleSetService;
        this.expressionMatcherFactory = expressionMatcherFactory;
    }

    @Override
    public AttributeMapFilter create(final DocRef policyRef) {
        return new DataReceiptPolicyAttributeMapFilter(new ReceiveDataPolicyChecker(ruleSetService,
                expressionMatcherFactory,
                policyRef));
    }
}
