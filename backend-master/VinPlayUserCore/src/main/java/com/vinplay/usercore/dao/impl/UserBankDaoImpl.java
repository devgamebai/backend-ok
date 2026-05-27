/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.payment.entities.UserBank;
import com.vinplay.usercore.dao.UserBankDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class UserBankDaoImpl
implements UserBankDao {
    private static final Logger logger = Logger.getLogger(UserBankDaoImpl.class);

    @Override
    public long add(UserBank userBank) throws SQLException {
        long id = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_insert(?,?,?,?,?,?,?)")) {
            int param = 1;
            call.setLong(param++, (long)userBank.getUserId().intValue());
            call.setString(param++, userBank.getNickName());
            call.setString(param++, userBank.getBankName());
            call.setString(param++, userBank.getCustomerName());
            call.setString(param++, userBank.getBankNumber());
            call.setInt(param++, (int)userBank.getStatus());
            call.setString(param++, userBank.getBranch());
            try (ResultSet rs = call.executeQuery()) {
                if (rs.next()) {
                    id = rs.getLong("id");
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return id;
    }

    @Override
    public boolean update(UserBank userBank) throws SQLException {
        boolean result = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_update(?,?,?,?,?,?,?,?,?)")) {
            int param = 1;
            call.setLong(param++, (long)userBank.getId());
            call.setLong(param++, (long)userBank.getUserId().intValue());
            call.setString(param++, userBank.getNickName());
            call.setString(param++, userBank.getBankName());
            call.setString(param++, userBank.getCustomerName());
            call.setString(param++, userBank.getBankNumber());
            call.setInt(param++, (int)userBank.getStatus());
            call.setString(param++, userBank.getBranch());
            call.setString(param++, userBank.getLastEditor());
            call.executeUpdate();
            result = true;
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return result;
    }

    @Override
    public boolean delete(long id) throws SQLException {
        boolean result = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_deletebyid(?)")) {
            int param = 1;
            call.setLong(param++, id);
            call.executeUpdate();
            result = true;
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return result;
    }

    private UserBank parseToUserModel(ResultSet rs) {
        UserBank userBank = new UserBank();
        try {
            userBank.setId(rs.getLong("id"));
            userBank.setUserId(rs.getInt("user_id"));
            userBank.setNickName(rs.getString("nick_name"));
            userBank.setBankName(rs.getString("bank_name"));
            userBank.setCustomerName(rs.getString("customer_name"));
            userBank.setBankNumber(rs.getString("bank_number"));
            userBank.setStatus(rs.getInt("status"));
            userBank.setCreateDate(rs.getTimestamp("create_date"));
            userBank.setBranch(rs.getString("branch"));
            userBank.setUpdateDate(rs.getTimestamp("update_date"));
            userBank.setLastEditor(rs.getString("last_editor"));
            return userBank;
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public UserBank getById(Long id) throws SQLException {
        UserBank user = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_getbyid(?)")) {
            call.setLong(1, (long)id);
            try (ResultSet rs = call.executeQuery()) {
                if (rs.next()) {
                    user = this.parseToUserModel(rs);
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return user;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public UserBank getByBankNumber(String nickname, String bankNumber) throws SQLException {
        UserBank user = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_getbybanknumber(?,?)")) {
            call.setString(1, nickname);
            call.setString(2, bankNumber);
            try (ResultSet rs = call.executeQuery()) {
                if (rs.next()) {
                    user = this.parseToUserModel(rs);
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            return null;
        }
        return user;
    }

    @Override
    public int getCountByNickName(String nickName) throws SQLException {
        int count = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_count(?)")) {
            call.setString(1, nickName);
            try (ResultSet rs = call.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("count");
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return count;
    }

    @Override
    public List<UserBank> getByCustomerName(String nickName, String customerName) throws SQLException {
        ArrayList<UserBank> userBanks = new ArrayList<UserBank>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_getbycustomename(?,?)")) {
            call.setString(1, nickName);
            call.setString(2, customerName);
            try (ResultSet rs = call.executeQuery()) {
                while (rs.next()) {
                    UserBank user = this.parseToUserModel(rs);
                    if (user == null) continue;
                    userBanks.add(user);
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return userBanks;
    }

    @Override
    public List<UserBank> search(String nickName, String bankName, String bankNumber, int isAdmin, int pageNumber, int limit) throws SQLException {
        String customerName = "";
        return this.search(nickName, customerName, bankName, bankNumber, isAdmin, pageNumber, limit);
    }

    @Override
    public List<UserBank> search(String nickName, String customerName, String bankName, String bankNumber, int isAdmin, int pageNumber, int limit) throws SQLException {
        ArrayList<UserBank> userBanks = new ArrayList<UserBank>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_search_1(?,?,?,?,?,?,?)")) {
            call.setInt(1, pageNumber);
            call.setInt(2, limit);
            call.setString(3, nickName);
            call.setString(4, customerName);
            call.setString(5, bankName);
            call.setString(6, bankNumber);
            call.setInt(7, isAdmin);
            try (ResultSet rs = call.executeQuery()) {
                while (rs.next()) {
                    UserBank user = this.parseToUserModel(rs);
                    if (user == null) continue;
                    userBanks.add(user);
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return userBanks;
    }

    @Override
    public int search_count(String nickName, String bankName, String bankNumber, int isAdmin) throws SQLException {
        String customerName = "";
        return this.search_count(nickName, customerName, bankName, bankNumber, isAdmin);
    }

    @Override
    public int search_count(String nickName, String customerName, String bankName, String bankNumber, int isAdmin) throws SQLException {
        int count = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             CallableStatement call = conn.prepareCall("CALL usersbank_search_count_1(?,?,?,?,?)")) {
            call.setString(1, nickName);
            call.setString(2, customerName);
            call.setString(3, bankName);
            call.setString(4, bankNumber);
            call.setInt(5, isAdmin);
            try (ResultSet rs = call.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("count");
                }
            }
        }
        catch (SQLException e) {
            logger.error(e);
            throw e;
        }
        return count;
    }

    @Override
    public boolean isAddedBank(String nickName) {
        long count = 0L;
        String sql = "SELECT count(*)  as cnt from users_bank WHERE nick_name=? ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setString(param++, nickName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                count = rs.getInt("cnt");
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
        return count > 0L;
    }

    @Override
    public boolean isExistBankNumber(String bankNumber) throws SQLException {
        long count = 0L;
        String sql = "SELECT count(*) as cnt FROM vinplay.users_bank where 1 =1 and bank_number=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            PreparedStatement stm = conn.prepareStatement(sql);
            if (bankNumber != null) {
                stm.setString(1, bankNumber);
            }
            if ((rs = stm.executeQuery()).next()) {
                count = rs.getInt("cnt");
            }
            boolean bl = count > 0L;
            return bl;
        }
    }
}

