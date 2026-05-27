/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.GetUserInfoResponse
 *  com.vinplay.vbee.common.response.UserInfoResponse
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.model.DuplicateIpGroupRow;
import com.vinplay.usercore.model.DuplicateIpUserRow;
import com.vinplay.vbee.common.response.GetUserInfoResponse;
import com.vinplay.vbee.common.response.UserInfoResponse;
import java.sql.SQLException;
import java.util.List;

public interface UserInfoDao {
    public List<UserInfoResponse> searchUserInfo(String var1, String var2, String var3, String var4, String var5, int var6);

    public int countsearchUserInfo(String var1, String var2, String var3, String var4, String var5);

    public GetUserInfoResponse listGetNickName(String var1) throws SQLException;

    /**
     * Return a paged list of IPs that appeared on more than one distinct user
     * account in the given time window.
     *
     * <p>Pipeline overview:
     * <ol>
     *   <li>$match on time_log [tsFrom, tsTo] and ip exists + non-empty.</li>
     *   <li>Optional pre-group filters: {@code type} and {@code nnSubstring} are
     *       applied before grouping (they narrow the input document set).</li>
     *   <li>$addFields — split ip on "," to obtain first_ip.</li>
     *   <li>Optional post-addFields filter: {@code firstIp} exact-matches
     *       first_ip (correctness gate; optimization handled inside impl).</li>
     *   <li>$group by first_ip; collect distinct user_ids, min/max time_log.</li>
     *   <li>$match userCount >= minUsers.</li>
     *   <li>$sort by userCount desc, lastSeen desc.</li>
     *   <li>$skip + $limit (PAGE_SIZE_DUP = 50) for the requested page.</li>
     *   <li>A separate $count pipeline populates totalRecord on every row.</li>
     * </ol>
     *
     * @param tsFrom   lower bound for time_log (inclusive), format "yyyy-MM-dd HH:mm:ss"
     * @param tsTo     upper bound for time_log (inclusive), format "yyyy-MM-dd HH:mm:ss"
     * @param type     login type filter (null = all)
     * @param nnSubstring null = no nickname filter; else a literal substring matched
     *                    case-insensitively against {@code nick_name} (the DAO escapes regex
     *                    metacharacters internally — callers pass raw user input).
     * @param firstIp  exact match on first_ip after addFields (null = all)
     * @param minUsers minimum number of distinct users for a group to be returned (typically 2)
     * @param page     1-based page number
     * @return list of matching IP groups, at most PAGE_SIZE_DUP entries
     */
    public List<DuplicateIpGroupRow> findDuplicateIpGroups(
            String tsFrom, String tsTo, Integer type,
            String nnSubstring, String firstIp,
            int minUsers, int page);

    /**
     * Return all (firstIp, userId) pairs for the given set of first IPs in the
     * given time window — used to populate the per-IP user detail table.
     *
     * <p>Short-circuits to {@link java.util.Collections#emptyList()} when
     * {@code firstIps} is null or empty to avoid a full-collection scan.
     *
     * @param tsFrom      lower bound for time_log (inclusive), format "yyyy-MM-dd HH:mm:ss"
     * @param tsTo        upper bound for time_log (inclusive), format "yyyy-MM-dd HH:mm:ss"
     * @param firstIps    list of first-hop IPs to look up; must be the result of the
     *                    group query so the caller controls cardinality
     * @param type        optional login type filter, mirrors {@link #findDuplicateIpGroups}
     *                    so user counts agree across the two queries; null = no filter
     * @param nnSubstring optional case-insensitive nickname substring filter, mirrors
     *                    {@link #findDuplicateIpGroups}; null/empty = no filter
     * @return flat list of user rows, sorted by firstIp asc then lastLogin desc
     */
    public List<DuplicateIpUserRow> findUsersForFirstIps(
            String tsFrom, String tsTo, List<String> firstIps,
            Integer type, String nnSubstring);
}

