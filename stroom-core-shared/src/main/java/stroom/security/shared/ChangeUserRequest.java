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

package stroom.security.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import stroom.util.shared.UserRef;

@JsonPropertyOrder({"user", "changedLinkedUsers", "changedAppPermissions"})
@JsonInclude(Include.NON_NULL)
public class ChangeUserRequest {

    @JsonProperty
    private final UserRef user;
    @JsonProperty
    private final ChangeSet<UserRef> changedLinkedUsers;
    @JsonProperty
    private final ChangeSet<AppPermission> changedAppPermissions;

    @JsonCreator
    public ChangeUserRequest(@JsonProperty("user") final UserRef user,
                             @JsonProperty("changedLinkedUsers") final ChangeSet<UserRef> changedLinkedUsers,
                             @JsonProperty("changedAppPermissions") final ChangeSet<AppPermission>
                                     changedAppPermissions) {
        this.user = user;
        this.changedLinkedUsers = changedLinkedUsers;
        this.changedAppPermissions = changedAppPermissions;
    }

    public UserRef getUser() {
        return user;
    }

    public ChangeSet<UserRef> getChangedLinkedUsers() {
        return changedLinkedUsers;
    }

    public ChangeSet<AppPermission> getChangedAppPermissions() {
        return changedAppPermissions;
    }
}
