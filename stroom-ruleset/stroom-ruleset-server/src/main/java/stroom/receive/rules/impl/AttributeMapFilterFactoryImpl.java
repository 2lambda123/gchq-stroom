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

import stroom.receive.AttributeMapFilter;
import stroom.receive.AttributeMapFilterFactory;
import stroom.dictionary.api.DictionaryStore;
import stroom.docref.DocRef;

import javax.inject.Inject;

public class AttributeMapFilterFactoryImpl implements AttributeMapFilterFactory {
    private final ReceiveDataRuleSetService ruleSetService;
    private final DictionaryStore dictionaryStore;

    @Inject
    AttributeMapFilterFactoryImpl(final ReceiveDataRuleSetService ruleSetService,
                                  final DictionaryStore dictionaryStore) {
        this.ruleSetService = ruleSetService;
        this.dictionaryStore = dictionaryStore;
    }

    @Override
    public AttributeMapFilter create() {
        return new AttributeMapFilterImpl(null);
    }

    @Override
    public AttributeMapFilter create(final DocRef policyRef) {
        return new AttributeMapFilterImpl(new ReceiveDataPolicyChecker(ruleSetService, dictionaryStore, policyRef));
    }
}
