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

package stroom.security;

import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.ResultList;
import stroom.security.shared.FetchUserRefAction;
import stroom.security.shared.FindUserCriteria;
import stroom.security.model.PermissionNames;
import stroom.security.model.User;
import stroom.security.shared.UserRef;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


class FetchUserRefHandler
        extends AbstractTaskHandler<FetchUserRefAction, ResultList<UserRef>> {
    private final UserService userService;
    private final Security security;

    @Inject
    FetchUserRefHandler(final UserService userService,
                        final Security security) {
        this.userService = userService;
        this.security = security;
    }

    @Override
    public ResultList<UserRef> exec(final FetchUserRefAction action) {
        return security.secureResult(PermissionNames.MANAGE_USERS_PERMISSION, () -> {
            final FindUserCriteria findUserCriteria = action.getCriteria();
            findUserCriteria.setSort(FindUserCriteria.FIELD_NAME);
            if (findUserCriteria.getRelatedUser() != null) {
                UserRef userRef = findUserCriteria.getRelatedUser();
                List<UserRef> list;
                if (userRef.isGroup()) {
                    list = userService.findUsersInGroup(userRef);
                } else {
                    list = userService.findGroupsForUser(userRef);
                }

                if (action.getCriteria().getName() != null) {
                    list = list.stream().filter(user -> action.getCriteria().getName().isMatch(user.getName())).collect(Collectors.toList());
                }

                // Create a result list limited by the page request.
                return BaseResultList.createPageLimitedList(list, findUserCriteria.getPageRequest());
            }

            final List<User> users = userService.find(findUserCriteria);
            final List<UserRef> userRefs = new ArrayList<>();
            users.forEach(user -> userRefs.add(UserRefFactory.create(user)));
            return new BaseResultList<>(userRefs, 0L, (long) users.size(), false);
        });
    }
}
