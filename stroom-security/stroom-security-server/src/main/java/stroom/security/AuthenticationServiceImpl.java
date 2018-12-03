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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.security.shared.FindUserCriteria;
import stroom.security.shared.PermissionNames;
import stroom.security.shared.UserRef;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.PersistenceException;

@Singleton
class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private static final String ADMINISTRATORS = "Administrators";
    private static final int GET_USER_ATTEMPTS = 2;

    private final UserService userService;
    private final UserAppPermissionService userAppPermissionService;
    private final Security security;

    @Inject
    AuthenticationServiceImpl(
            final UserService userService,
            final UserAppPermissionService userAppPermissionService,
            final Security security) {
        this.userService = userService;
        this.userAppPermissionService = userAppPermissionService;
        this.security = security;
    }

    @Override
    public UserRef getUserRef(final AuthenticationToken token) {
        if (token == null || token.getUserId() == null || token.getUserId().trim().length() == 0) {
            return null;
        }

        // Race conditions can mean that multiple processes kick off the creation of a user
        // The first one will succeed, but the others may clash. So we retrieve/create the user
        // in a loop to allow the failures caused by the race to be absorbed without failure
        int attempts = 0;
        UserRef userRef = null;

        while (userRef == null) {
            userRef = loadUserByUsername(token.getUserId());

            if (userRef == null) {
                // At this point the user has been authenticated using JWT.
                // If the user doesn't exist in the DB then we need to create them an account here, so Stroom has
                // some way of sensibly referencing the user and something to attach permissions to.
                // We need to elevate the user because no one is currently logged in.
                try {
                    userRef = security.asProcessingUserResult(() -> userService.createUser(token.getUserId()));
                } catch (final PersistenceException e) {
                    final String msg = String.format("Could not create user, this is attempt %d", attempts);
                    if (attempts == 0) {
                        LOGGER.warn(msg);
                    } else {
                        LOGGER.info(msg);
                    }
                }
            }

            if (attempts++ > GET_USER_ATTEMPTS) {
                break;
            }
        }

        return userRef;
    }

    private UserRef loadUserByUsername(final String username) {
        UserRef userRef;

        try {
            userRef = userService.getUserByName(username);
            if (userRef == null) {
                // The requested system user does not exist.
                if (UserService.ADMIN_USER_NAME.equals(username)) {
                    userRef = createOrRefreshUser(UserService.ADMIN_USER_NAME);
                } else if (UserService.STROOM_SERVICE_USER_NAME.equals(username)) {
                    userRef = createOrRefreshUser(UserService.STROOM_SERVICE_USER_NAME);
                }
            }

        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;
        }

        return userRef;
    }

    private UserRef createOrRefreshUser(String name) {
        return security.asProcessingUserResult(() -> {
            // Ensure all perms have been created
            userAppPermissionService.init();

            UserRef userRef = userService.getUserByName(name);
            if (userRef == null) {
                User user = new User();
                user.setName(name);
                user = userService.save(user);

                final UserRef userGroup = createOrRefreshAdminUserGroup();
                try {
                    userService.addUserToGroup(UserRefFactory.create(user), userGroup);
                } catch (final RuntimeException e) {
                    // Expected.
                    LOGGER.debug(e.getMessage());
                }

                userRef = UserRefFactory.create(user);
            }

            return userRef;
        });
    }

    /**
     * Enusure the admin user groups are created
     *
     * @return the full admin user group
     */
    private UserRef createOrRefreshAdminUserGroup() {
        return createOrRefreshAdminUserGroup(ADMINISTRATORS);
    }

    private UserRef createOrRefreshAdminUserGroup(final String userGroupName) {
        return security.asProcessingUserResult(() -> {
            final FindUserCriteria findUserGroupCriteria = new FindUserCriteria(userGroupName, true);
            findUserGroupCriteria.getFetchSet().add(Permission.ENTITY_TYPE);

            final User userGroup = userService.find(findUserGroupCriteria).getFirst();
            if (userGroup != null) {
                return UserRefFactory.create(userGroup);
            }

            UserRef newUserGroup = userService.createUserGroup(userGroupName);
            try {
                userAppPermissionService.addPermission(newUserGroup, PermissionNames.ADMINISTRATOR);
            } catch (final RuntimeException e) {
                // Expected.
                LOGGER.debug(e.getMessage());
            }

            return newUserGroup;
        });
    }
}
