package stroom.security.impl;

import stroom.security.shared.FindUserCriteria;
import stroom.security.shared.User;
import stroom.util.filter.FilterFieldMapper;
import stroom.util.filter.FilterFieldMappers;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public interface UserDao {

    FilterFieldMappers<User> FILTER_FIELD_MAPPERS = FilterFieldMappers.of(
            FilterFieldMapper.of(FindUserCriteria.FIELD_DEF_NAME, User::getName),
            FilterFieldMapper.of(FindUserCriteria.FIELD_DEF_PREFERRED_USERNAME, User::getDisplayName),
            FilterFieldMapper.of(FindUserCriteria.FIELD_DEF_FULL_NAME, User::getFullName));

    User create(User user);

    default User tryCreate(User user) {
        return tryCreate(user, null);
    }

    User tryCreate(User user, final Consumer<User> onUserCreateAction);

    Optional<User> getById(int id);

    Optional<User> getByUuid(String uuid);

    Optional<User> getByName(String name);

    /**
     * Gets by displayName, falling back to
     * @param displayName
     * @return
     */
    Optional<User> getByDisplayName(String displayName);

    Optional<User> getByName(String name, boolean isGroup);

    User update(User user);

    void delete(String uuid);

    List<User> find(String quickFilter, boolean isGroup);

    List<User> findUsersInGroup(String groupUuid, String quickFilterInput);

    List<User> findGroupsForUser(String userUuid, String quickFilterInput);

    Set<String> findGroupUuidsForUser(String userUuid);

    List<User> findGroupsForUserName(String userName);

    void addUserToGroup(String userUuid, String groupUuid);

    void removeUserFromGroup(String userUuid, String groupUuid);
}
