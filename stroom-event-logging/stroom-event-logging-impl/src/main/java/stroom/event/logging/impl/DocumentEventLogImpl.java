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

package stroom.event.logging.impl;

import event.logging.BaseObject;
import event.logging.CopyMove;
import event.logging.CopyMoveOutcome;
import event.logging.Criteria;
import event.logging.Criteria.ResultPage;
import event.logging.Data;
import event.logging.Event;
import event.logging.Event.EventDetail.Update;
import event.logging.Export;
import event.logging.MultiObject;
import event.logging.Object;
import event.logging.ObjectOutcome;
import event.logging.Query;
import event.logging.Search;
import event.logging.util.EventLoggingUtil;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.docref.DocRef;
import stroom.event.logging.api.DocumentEventLog;
import stroom.event.logging.api.ObjectInfoProvider;
import stroom.event.logging.api.ObjectType;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.security.api.SecurityContext;
import stroom.util.shared.BaseCriteria;
import stroom.util.shared.HasId;
import stroom.util.shared.HasIntegerId;
import stroom.util.shared.HasName;
import stroom.util.shared.HasUuid;
import stroom.util.shared.PageResponse;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class DocumentEventLogImpl implements DocumentEventLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEventLogImpl.class);

    private final StroomEventLoggingService eventLoggingService;
    private final Map<ObjectType, Provider<ObjectInfoProvider>> objectInfoProviderMap;
    private final SecurityContext securityContext;

    @Inject
    public DocumentEventLogImpl(final StroomEventLoggingService eventLoggingService,
                                final Map<ObjectType, Provider<ObjectInfoProvider>> objectInfoProviderMap,
                                final SecurityContext securityContext) {
        this.eventLoggingService = eventLoggingService;
        this.objectInfoProviderMap = objectInfoProviderMap;
        this.securityContext = securityContext;
    }

    private ObjectInfoProvider getInfoAppender(final Class<?> type) {
        ObjectInfoProvider appender = null;

        // Some providers exist for superclasses and not subclass types so keep looking through the class hierarchy to find a provider.
        Class<?> currentType = type;
        Provider<ObjectInfoProvider> provider = null;
        while (currentType != null && provider == null) {
            provider = objectInfoProviderMap.get(new ObjectType(currentType));
            currentType = currentType.getSuperclass();
        }

        if (provider != null) {
            appender = provider.get();
        }

        if (appender == null) {
            LOGGER.error("No appender found for " + type.getName());
        }

        return appender;
    }

    @Override
    public void create(final String objectType, final String objectName, final Throwable ex) {
        create(objectType, objectName, "Create", ex);
    }
    @Override
    public void create(final String objectType, final String objectName, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Creating", objectType, objectName);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setCreate(objectOutcome);

                final Object object = new Object();
                object.setType(objectType);
                object.setName(objectName);

                objectOutcome.getObjects().add(object);
                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(ex));
                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to create event!", e);
            }
        });
    }

//    @Override
//    public void create(final java.lang.Object object) {
//        create(object, null);
//    }

    @Override
    public void create(final java.lang.Object object, final Throwable ex) {
        create(object, "Create", ex);
    }
        @Override
        public void create(final java.lang.Object object, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Creating", object);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setCreate(objectOutcome);
                final BaseObject baseObject = createBaseObject(object);
                objectOutcome.getObjects().add(baseObject);
                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(ex));
                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to create event!", e);
            }
        });
    }

//    @Override
//    public void update(final java.lang.Object before, final java.lang.Object after) {
//        update(before, after, null);
//    }

    @Override
    public void update(final java.lang.Object before, final java.lang.Object after, final Throwable ex) {
        update(before, after, "Update", ex);
    }

    @Override
    public void update(final java.lang.Object before, final java.lang.Object after, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Updating", before);
                final Update update = new Update();
                event.getEventDetail().setUpdate(update);

                if (before != null) {
                    final MultiObject bef = new MultiObject();
                    update.setBefore(bef);
                    bef.getObjects().add(createBaseObject(before));
                }

                if (after != null) {
                    final MultiObject aft = new MultiObject();
                    update.setAfter(aft);
                    aft.getObjects().add(createBaseObject(after));
                }

                update.setOutcome(EventLoggingUtil.createOutcome(ex));

                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to update event!", e);
            }
        });
    }

//    @Override
//    public void move(final java.lang.Object before, final java.lang.Object after) {
//        move(before, after, null);
//    }

    @Override
    public void copy(final java.lang.Object before, final java.lang.Object after, final Throwable ex) {
        copy(before, after, "Copy", ex);
    }

    @Override
    public void copy(final java.lang.Object before, final java.lang.Object after, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Copying", before);
                final CopyMove copy = new CopyMove();
                event.getEventDetail().setCopy(copy);

                if (before != null) {
                    final MultiObject source = new MultiObject();
                    copy.setSource(source);
                    source.getObjects().add(createBaseObject(before));
                }

                if (after != null) {
                    final MultiObject destination = new MultiObject();
                    copy.setDestination(destination);
                    destination.getObjects().add(createBaseObject(after));
                }

                if (ex != null && ex.getMessage() != null) {
                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
                    outcome.setSuccess(Boolean.FALSE);
                    outcome.setDescription(ex.getMessage());
                    copy.setOutcome(outcome);
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to copy event!", e);
            }
        });
    }

    @Override
    public void move(final java.lang.Object before, final java.lang.Object after, final Throwable ex) {
        move(before, after, "Move", ex);
    }

    @Override
    public void move(final java.lang.Object before, final java.lang.Object after, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Moving", before);
                final CopyMove move = new CopyMove();
                event.getEventDetail().setMove(move);

                if (before != null) {
                    final MultiObject source = new MultiObject();
                    move.setSource(source);
                    source.getObjects().add(createBaseObject(before));
                }

                if (after != null) {
                    final MultiObject destination = new MultiObject();
                    move.setDestination(destination);
                    destination.getObjects().add(createBaseObject(after));
                }

                if (ex != null && ex.getMessage() != null) {
                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
                    outcome.setSuccess(Boolean.FALSE);
                    outcome.setDescription(ex.getMessage());
                    move.setOutcome(outcome);
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to move event!", e);
            }
        });
    }

    @Override
    public void rename(final java.lang.Object before, final java.lang.Object after, final Throwable ex) {
        rename(before, after, "Rename", ex);
    }

    @Override
    public void rename(final java.lang.Object before, final java.lang.Object after, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Renaming", before);
                final CopyMove move = new CopyMove();
                event.getEventDetail().setMove(move);

                if (before != null) {
                    final MultiObject source = new MultiObject();
                    move.setSource(source);
                    source.getObjects().add(createBaseObject(before));
                }

                if (after != null) {
                    final MultiObject destination = new MultiObject();
                    move.setDestination(destination);
                    destination.getObjects().add(createBaseObject(after));
                }

                if (ex != null && ex.getMessage() != null) {
                    final CopyMoveOutcome outcome = new CopyMoveOutcome();
                    outcome.setSuccess(Boolean.FALSE);
                    outcome.setDescription(ex.getMessage());
                    move.setOutcome(outcome);
                }

                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to rename event!", e);
            }
        });
    }

    @Override
    public void delete(final java.lang.Object object, final Throwable ex) {
       delete(object, "Delete", ex);
    }

    @Override
    public void delete(final java.lang.Object object, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Deleting", object);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setDelete(objectOutcome);
                final BaseObject baseObject = createBaseObject(object);
                objectOutcome.getObjects().add(baseObject);
                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(ex));
                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to delete event!", e);
            }
        });
    }

    @Override
    public void view(final java.lang.Object object, final Throwable ex) {
        view(object, "View", ex);
    }

    @Override
    public void view(final java.lang.Object object, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Viewing", object);
                final ObjectOutcome objectOutcome = new ObjectOutcome();
                event.getEventDetail().setView(objectOutcome);
                final BaseObject baseObject = createBaseObject(object);
                objectOutcome.getObjects().add(baseObject);
                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(ex));
                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to view event!", e);
            }
        });
    }

    @Override
    public void delete(final BaseCriteria criteria, final Query query, final Long size, final Throwable ex) {
        delete(criteria, query, size, criteria.getClass().getSimpleName(), ex);
    }

    @Override
    public void delete(final BaseCriteria criteria, final Query query, final Long size, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Delete by criteria " + getObjectType(criteria),
                        null);

                final Criteria crit = new Criteria();
                crit.setQuery(query);
                if (size != null) {
                    crit.setTotalResults(BigInteger.valueOf(size));
                }

                final ObjectOutcome objectOutcome = new ObjectOutcome();
                objectOutcome.getObjects().add(crit);
                objectOutcome.setOutcome(EventLoggingUtil.createOutcome(ex));

                event.getEventDetail().setDelete(objectOutcome);

                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to log delete!", e);
            }
        });
    }

    @Override
    public void download(final java.lang.Object object, final Throwable ex) {
        download(object, "Download", ex);
    }

    @Override
    public void download(final java.lang.Object object, final String eventTypeId, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(eventTypeId, "Downloading", object);

                final MultiObject multiObject = new MultiObject();
                multiObject.getObjects().add(createBaseObject(object));

                final Export exp = new Export();
                exp.setSource(multiObject);
                exp.setOutcome(EventLoggingUtil.createOutcome(ex));

                event.getEventDetail().setExport(exp);

                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void search(final String typeId, final Query query, final String resultType, final PageResponse pageResponse, final Throwable ex) {
        securityContext.insecure(() -> {
            try {
                final Event event = createAction(typeId, "Finding " + resultType,
                        null);
                final Search search = new Search();
                event.getEventDetail().setSearch(search);
                search.setQuery(query);

                if (pageResponse != null) {
                    final ResultPage resultPage = getResultPage(pageResponse);
                    search.setResultPage(resultPage);
                    if (pageResponse.getTotal() != null) {
                        search.setTotalResults(BigInteger.valueOf(pageResponse.getTotal()));
                    }
                }

                search.setOutcome(EventLoggingUtil.createOutcome(ex));
                eventLoggingService.log(event);
            } catch (final RuntimeException e) {
                LOGGER.error("Unable to doSearch!", e);
            }
        });
    }

//    @Override
//    public void searchSummary(final BaseCriteria criteria, final Query query, final String resultType, final BaseResultList<?> results,
//                              final Throwable ex) {
//        securityContext.insecure(() -> {
//            try {
//                final Event event = createAction(criteria.getClass().getSimpleName(),
//                        "Finding Summary " + resultType, null);
//                final Search search = new Search();
//                event.getEventDetail().setSearch(search);
//                search.setQuery(query);
//
//                if (results != null && results.getPageResponse() != null) {
//                    final PageResponse pageResponse = results.getPageResponse();
//                    final ResultPage resultPage = getResultPage(pageResponse);
//                    search.setResultPage(resultPage);
//                    if (pageResponse.getTotal() != null) {
//                        search.setTotalResults(BigInteger.valueOf(pageResponse.getTotal()));
//                    }
//                }
//
//                search.setOutcome(EventLoggingUtil.createOutcome(ex));
//                eventLoggingService.log(event);
//            } catch (final RuntimeException e) {
//                LOGGER.error("Unable to doSearchSummary", e);
//            }
//        });
//    }

    private ResultPage getResultPage(final PageResponse pageResponse) {
        ResultPage resultPage = new ResultPage();
        resultPage.setFrom(BigInteger.valueOf(pageResponse.getOffset()));
        resultPage.setTo(BigInteger.valueOf(pageResponse.getOffset() + pageResponse.getLength()));
        return resultPage;
    }

    private Event createAction(final String typeId, final String description, final java.lang.Object object) {
        final StringBuilder desc = new StringBuilder(description);
        if (object != null) {
            final String objectType = getObjectType(object);
            if (objectType != null) {
                desc.append(" ");
                desc.append(objectType);
            }

            final String objectName = getObjectName(object);
            if (objectName != null) {
                desc.append(" \"");
                desc.append(objectName);
                desc.append("\"");
            }

            final String objectId = getObjectId(object);
            if (objectId != null) {
                desc.append(" id=");
                desc.append(objectId);
            }
        }

        return eventLoggingService.createAction(typeId, desc.toString());
    }

    private Event createAction(final String typeId, final String description, final String objectType,
                               final String objectName) {
        final String desc = description + " " + objectType + " \"" + objectName;
        return eventLoggingService.createAction(typeId, desc);
    }

    private String getObjectType(final java.lang.Object object) {
        if (object instanceof DocRef) {
            return String.valueOf(((DocRef) object).getType());
        }

        final ObjectInfoProvider objectInfoProvider = getInfoAppender(object.getClass());
        if (objectInfoProvider == null){
            if (object instanceof Collection){
                Collection collection = (Collection) object;
                if (collection.isEmpty()) {
                    return "Empty collection";
                } else {
                    return "Collection containing " + collection.stream().count()
                            + collection.stream().findFirst().get().getClass().getSimpleName() +
                            " and possibly other objects";
                }
            }
            return object.getClass().getSimpleName();
        }
        return objectInfoProvider.getObjectType(object);
    }

    private String getObjectName(final java.lang.Object object) {
        if (object instanceof DocRef) {
            return ((DocRef) object).getName();
        } else if  (object instanceof HasName){
            return ((HasName) object).getName();
        }

        return null;
    }

    private String getObjectId(final java.lang.Object object) {
        if (object instanceof HasUuid) {
            return ((HasUuid) object).getUuid();
        }

        if (object instanceof HasId) {
            return String.valueOf(((HasId) object).getId());
        }

        if (object instanceof HasIntegerId) {
            return String.valueOf(((HasIntegerId) object).getId());
        }

        if (object instanceof DocRef) {
            return String.valueOf(((DocRef) object).getUuid());
        }

        return null;
    }

    private BaseObject createBaseObject(final java.lang.Object object) {
        if (object == null) {
            return null;
        }
        final ObjectInfoProvider objectInfoAppender = getInfoAppender(object.getClass());
        if (objectInfoAppender == null) {
            return createDefaultBaseObject(object);
        }
        return objectInfoAppender.createBaseObject(object);
    }

    private BaseObject createDefaultBaseObject(final java.lang.Object object) {
        final event.logging.Object baseObj = new event.logging.Object();
        baseObj.setType(getObjectType(object));
        baseObj.setId(getObjectId(object));
        baseObj.setName(getObjectName(object));

        baseObj.getData().addAll(getDataItems(object));
        return baseObj;
    }

    private List<Data> getDataItems(java.lang.Object obj){

        try{
            final Map<String, java.lang.Object> allProps = PropertyUtils.describe(obj);
            return allProps.keySet().stream().map(propName -> {
                java.lang.Object val = allProps.get(propName);

                if (val == null){
                    return null;
                }

                Data d = new Data();
                d.setName(propName);
                d.setValue(val.toString());
                return d;
            }).collect(Collectors.toList());
        } catch (Exception ex) {
            return List.of();
        }
    }

}
