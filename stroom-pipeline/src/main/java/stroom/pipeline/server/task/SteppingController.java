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

package stroom.pipeline.server.task;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.pipeline.server.LocationFactoryProxy;
import stroom.pipeline.server.errorhandler.ErrorReceiver;
import stroom.pipeline.server.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.server.errorhandler.LoggingErrorReceiver;
import stroom.pipeline.shared.SourceLocation;
import stroom.pipeline.shared.StepLocation;
import stroom.pipeline.shared.StepType;
import stroom.pipeline.state.LocationHolder;
import stroom.pipeline.state.StreamHolder;
import stroom.util.shared.DefaultLocation;
import stroom.util.shared.Highlight;
import stroom.util.spring.StroomScope;
import stroom.util.task.TaskMonitor;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Component
@Scope(value = StroomScope.TASK)
public class SteppingController {
    private static final Highlight DEFAULT_HIGHLIGHT = new Highlight(
            new DefaultLocation(1, 0),
            new DefaultLocation(1, 0));

    private final Set<ElementMonitor> monitors = new HashSet<>();

    @Resource
    private StreamHolder streamHolder;
    @Resource
    private LocationFactoryProxy locationFactory;
    @Resource
    private SteppingResponseCache steppingResponseCache;
    @Resource
    private ErrorReceiverProxy errorReceiverProxy;
    @Resource
    private LocationHolder locationHolder;

    private String streamInfo;
    private SteppingTask request;
    private StepLocation stepLocation;
    private StepLocation foundLocation;
    private RecordDetector recordDetector;

    private TaskMonitor taskMonitor;

    public void registerMonitor(final ElementMonitor monitor) {
        monitors.add(monitor);
    }

    public Set<ElementMonitor> getMonitors() {
        return monitors;
    }

    public RecordDetector getRecordDetector() {
        return recordDetector;
    }

    public void setRecordDetector(final RecordDetector recordDetector) {
        this.recordDetector = recordDetector;
    }

    public SteppingTask getRequest() {
        return request;
    }

    public void setRequest(final SteppingTask request) {
        this.request = request;
    }

    public void resetSourceLocation() {
        locationHolder.reset();
    }

    public TaskMonitor getTaskMonitor() {
        return taskMonitor;
    }

    public void setTaskMonitor(final TaskMonitor taskMonitor) {
        this.taskMonitor = taskMonitor;
    }

    public void setStepLocation(final StepLocation stepLocation) {
        this.stepLocation = stepLocation;
    }

    public StepLocation getLastFoundLocation() {
        return foundLocation;
    }

    public boolean isFound() {
        return foundLocation != null;
    }

    /**
     * Called by the step detector to tell us that we have reached the end of a
     * record.
     *
     * @return True if the step detector should terminate stepping.
     */
    public boolean endRecord(final long currentRecordNo) {
        // Get the current stream number.
        final long currentStreamNo = streamHolder.getStreamNo();

        // Update the progress monitor.
        if (getTaskMonitor() != null) {
            if (getTaskMonitor().isTerminated()) {
                return true;
            }
            getTaskMonitor().info("Processing stream - {} : [{}:{}]", streamInfo, currentStreamNo, currentRecordNo);
        }

        // Figure out what the highlighted portion of the input stream should be.
        Highlight highlight = DEFAULT_HIGHLIGHT;
        if (locationHolder != null && locationHolder.getCurrentLocation() != null) {
            highlight = locationHolder.getCurrentLocation().getHighlight();
        }

        // First we need to check that the record is ok WRT the location of the
        // record, i.e. is it after the last record found if stepping forward
        // etc.
        if (isRecordPositionOk(currentRecordNo)) {
            // Now that we have found an appropriate record to return lets check
            // to see if it matches any filters.
            boolean allMatch = true;
            boolean filterMatch = false;

            // We only want to filter records if we are stepping forward or
            // backward.
            if (!StepType.REFRESH.equals(request.getStepType())) {
                for (final ElementMonitor monitor : monitors) {
                    if (monitor.isFilterApplied()) {
                        if (monitor.filterMatches(currentRecordNo)) {
                            filterMatch = true;
                        } else {
                            allMatch = false;
                        }
                    }
                }
            }

            // We are going to make an early exit from the current parse if we
            // have found the record we are interested in and any filter matches
            // if one has been specified.
            if (allMatch || filterMatch) {
                // Create step data for the current step.
                final StepData stepData = createStepData(highlight);

                // Create a location for each monitoring filter to store data
                // against.
                foundLocation = new StepLocation(streamHolder.getStream().getId(), currentStreamNo, currentRecordNo);
                steppingResponseCache.setStepData(foundLocation, stepData);

                // We want to exit early if we have found a record and are
                // stepping first, forward or refreshing.
                if (!StepType.BACKWARD.equals(request.getStepType()) && !StepType.LAST.equals(request.getStepType())) {
                    return true;
                }
            }
        }

        // Reset all filters.
        clearAllFilters(highlight);

        // We want to exit early from backward stepping if we have got to the
        // previous record number.
        return StepType.BACKWARD.equals(request.getStepType()) && stepLocation != null
                && currentStreamNo == stepLocation.getPartNo() && currentRecordNo >= stepLocation.getRecordNo() - 1;

    }

    StepData createStepData(final Highlight highlight) {
        SourceLocation sourceLocation = null;
        if (stepLocation != null) {
            sourceLocation = new SourceLocation(stepLocation.getId(),
                    streamHolder.getChildStreamType(),
                    stepLocation.getPartNo(),
                    stepLocation.getRecordNo(),
                    highlight);
        }

        final StepData stepData = new StepData();
        stepData.setSourceLocation(sourceLocation);

        // Store the current data and reset for each filter.
        final LoggingErrorReceiver errorReceiver = getErrorReceiver();
        for (final ElementMonitor monitor : monitors) {
            final ElementData elementData = monitor.getElementData(errorReceiver, highlight);
            stepData.getElementMap().put(monitor.getElementId(), elementData);
        }

        return stepData;
    }

    /**
     * This method resets all filters so they are ready for the next record.
     */
    void clearAllFilters(final Highlight highlight) {
        // Store the current data for each filter.
        monitors.forEach(elementMonitor -> elementMonitor.clear(highlight));

        // Clear all indicators.
        final LoggingErrorReceiver loggingErrorReceiver = getErrorReceiver();
        if (loggingErrorReceiver != null) {
            loggingErrorReceiver.getIndicatorsMap().clear();
        }
    }

    private LoggingErrorReceiver getErrorReceiver() {
        final ErrorReceiver errorReceiver = errorReceiverProxy.getErrorReceiver();
        if (errorReceiver instanceof LoggingErrorReceiver) {
            return (LoggingErrorReceiver) errorReceiver;
        }
        return null;
    }

    /**
     * Used to decide if we have found a record that is in the appropriate
     * location, e.g. after the last returned record when stepping forward.
     */
    private boolean isRecordPositionOk(final long currentRecordNo) {
        // final PipelineStepTask request = controller.getRequest();
        final long currentStreamNo = streamHolder.getStreamNo();

        // If we aren't using a step location as a reference point to look
        // before or after then the location will always be ok.
        if (stepLocation == null) {
            return true;
        }

        // If we are moving forward and have got to a stream number that is
        // greater than the one we are looking for then the record position is
        // ok, likewise if we are going backward and the stream number is less
        // than the one requested then the record position is ok.

        // This is written as multiple tests for clarity.
        final StepType stepType = request.getStepType();
        if (StepType.FIRST.equals(stepType) || StepType.LAST.equals(stepType)) {
            return true;
        } else if (StepType.FORWARD.equals(stepType) && currentStreamNo > stepLocation.getPartNo()) {
            return true;
        } else if (StepType.BACKWARD.equals(stepType) && currentStreamNo < stepLocation.getPartNo()) {
            return true;
        }

        // If the stream number is the same as the one requested then we can
        // test the record number.
        if (currentStreamNo == stepLocation.getPartNo()) {
            if (StepType.REFRESH.equals(stepType) && currentRecordNo == stepLocation.getRecordNo()) {
                return true;
            } else if (StepType.FORWARD.equals(stepType) && currentRecordNo > stepLocation.getRecordNo()) {
                return true;
            } else if (StepType.BACKWARD.equals(stepType) && currentRecordNo < stepLocation.getRecordNo()) {
                return true;
            }
        }

        return false;
    }

    public void setStreamInfo(final String streamInfo) {
        this.streamInfo = streamInfo;
    }
}
