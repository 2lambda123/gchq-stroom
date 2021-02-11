
/*
 *
 *  * Copyright 2018 Crown Copyright
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package stroom.processor.impl;

import stroom.entity.shared.ExpressionCriteria;
import stroom.processor.api.ProcessorFilterService;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.ProcessorFilterFields;
import stroom.processor.shared.ProcessorTaskFields;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.security.api.SecurityContext;
import stroom.util.logging.LogUtil;
import stroom.util.shared.ResourcePaths;
import stroom.util.shared.RestResource;
import stroom.util.shared.ResultPage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.util.Comparator.comparingInt;
import static stroom.processor.impl.SearchKeywords.SORT_NEXT;
import static stroom.processor.impl.SearchKeywords.addFiltering;
import static stroom.processor.impl.SearchKeywords.addSorting;

@Api(value = "stream task - /v1")
@Path("/streamtasks" + ResourcePaths.V1)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StreamTaskResource implements RestResource {

    private static final String FIELD_PROGRESS = "progress";

    private final ProcessorFilterService processorFilterService;
    private final SecurityContext securityContext;

    @Inject
    public StreamTaskResource(
            ProcessorFilterService processorFilterService,
            SecurityContext securityContext) {
        this.processorFilterService = processorFilterService;
        this.securityContext = securityContext;
    }

    @PATCH
    @Path("/{filterId}")
    public Response enable(
            @PathParam("filterId") int filterId,
            StreamTaskPatch patch) {

        return processorFilterService.fetch(filterId)
                .map(processorFilter -> {
                    boolean patchApplied = false;
                    if (patch.getOp().equalsIgnoreCase("replace")) {
                        if (patch.getPath().equalsIgnoreCase("enabled")) {
                            processorFilter.setEnabled(Boolean.parseBoolean(patch.getValue()));
                            patchApplied = true;
                        }
                    }

                    if (patchApplied) {
                        processorFilterService.update(processorFilter);
                        return Response
                                .ok()
                                .build();
                    } else {
                        return Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity("Unable to apply the requested patch. See server logs for details.")
                                .build();
                    }
                })
                .orElseGet(() ->
                        Response
                                .status(Response.Status.NOT_FOUND)
                                .entity(LogUtil.message("Filter with ID {} could not be found", filterId))
                                .build());
    }

    @GET
    public Response fetch(
            @QueryParam("offset") Integer offset,
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("desc") boolean desc,
            @QueryParam("filter") String filter) {
        // TODO: Authorisation

        final ExpressionCriteria criteria = new ExpressionCriteria();

        // SORTING
        if (sortBy != null) {
            if (sortBy.equalsIgnoreCase(ProcessorTaskFields.FIELD_PIPELINE)
                    || sortBy.equalsIgnoreCase(ProcessorTaskFields.FIELD_PRIORITY)) {
                criteria.setSort(sortBy, desc, false);
            } else if (sortBy.equalsIgnoreCase(FIELD_PROGRESS)) {
                // Sorting progress is done below -- this is here for completeness.
                // Percentage is a calculated variable so it has to be done after retrieval.
                // This poses a problem for paging and at the moment sorting by tracker % won't work correctly when paging.
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid sortBy field").build();
            }
        }

        // PAGING
        if (offset < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Page offset must be greater than 0").build();
        }
        if (pageSize != null && pageSize < 1) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Page size, if used, must be greater than 1").build();
        }

        final ExpressionOperator.Builder builder = ExpressionOperator.builder();
        addFiltering(filter, builder);

        addSorting(filter, criteria);

        if (!securityContext.isAdmin()) {
            builder.addTerm(ProcessorFilterFields.CREATE_USER, Condition.EQUALS, securityContext.getUserId());
        }

        criteria.setExpression(builder.build());

        // We have to load everything because we need to sort by progress, and we can't do that on the database.
        final List<StreamTask> values = find(criteria);

        if (sortBy != null) {
            // If the user is requesting a sort:next then we don't want to apply any other sorting.
            if (sortBy.equalsIgnoreCase(FIELD_PROGRESS) && !filter.contains(SORT_NEXT)) {
                if (desc) {
                    values.sort(comparingInt(StreamTask::getTrackerPercent).reversed());
                } else {
                    values.sort(comparingInt(StreamTask::getTrackerPercent));
                }
            }
        }

        int from = offset * pageSize;
        int to = (offset * pageSize) + pageSize;
        if (values.size() <= to) {
            to = values.size();
        }
        // PAGING
        List<StreamTask> pageToReturn = values.subList(from, to);

        final StreamTasks response = new StreamTasks(pageToReturn, values.size());

        return Response.ok(response).build();
    }

    private List<StreamTask> find(final ExpressionCriteria criteria) {

        final ResultPage<ProcessorFilter> processorFilters = processorFilterService
                .find(criteria);


        List<StreamTask> streamTasks = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (ProcessorFilter filter : processorFilters.getValues()) {
            StreamTask.StreamTaskBuilder builder = StreamTask.builder();

            // Indented to make the source easier to read
            builder
                    .withPipelineName(filter.getProcessor().getPipelineUuid())
                    //.withPipelineId(     filter.getProcessor().getPipeline().getId())
                    .withPriority(filter.getPriority())
                    .withEnabled(filter.isEnabled())
                    .withFilterId(filter.getId())
                    .withCreateUser(filter.getCreateUser())
                    .withCreatedOn(filter.getCreateTimeMs())
                    .withUpdateUser(filter.getUpdateUser())
                    .withUpdatedOn(filter.getUpdateTimeMs())
                    .withFilter(filter.getQueryData());

            if (filter.getProcessorFilterTracker() != null) {
                Integer trackerPercent = filter.getProcessorFilterTracker().getTrackerStreamCreatePercentage();
                if (trackerPercent == null) {
                    trackerPercent = 0;
                }
                builder.withTrackerMs(filter.getProcessorFilterTracker().getMetaCreateMs())
                        .withTrackerPercent(trackerPercent)
                        .withLastPollAge(filter.getProcessorFilterTracker().getLastPollAge())
                        .withTaskCount(filter.getProcessorFilterTracker().getLastPollTaskCount())
                        .withMinStreamId(filter.getProcessorFilterTracker().getMinMetaId())
                        .withMinEventId(filter.getProcessorFilterTracker().getMinEventId())
                        .withStatus((filter.getProcessorFilterTracker().getStatus()));
            }

            StreamTask streamTask = builder.build();
            streamTasks.add(streamTask);
        }

        return streamTasks;
    }

}
