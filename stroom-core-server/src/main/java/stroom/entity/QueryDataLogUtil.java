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

package stroom.entity;

import event.logging.BaseAdvancedQueryItem;
import event.logging.BaseAdvancedQueryOperator;
import event.logging.BaseAdvancedQueryOperator.And;
import event.logging.BaseAdvancedQueryOperator.Not;
import event.logging.BaseAdvancedQueryOperator.Or;
import event.logging.TermCondition;
import event.logging.util.EventLoggingUtil;
import stroom.dictionary.DictionaryStore;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm;

import java.util.List;

public class QueryDataLogUtil {
    public static void appendExpressionItem(final List<BaseAdvancedQueryItem> items,
                                            final DictionaryStore dictionaryStore,
                                            final ExpressionItem item) {
        if (item == null) {
            return;
        }

        if (item.getEnabled()) {
            if (item instanceof ExpressionOperator) {
                appendOperator(items, dictionaryStore, (ExpressionOperator) item);
            } else {
                final ExpressionTerm expressionTerm = (ExpressionTerm) item;

                final String field = expressionTerm.getField();
                String value = expressionTerm.getValue();

                switch (expressionTerm.getCondition()) {
                    case EQUALS:
                        appendTerm(items, field, TermCondition.EQUALS, value);
                        break;
                    case CONTAINS:
                        appendTerm(items, field, TermCondition.CONTAINS, value);
                        break;
                    case GREATER_THAN:
                        appendTerm(items, field, TermCondition.GREATER_THAN, value);
                        break;
                    case GREATER_THAN_OR_EQUAL_TO:
                        appendTerm(items, field, TermCondition.GREATER_THAN_EQUAL_TO, value);
                        break;
                    case LESS_THAN:
                        appendTerm(items, field, TermCondition.LESS_THAN, value);
                        break;
                    case LESS_THAN_OR_EQUAL_TO:
                        appendTerm(items, field, TermCondition.LESS_THAN_EQUAL_TO, value);
                        break;
                    case BETWEEN:
                        appendTerm(items, field, TermCondition.CONTAINS, value);
                        break;
                    case IN:
                        appendTerm(items, field, TermCondition.CONTAINS, value);
                        break;
                    case IS_DOC_REF: {
                        final DocRef docRef = expressionTerm.getDocRef();
                        if (docRef != null) {
                            appendTerm(items, field, TermCondition.EQUALS, docRef.toInfoString());
                        }
                        break;
                    }
                    case IN_DICTIONARY:
                        if (dictionaryStore != null) {
                            DocRef docRef = expressionTerm.getDictionary();
                            if (docRef == null) {
                                final List<DocRef> docRefs = dictionaryStore.findByName(expressionTerm.getValue());
                                if (docRefs != null && docRefs.size() > 0) {
                                    docRef = docRefs.get(0);
                                }
                            }

                            if (docRef != null) {
                                final String words = dictionaryStore.getCombinedData(docRef);
                                if (words != null) {
                                    value += " (" + words + ")";
                                }

                                appendTerm(items, field, TermCondition.CONTAINS, value);

                            } else {
                                appendTerm(items, field, TermCondition.CONTAINS, "dictionary: " + value);
                            }

                        } else {
                            appendTerm(items, field, TermCondition.CONTAINS, "dictionary: " + value);
                        }
                        break;
                }
            }
        }
    }

    private static void appendOperator(final List<BaseAdvancedQueryItem> items,
                                       final DictionaryStore dictionaryStore,
                                       final ExpressionOperator exp) {
        BaseAdvancedQueryOperator operator;
        if (exp.getOp() == Op.NOT) {
            operator = new Not();
        } else if (exp.getOp() == Op.OR) {
            operator = new Or();
        } else {
            operator = new And();
        }

        items.add(operator);

        if (exp.getChildren() != null) {
            for (final ExpressionItem child : exp.getChildren()) {
                appendExpressionItem(operator.getAdvancedQueryItems(), dictionaryStore, child);
            }
        }
    }

    private static void appendTerm(final List<BaseAdvancedQueryItem> items, String field, TermCondition condition,
                                   String value) {
        if (field == null) {
            field = "";
        }
        if (condition == null) {
            condition = TermCondition.EQUALS;
        }
        if (value == null) {
            value = "";
        }
        items.add(EventLoggingUtil.createTerm(field, condition, value));
    }
}
