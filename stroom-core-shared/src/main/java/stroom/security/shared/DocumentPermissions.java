package stroom.security.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@JsonPropertyOrder({"docUuid", "users", "groups", "permissions"})
@JsonInclude(Include.NON_NULL)
public class DocumentPermissions {

    @JsonProperty
    private final String docUuid;
    @JsonProperty
    private final List<User> users;
    @JsonProperty
    private final List<User> groups;
    @JsonProperty
    private final Map<String, Set<String>> permissions;

    @JsonCreator
    public DocumentPermissions(@JsonProperty("docUuid") final String docUuid,
                               @JsonProperty("users") final List<User> users,
                               @JsonProperty("groups") final List<User> groups,
                               @JsonProperty("permissions") final Map<String, Set<String>> permissions) {
        this.docUuid = docUuid;
        this.users = users;
        this.groups = groups;
        this.permissions = permissions;
    }

    public String getDocUuid() {
        return docUuid;
    }

    public List<User> getUsers() {
        return users;
    }

    public List<User> getGroups() {
        return groups;
    }

    public Map<String, Set<String>> getPermissions() {
        return permissions;
    }

    public boolean containsUserOrGroup(final String uuid, final boolean isGroup) {
        return isGroup
                ? containsGroup(uuid)
                : containsUser(uuid);
    }

    public boolean containsUser(final String userUuid) {
        return users.stream()
                .map(User::getUuid)
                .anyMatch(uuid -> Objects.equals(uuid, userUuid));
    }

    public boolean containsGroup(final String groupUuid) {
        return groups.stream()
                .map(User::getUuid)
                .anyMatch(uuid -> Objects.equals(uuid, groupUuid));
    }

    public Set<String> getPermissionsForUser(final String userUuid) {
        return permissions.getOrDefault(userUuid, Collections.emptySet());
    }

    public void addUser(final User user) {
        users.add(user);
        permissions.putIfAbsent(user.getUuid(), new HashSet<>());
    }

    public void addGroup(final User group) {
        groups.add(group);
        permissions.putIfAbsent(group.getUuid(), new HashSet<>());
    }

    public void addUser(final User user, final boolean isGroup) {
        if (isGroup) {
            addGroup(user);
        } else {
            addUser(user);
        }
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DocumentPermissions that = (DocumentPermissions) o;
        return Objects.equals(docUuid, that.docUuid) &&
                Objects.equals(users, that.users) &&
                Objects.equals(groups, that.groups) &&
                Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docUuid, users, groups, permissions);
    }

    @Override
    public String toString() {
        return "DocumentPermissions{" +
                "docUuid='" + docUuid + '\'' +
                ", users=" + users +
                ", groups=" + groups +
                ", permissions=" + permissions +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder {

        private String docUuid;
        private List<User> users;
        private List<User> groups;
        private Map<String, Set<String>> permissions = new HashMap<>();

        private Builder() {
        }

        private Builder(final DocumentPermissions documentPermissions) {
            this.docUuid = documentPermissions.docUuid;
            this.users = documentPermissions.users;
            this.groups = documentPermissions.groups;
            this.permissions = documentPermissions.permissions;
        }

        public Builder docUuid(final String value) {
            docUuid = value;
            return this;
        }

        public Builder users(final List<User> value) {
            users = value;
            return this;
        }

        public Builder groups(final List<User> value) {
            groups = value;
            return this;
        }

        public Builder permission(final String userUuid, final String permission) {
            permissions.computeIfAbsent(userUuid, k -> new HashSet<>()).add(permission);
            return this;
        }

        public DocumentPermissions build() {
            return new DocumentPermissions(docUuid, users, groups, permissions);
        }
    }
}
