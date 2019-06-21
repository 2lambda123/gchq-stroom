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
 */

package stroom.dashboard.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stroom.dashboard.shared.ComponentSettings;
import stroom.dashboard.shared.QueryComponentSettings;
import stroom.dashboard.shared.TableComponentSettings;
import stroom.dashboard.shared.TextComponentSettings;
import stroom.dashboard.shared.VisComponentSettings;
import stroom.dashboard.shared.Dashboard;
import stroom.dashboard.shared.FindDashboardCriteria;
import stroom.dashboard.shared.QueryEntity;
import stroom.entity.server.AutoMarshal;
import stroom.entity.server.DocumentEntityServiceImpl;
import stroom.entity.server.QueryAppender;
import stroom.entity.server.util.SqlBuilder;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.shared.DocRefUtil;
import stroom.importexport.server.ImportExportHelper;
import stroom.query.api.v2.DocRef;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.security.SecurityContext;
import stroom.util.io.StreamUtil;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional
@AutoMarshal
public class DashboardServiceImpl extends DocumentEntityServiceImpl<Dashboard, FindDashboardCriteria>
        implements DashboardService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final ResourceLoader resourceLoader;
    private String xmlTemplate;

    @Inject
    DashboardServiceImpl(final StroomEntityManager entityManager,
                         final ImportExportHelper importExportHelper,
                         final SecurityContext securityContext,
                         final ResourceLoader resourceLoader) {
        super(entityManager, importExportHelper, securityContext);
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Class<Dashboard> getEntityClass() {
        return Dashboard.class;
    }

    @Override
    public FindDashboardCriteria createCriteria() {
        return new FindDashboardCriteria();
    }

    @Override
    public DocRef copyDocument(final String originalUuid,
                               final String copyUuid,
                               final Map<String, String> otherCopiesByOriginalUuid,
                               final String parentFolderUUID) {
        final DocRef copiedDocRef = super.copyDocument(originalUuid,
                copyUuid,
                otherCopiesByOriginalUuid,
                parentFolderUUID);

        return makeCopyUuidReplacements(copiedDocRef,
                otherCopiesByOriginalUuid,
                Dashboard::getData,
                Dashboard::setData);
    }

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        final Set<DocRef> docs = super.listDocuments();
        return docs.stream().collect(Collectors.toMap(Function.identity(), this::getDependencies));
    }

    private Set<DocRef> getDependencies(final DocRef docRef) {
        final Set<DocRef> docRefs = Collections.newSetFromMap(new ConcurrentHashMap<>());

        try {
            final Dashboard dashboard = loadByUuid(docRef.getUuid());

            if (dashboard.getDashboardData() != null && dashboard.getDashboardData().getComponents() != null) {
                dashboard.getDashboardData().getComponents().forEach(component -> {
                    final ComponentSettings componentSettings = component.getSettings();
                    if (componentSettings != null) {
                        if (componentSettings instanceof QueryComponentSettings) {
                            final QueryComponentSettings queryComponentSettings = (QueryComponentSettings) componentSettings;
                            if (queryComponentSettings.getDataSource() != null) {
                                docRefs.add(queryComponentSettings.getDataSource());
                            }

                            if (queryComponentSettings.getExpression() != null) {
                                addExpressionRefs(docRefs, queryComponentSettings.getExpression());
                            }
                        } else if (componentSettings instanceof TableComponentSettings) {
                            final TableComponentSettings tableComponentSettings = (TableComponentSettings) componentSettings;
                            if (tableComponentSettings.getExtractionPipeline() != null) {
                                docRefs.add(tableComponentSettings.getExtractionPipeline());
                            }

                        } else if (componentSettings instanceof VisComponentSettings) {
                            final VisComponentSettings visComponentSettings = (VisComponentSettings) componentSettings;
                            if (visComponentSettings.getVisualisation() != null) {
                                docRefs.add(visComponentSettings.getVisualisation());
                            }

                        } else if (componentSettings instanceof TextComponentSettings) {
                            final TextComponentSettings textComponentSettings = (TextComponentSettings) componentSettings;
                            if (textComponentSettings.getPipeline() != null) {
                                docRefs.add(textComponentSettings.getPipeline());
                            }
                        }
                    }
                });
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return docRefs;
    }

    private void addExpressionRefs(final Set<DocRef> docRefs, final ExpressionItem expressionItem) {
        if (expressionItem instanceof ExpressionOperator) {
            final ExpressionOperator expressionOperator = (ExpressionOperator) expressionItem;
            if (expressionOperator.getChildren() != null) {
                expressionOperator.getChildren().forEach(item -> addExpressionRefs(docRefs, item));
            }
        } else if (expressionItem instanceof ExpressionTerm) {
            final ExpressionTerm expressionTerm = (ExpressionTerm) expressionItem;
            if (expressionTerm.getDocRef() != null) {
                docRefs.add(expressionTerm.getDocRef());
            }
        }
    }

//    @Override
//    protected Dashboard create(final Dashboard entity) {
//        final Dashboard dashboard = super.create(entity);
//        // Add the template.
//        if (dashboard.getData() == null) {
//            dashboard.setData(getTemplate());
//        }
//        return super.save(dashboard);
//    }
//
//    @Override
//    public Dashboard save(Dashboard entity) throws RuntimeException {
//        if (entity.getData() == null) {
//            entity.setData(getTemplate());
//        }
//        return super.save(entity);
//    }

    @Override
    public Boolean delete(final Dashboard entity) throws RuntimeException {
        checkDeletePermission(DocRefUtil.create(entity));

        // Delete associated queries first.
        final SqlBuilder sql = new SqlBuilder();
        sql.append("DELETE FROM ");
        sql.append(QueryEntity.TABLE_NAME);
        sql.append(" WHERE ");
        sql.append(QueryEntity.DASHBOARD_ID);
        sql.append(" = ");
        sql.arg(entity.getId());
        getEntityManager().executeNativeUpdate(sql);

        return super.delete(entity);
    }

    private String getTemplate() {
        if (xmlTemplate == null) {
            try {
                final org.springframework.core.io.Resource resource = resourceLoader
                        .getResource("classpath:/stroom/dashboard/DashboardTemplate.data.xml");
                xmlTemplate = StreamUtil.streamToString(resource.getInputStream());
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            // Don't try and load this template again if it fails.
            if (xmlTemplate == null) {
                xmlTemplate = "";
            }
        }

        return xmlTemplate;
    }

    @Override
    protected QueryAppender<Dashboard, FindDashboardCriteria> createQueryAppender(final StroomEntityManager entityManager) {
        return new DashboardQueryAppender(entityManager, this);
    }

    private static class DashboardQueryAppender extends QueryAppender<Dashboard, FindDashboardCriteria> {
        private final DashboardServiceImpl dashboardService;
        private final DashboardMarshaller marshaller;

        DashboardQueryAppender(final StroomEntityManager entityManager, final DashboardServiceImpl dashboardService) {
            super(entityManager);
            this.dashboardService = dashboardService;
            this.marshaller = new DashboardMarshaller();
        }

        @Override
        protected void postLoad(final Dashboard entity) {
            super.postLoad(entity);
            marshaller.unmarshal(entity);
        }

        @Override
        protected void preSave(final Dashboard entity) {
            super.preSave(entity);

            if (entity.getData() == null && entity.getDashboardData() == null) {
                entity.setData(dashboardService.getTemplate());
            } else {
                marshaller.marshal(entity);
            }
        }
    }
}
