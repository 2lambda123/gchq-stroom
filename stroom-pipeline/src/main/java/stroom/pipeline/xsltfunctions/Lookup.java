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

package stroom.pipeline.xsltfunctions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import stroom.pipeline.refdata.LookupIdentifier;
import stroom.pipeline.refdata.ReferenceData;
import stroom.pipeline.refdata.ReferenceDataResult;
import stroom.pipeline.refdata.store.RefDataValueProxy;
import stroom.pipeline.refdata.store.RefDataValueProxyConsumerFactory;
import stroom.pipeline.state.MetaHolder;
import stroom.util.shared.Severity;

import javax.inject.Inject;

class Lookup extends AbstractLookup {

    @Inject
    Lookup(final ReferenceData referenceData,
           final MetaHolder metaHolder,
           final RefDataValueProxyConsumerFactory.Factory consumerFactoryFactory) {
        super(referenceData, metaHolder, consumerFactoryFactory);
    }

    @Override
    protected Sequence doLookup(final XPathContext context,
                                final boolean ignoreWarnings,
                                final boolean trace,
                                final LookupIdentifier lookupIdentifier) throws XPathException {
        // TODO rather than putting the proxy in the result we could just put the refStreamDefinition
        // in there and then do the actual lookup in the sequenceMaker by passing an injected RefDataStore
        // into it.
        final ReferenceDataResult result = getReferenceData(lookupIdentifier);

        final RefDataValueProxy refDataValueProxy = result.getRefDataValueProxy();

//        final SequenceMaker sequenceMaker = new SequenceMaker(context, getRefDataStore(), getConsumerFactory());
        final SequenceMaker sequenceMaker = new SequenceMaker(context, getRefDataValueProxyConsumerFactoryFactory());

        boolean wasFound = false;
        try {
            if (refDataValueProxy != null) {
                sequenceMaker.open();

                wasFound = sequenceMaker.consume(refDataValueProxy);

                sequenceMaker.close();

                if (wasFound && trace) {
                    outputInfo(Severity.INFO, "Lookup success ", lookupIdentifier, trace, result, context);
                }
            }
        } catch (XPathException e) {
            outputInfo(Severity.ERROR, "Lookup errored: " + e.getMessage(), lookupIdentifier, trace, result, context);
        }

        if (!wasFound && !ignoreWarnings) {
            outputInfo(Severity.WARNING, "Lookup failed ", lookupIdentifier, trace, result, context);
        }

        return sequenceMaker.toSequence();
    }
}
