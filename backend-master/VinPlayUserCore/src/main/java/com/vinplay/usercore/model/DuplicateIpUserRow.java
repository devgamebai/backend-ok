package com.vinplay.usercore.model;

/**
 * One row in the per-user detail list returned by
 * {@link com.vinplay.usercore.dao.UserInfoDao#findUsersForFirstIps}.
 *
 * Each instance represents a unique (firstIp, userId) pair found in
 * user_login_info for the queried time window.
 */
public class DuplicateIpUserRow {
    /** The first IP in the XFF chain (the real client IP before CDN edges). */
    public String firstIp;
    /** The user's numeric ID (user_login_info.user_id). */
    public long   userId;
    /** Login username (user_login_info.user_name). */
    public String userName;
    /** Display nickname (user_login_info.nick_name). */
    public String nickName;
    /** Number of login records for this (firstIp, userId) pair in the window. */
    public int    loginCount;
    /** Earliest time_log for this pair (string, "yyyy-MM-dd HH:mm:ss"). */
    public String firstLogin;
    /** Latest time_log for this pair (string, "yyyy-MM-dd HH:mm:ss"). */
    public String lastLogin;
}
