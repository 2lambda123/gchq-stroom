package stroom.security.api;

import stroom.util.shared.HasAuditableUserIdentity;
import stroom.util.shared.SimpleUserName;
import stroom.util.shared.UserName;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the identity of a user for back-end authentication purposes.
 */
public interface UserIdentity extends HasAuditableUserIdentity {

    /**
     * @return The unique identifier for the user. In the case of an Open ID Connect user
     * this would be the claim value that uniquely identifies the user on the IDP (often 'sub' or 'oid').
     * These values are often UUIDs and thus not pretty to look at for an admin.
     * For the internal IDP this would likely be a more human friendly username.
     */
    String getSubjectId();

    /**
     * @return Whether this {@link UserIdentity} represents a single user or a named group of users.
     */
    default boolean isGroup() {
        return false;
    }

    /**
     * @return The non-unique username for the user, e.g. 'jbloggs'. In the absence of a specific
     * value this should just return the subjectId.
     */
    default String getDisplayName() {
        return getSubjectId();
    }

    /**
     * @return The user's full name if known, e.g. 'Joe Bloggs'.
     */
    default Optional<String> getFullName() {
        return Optional.empty();
    }

    /**
     * A value for use in the UI.
     * If there is no {@code displayName}, this will return {@code name}.
     * If {@code displayName} is the same as {@code name}, this will return {@code name}.
     * Else it will return 'displayName (name)'.
     */
    default String getCombinedName() {
        // This logic is replicated in UserName
        final String id = getSubjectId();
        final String displayName = getDisplayName();
        if (displayName == null) {
            return id;
        } else if (Objects.equals(id, displayName)) {
            return displayName;
        } else {
            return displayName + " (" + id + ")";
        }
    }

    @Override
    default String getUserIdentityForAudit() {
        return HasAuditableUserIdentity.fromUserNames(getSubjectId(), getDisplayName());
    }

    // TODO: 28/11/2022 Potentially worth introducing scopes, e.g. a datafeed scope so only tokens
    //  with the datafeed scope can send data. Similarly we could have a scope per resource so people
    //  can create tokens that are very limited in what they can do. May need an 'api-all' scope to
    //  allow people to hit any resource.
//    /**
//     * @return The set of scopes that this user identity has. Scopes add restrictions
//     * on top of the things that a user has permission to do.
//     */
//    default Set<String> getScopes() {
//        return Collections.emptySet();
//    };

    default UserName asUserName() {
        final String subjectId = getSubjectId();
        String displayName = getDisplayName();
        if (Objects.equals(displayName, subjectId)) {
            displayName = null;
        }
        return new SimpleUserName(
                subjectId,
                displayName,
                getFullName().orElse(null),
                isGroup());
    }
}
