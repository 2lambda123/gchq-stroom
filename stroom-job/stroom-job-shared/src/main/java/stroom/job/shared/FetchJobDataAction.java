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

package stroom.job.shared;

import stroom.job.api.Job;
import stroom.task.shared.Action;
import stroom.entity.shared.ResultList;

public class FetchJobDataAction extends Action<ResultList<JobNodeRow>> {
    private static final long serialVersionUID = -6808045615241590297L;

    private Job job;

    public FetchJobDataAction() {
        // Default constructor necessary for GWT serialisation.
    }

    @Override
    public String getTaskName() {
        return "Fetch job data";
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

}
