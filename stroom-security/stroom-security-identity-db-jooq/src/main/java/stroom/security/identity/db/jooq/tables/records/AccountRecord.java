/*
 * This file is generated by jOOQ.
 */
package stroom.security.identity.db.jooq.tables.records;


import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.security.identity.db.jooq.tables.Account;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AccountRecord extends UpdatableRecordImpl<AccountRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>stroom.account.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.account.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>stroom.account.version</code>.
     */
    public void setVersion(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.account.version</code>.
     */
    public Integer getVersion() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>stroom.account.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.account.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>stroom.account.create_user</code>.
     */
    public void setCreateUser(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>stroom.account.create_user</code>.
     */
    public String getCreateUser() {
        return (String) get(3);
    }

    /**
     * Setter for <code>stroom.account.update_time_ms</code>.
     */
    public void setUpdateTimeMs(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>stroom.account.update_time_ms</code>.
     */
    public Long getUpdateTimeMs() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>stroom.account.update_user</code>.
     */
    public void setUpdateUser(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>stroom.account.update_user</code>.
     */
    public String getUpdateUser() {
        return (String) get(5);
    }

    /**
     * Setter for <code>stroom.account.user_id</code>.
     */
    public void setUserId(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>stroom.account.user_id</code>.
     */
    public String getUserId() {
        return (String) get(6);
    }

    /**
     * Setter for <code>stroom.account.email</code>.
     */
    public void setEmail(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>stroom.account.email</code>.
     */
    public String getEmail() {
        return (String) get(7);
    }

    /**
     * Setter for <code>stroom.account.password_hash</code>.
     */
    public void setPasswordHash(String value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.account.password_hash</code>.
     */
    public String getPasswordHash() {
        return (String) get(8);
    }

    /**
     * Setter for <code>stroom.account.password_last_changed_ms</code>.
     */
    public void setPasswordLastChangedMs(Long value) {
        set(9, value);
    }

    /**
     * Getter for <code>stroom.account.password_last_changed_ms</code>.
     */
    public Long getPasswordLastChangedMs() {
        return (Long) get(9);
    }

    /**
     * Setter for <code>stroom.account.first_name</code>.
     */
    public void setFirstName(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>stroom.account.first_name</code>.
     */
    public String getFirstName() {
        return (String) get(10);
    }

    /**
     * Setter for <code>stroom.account.last_name</code>.
     */
    public void setLastName(String value) {
        set(11, value);
    }

    /**
     * Getter for <code>stroom.account.last_name</code>.
     */
    public String getLastName() {
        return (String) get(11);
    }

    /**
     * Setter for <code>stroom.account.comments</code>.
     */
    public void setComments(String value) {
        set(12, value);
    }

    /**
     * Getter for <code>stroom.account.comments</code>.
     */
    public String getComments() {
        return (String) get(12);
    }

    /**
     * Setter for <code>stroom.account.login_count</code>.
     */
    public void setLoginCount(Integer value) {
        set(13, value);
    }

    /**
     * Getter for <code>stroom.account.login_count</code>.
     */
    public Integer getLoginCount() {
        return (Integer) get(13);
    }

    /**
     * Setter for <code>stroom.account.login_failures</code>.
     */
    public void setLoginFailures(Integer value) {
        set(14, value);
    }

    /**
     * Getter for <code>stroom.account.login_failures</code>.
     */
    public Integer getLoginFailures() {
        return (Integer) get(14);
    }

    /**
     * Setter for <code>stroom.account.last_login_ms</code>.
     */
    public void setLastLoginMs(Long value) {
        set(15, value);
    }

    /**
     * Getter for <code>stroom.account.last_login_ms</code>.
     */
    public Long getLastLoginMs() {
        return (Long) get(15);
    }

    /**
     * Setter for <code>stroom.account.reactivated_ms</code>.
     */
    public void setReactivatedMs(Long value) {
        set(16, value);
    }

    /**
     * Getter for <code>stroom.account.reactivated_ms</code>.
     */
    public Long getReactivatedMs() {
        return (Long) get(16);
    }

    /**
     * Setter for <code>stroom.account.force_password_change</code>.
     */
    public void setForcePasswordChange(Boolean value) {
        set(17, value);
    }

    /**
     * Getter for <code>stroom.account.force_password_change</code>.
     */
    public Boolean getForcePasswordChange() {
        return (Boolean) get(17);
    }

    /**
     * Setter for <code>stroom.account.never_expires</code>.
     */
    public void setNeverExpires(Boolean value) {
        set(18, value);
    }

    /**
     * Getter for <code>stroom.account.never_expires</code>.
     */
    public Boolean getNeverExpires() {
        return (Boolean) get(18);
    }

    /**
     * Setter for <code>stroom.account.enabled</code>.
     */
    public void setEnabled(Boolean value) {
        set(19, value);
    }

    /**
     * Getter for <code>stroom.account.enabled</code>.
     */
    public Boolean getEnabled() {
        return (Boolean) get(19);
    }

    /**
     * Setter for <code>stroom.account.inactive</code>.
     */
    public void setInactive(Boolean value) {
        set(20, value);
    }

    /**
     * Getter for <code>stroom.account.inactive</code>.
     */
    public Boolean getInactive() {
        return (Boolean) get(20);
    }

    /**
     * Setter for <code>stroom.account.locked</code>.
     */
    public void setLocked(Boolean value) {
        set(21, value);
    }

    /**
     * Getter for <code>stroom.account.locked</code>.
     */
    public Boolean getLocked() {
        return (Boolean) get(21);
    }

    /**
     * Setter for <code>stroom.account.processing_account</code>.
     */
    public void setProcessingAccount(Boolean value) {
        set(22, value);
    }

    /**
     * Getter for <code>stroom.account.processing_account</code>.
     */
    public Boolean getProcessingAccount() {
        return (Boolean) get(22);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AccountRecord
     */
    public AccountRecord() {
        super(Account.ACCOUNT);
    }

    /**
     * Create a detached, initialised AccountRecord
     */
    public AccountRecord(Integer id, Integer version, Long createTimeMs, String createUser, Long updateTimeMs, String updateUser, String userId, String email, String passwordHash, Long passwordLastChangedMs, String firstName, String lastName, String comments, Integer loginCount, Integer loginFailures, Long lastLoginMs, Long reactivatedMs, Boolean forcePasswordChange, Boolean neverExpires, Boolean enabled, Boolean inactive, Boolean locked, Boolean processingAccount) {
        super(Account.ACCOUNT);

        setId(id);
        setVersion(version);
        setCreateTimeMs(createTimeMs);
        setCreateUser(createUser);
        setUpdateTimeMs(updateTimeMs);
        setUpdateUser(updateUser);
        setUserId(userId);
        setEmail(email);
        setPasswordHash(passwordHash);
        setPasswordLastChangedMs(passwordLastChangedMs);
        setFirstName(firstName);
        setLastName(lastName);
        setComments(comments);
        setLoginCount(loginCount);
        setLoginFailures(loginFailures);
        setLastLoginMs(lastLoginMs);
        setReactivatedMs(reactivatedMs);
        setForcePasswordChange(forcePasswordChange);
        setNeverExpires(neverExpires);
        setEnabled(enabled);
        setInactive(inactive);
        setLocked(locked);
        setProcessingAccount(processingAccount);
    }
}
