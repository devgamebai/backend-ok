package com.vinplay.usercore.model;

/**
 * One row in the duplicate-IP group summary returned by
 * {@link com.vinplay.usercore.dao.UserInfoDao#findDuplicateIpGroups}.
 *
 * Each instance represents a single first-hop IP that appears in
 * user_login_info for more than one distinct user_id in the queried
 * time window.
 */
public class DuplicateIpGroupRow {
    /** The first IP in the XFF chain stored on user_login_info.ip. */
    public String firstIp;
    /** Number of distinct user_id values seen from this IP. */
    public int    userCount;
    /** Earliest time_log value seen for this IP group (string, "yyyy-MM-dd HH:mm:ss"). */
    public String firstSeen;
    /** Latest time_log value seen for this IP group (string, "yyyy-MM-dd HH:mm:ss"). */
    public String lastSeen;
    /** Total number of groups matching all filters (for pagination). Populated on every returned row. */
    public long   totalRecord;
}
