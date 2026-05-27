/*
 * SUN-13xx Phase 7 — wallet unification.
 * users.recharge_money was dropped. Source of truth is now the money ledger
 * exposed via the v_derived_deposit_total view.
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.TopRechargeMoneyDAO;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.TopRechargeMoneyResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TopRechargeMoneyDAOImpl
implements TopRechargeMoneyDAO {
    @Override
    public List<TopRechargeMoneyResponse> getTopRechargeMoney(int top, String nickName, int page, int bot) throws SQLException {
        ArrayList<TopRechargeMoneyResponse> results = new ArrayList<TopRechargeMoneyResponse>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            int numStart = (page - 1) * 50;
            String conditions = "";
            String limit = " LIMIT " + numStart + "," + 50;
            if (bot == 0 || bot == 1) {
                conditions += " AND u.is_bot=" + bot;
            }
            if (nickName != null && !nickName.isEmpty()) {
                // legacy behaviour: exact match on nick_name, parameterised below
                conditions += " AND u.nick_name=?";
            }
            String sql = "SELECT u.nick_name, COALESCE(d.deposit_total, 0) AS recharge_money "
                    + "FROM users u "
                    + "LEFT JOIN v_derived_deposit_total d ON d.user_id = u.id "
                    + "WHERE 1=1 " + conditions
                    + " ORDER BY COALESCE(d.deposit_total, 0) DESC"
                    + limit;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (nickName != null && !nickName.isEmpty()) {
                    stmt.setString(1, nickName);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        TopRechargeMoneyResponse entry = new TopRechargeMoneyResponse();
                        entry.userName = rs.getString("nick_name");
                        entry.money = rs.getLong("recharge_money");
                        results.add(entry);
                    }
                }
            }
        }
        return results;
    }
}
