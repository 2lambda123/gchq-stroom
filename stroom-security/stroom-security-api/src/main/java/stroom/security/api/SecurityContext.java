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

package stroom.security.api;

import stroom.security.shared.UserToken;

import java.util.function.Supplier;

public interface SecurityContext {
    /**
     * Get the id of the user associated with this security context.
     *
     * @return The id of the user associated with this security context.
     */
    String getUserId();

    /**
     * Get the current user token associated with this security context.
     *
     * @return The current user token associated with this security context.
     */
    UserToken getUserToken();

    /**
     * Gets an API token string for the current user.
     *
     * @return An API token string for the current user.
     */
    String getApiToken();

    /**
     * Check if the user associated with this security context is logged in.
     *
     * @return True if the user is logged in.
     */
    boolean isLoggedIn();

    /**
     * This is a convenience method to check that the user has system administrator privileges.
     *
     * @return True if the current user is an administrator.
     */
    boolean isAdmin();

    /**
     * Check if the user associated with this security context has the requested
     * permission to use the specified functionality.
     *
     * @param permission The permission we are checking for.
     * @return True if the user associated with the security context has the
     * requested permission.
     */
    boolean hasAppPermission(String permission);

    /**
     * Check if the user associated with this security context has the requested
     * permission on the document specified by the document type and document
     * id.
     *
     * @param documentType The type of document.
     * @param documentUuid The id of the document.
     * @param permission   The permission we are checking for.
     * @return True if the user associated with the security context has the
     * requested permission.
     */
    boolean hasDocumentPermission(String documentType, String documentUuid, String permission);

    <T> T asUserResult(UserToken userToken, Supplier<T> supplier);

    void asUser(UserToken userToken, Runnable runnable);

    <T> T asProcessingUserResult(Supplier<T> supplier);

    void asProcessingUser(Runnable runnable);

    <T> T useAsReadResult(Supplier<T> supplier);

    void useAsRead(Runnable runnable);

    void secure(String permission, Runnable runnable);

    <T> T secureResult(String permission, Supplier<T> supplier);

    void secure(Runnable runnable);

    <T> T secureResult(Supplier<T> supplier);

    void insecure(Runnable runnable);

    <T> T insecureResult(Supplier<T> supplier);
}
