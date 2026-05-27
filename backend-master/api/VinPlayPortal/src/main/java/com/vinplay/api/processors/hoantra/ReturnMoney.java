/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.vinplay.usercore.service.MailBoxService
 *  com.vinplay.usercore.service.impl.MailBoxServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.HoanTraModel
 *  com.vinplay.vbee.common.models.LogReportModel
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.api.processors.hoantra;

import com.google.gson.Gson;
import com.vinplay.api.processors.hoantra.HoanTraDescription;
import com.vinplay.usercore.service.MailBoxService;
import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.HoanTraModel;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnMoney {
    private Gson gson = new Gson();
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    // SUN-934: removed instance-field conn (zombie pattern + double-close).
    private Connection getConnection() {
        return ConnectionPool.getInstance().getConnection("mysqlpoolname");
    }

    public List<HoanTraModel> getMoneyHoanTra(Date date) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        try (Connection conn = this.getConnection();){
            String sql = "SELECT 0 AS vip_point, 0 AS vip_point_save, 0 AS money_vp, l.*  FROM vinplay.log_report_user l JOIN vinplay.users u ON l.nick_name = u.nick_name  WHERE time_report = ? and dai_ly = 0 and u.is_bot = 0";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setDate(param++, date);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                LogReportModel logReportModel = new LogReportModel();
                logReportModel.id = rs.getInt("id");
                logReportModel.nick_name = rs.getString("nick_name");
                logReportModel.time = this.df.format(rs.getDate("time_report"));
                logReportModel.wm = rs.getLong("wm");
                logReportModel.wm_win = rs.getLong("wm_win");
                logReportModel.ibc = rs.getLong("ibc");
                logReportModel.ibc_win = rs.getLong("ibc_win");
                logReportModel.cmd = rs.getLong("cmd");
                logReportModel.cmd_win = rs.getLong("cmd_win");
                logReportModel.ag = rs.getLong("ag");
                logReportModel.ag_win = rs.getLong("ag_win");
                logReportModel.tlmn = rs.getLong("tlmn");
                logReportModel.tlmn_win = rs.getLong("tlmn_win");
                logReportModel.bacay = rs.getLong("bacay");
                logReportModel.bacay_win = rs.getLong("bacay_win");
                logReportModel.xocdia = rs.getLong("xocdia");
                logReportModel.xocdia_win = rs.getLong("xocdia_win");
                logReportModel.minipoker = rs.getLong("minipoker");
                logReportModel.minipoker_win = rs.getLong("minipoker_win");
                logReportModel.slot_pokemon = rs.getLong("slot_pokemon");
                logReportModel.slot_pokemon_win = rs.getLong("slot_pokemon_win");
                logReportModel.baucua = rs.getLong("baucua");
                logReportModel.baucua_win = rs.getLong("baucua_win");
                logReportModel.taixiu = rs.getLong("taixiu");
                logReportModel.taixiu_win = rs.getLong("taixiu_win");
                logReportModel.caothap = rs.getLong("caothap");
                logReportModel.caothap_win = rs.getLong("caothap_win");
                logReportModel.slot_bitcoin = rs.getLong("slot_bitcoin");
                logReportModel.slot_bitcoin_win = rs.getLong("slot_bitcoin_win");
                logReportModel.slot_taydu = rs.getLong("slot_taydu");
                logReportModel.slot_taydu_win = rs.getLong("slot_taydu_win");
                logReportModel.slot_angrybird = rs.getLong("slot_angrybird");
                logReportModel.slot_angrybird_win = rs.getLong("slot_angrybird_win");
                logReportModel.slot_thantai = rs.getLong("slot_thantai");
                logReportModel.slot_thantai_win = rs.getLong("slot_thantai_win");
                logReportModel.slot_thethao = rs.getLong("slot_thethao");
                logReportModel.slot_thethao_win = rs.getLong("slot_thethao_win");
                logReportModel.slot_chiemtinh = rs.getLong("slot_chiemtinh");
                logReportModel.slot_chiemtinh_win = rs.getLong("slot_chiemtinh_win");
                logReportModel.taixiu_st = rs.getLong("taixiu_st");
                logReportModel.taixiu_st_win = rs.getLong("taixiu_st_win");
                logReportModel.deposit = rs.getLong("deposit");
                logReportModel.withdraw = rs.getLong("withdraw");
                logReportModel.slot_bikini = rs.getLong("slot_bikini");
                logReportModel.slot_bikini_win = rs.getLong("slot_bikini_win");
                logReportModel.slot_galaxy = rs.getLong("slot_galaxy");
                logReportModel.slot_galaxy_win = rs.getLong("slot_galaxy_win");
                logReportModel.ebet = rs.getLong("ebet");
                logReportModel.ebet_win = rs.getLong("ebet_win");
                int vippoint = 0;
                HoanTraModel hoanTraModel = new HoanTraModel(logReportModel, vippoint);
                if (!hoanTraModel.isHoanTra()) continue;
                listHoanTraModel.add(hoanTraModel);
            }
            ArrayList<HoanTraModel> arrayList = listHoanTraModel;
            return arrayList;
        }
    }

    public HoanTraModel getMoneyHoanTra(Date date, String nickname) throws SQLException {
        HoanTraModel hoanTraModel = new HoanTraModel();
        try (Connection conn = this.getConnection();){
            String sql = "SELECT 0 AS vip_point, 0 AS vip_point_save, 0 AS money_vp, l.*  FROM vinplay.log_report_user l JOIN vinplay.users u ON l.nick_name = u.nick_name  WHERE time_report = ? and nick_name=? and dai_ly = 0 and u.is_bot = 0";
            try {
                PreparedStatement stm = conn.prepareStatement(sql);
                int param = 1;
                stm.setDate(param++, date);
                stm.setString(param++, nickname);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    LogReportModel logReportModel = new LogReportModel();
                    logReportModel.id = rs.getInt("id");
                    logReportModel.nick_name = rs.getString("nick_name");
                    logReportModel.time = this.df.format(rs.getDate("time_report"));
                    logReportModel.wm = rs.getLong("wm");
                    logReportModel.wm_win = rs.getLong("wm_win");
                    logReportModel.ibc = rs.getLong("ibc");
                    logReportModel.ibc_win = rs.getLong("ibc_win");
                    logReportModel.cmd = rs.getLong("cmd");
                    logReportModel.cmd_win = rs.getLong("cmd_win");
                    logReportModel.ag = rs.getLong("ag");
                    logReportModel.ag_win = rs.getLong("ag_win");
                    logReportModel.tlmn = rs.getLong("tlmn");
                    logReportModel.tlmn_win = rs.getLong("tlmn_win");
                    logReportModel.bacay = rs.getLong("bacay");
                    logReportModel.bacay_win = rs.getLong("bacay_win");
                    logReportModel.xocdia = rs.getLong("xocdia");
                    logReportModel.xocdia_win = rs.getLong("xocdia_win");
                    logReportModel.minipoker = rs.getLong("minipoker");
                    logReportModel.minipoker_win = rs.getLong("minipoker_win");
                    logReportModel.slot_pokemon = rs.getLong("slot_pokemon");
                    logReportModel.slot_pokemon_win = rs.getLong("slot_pokemon_win");
                    logReportModel.baucua = rs.getLong("baucua");
                    logReportModel.baucua_win = rs.getLong("baucua_win");
                    logReportModel.taixiu = rs.getLong("taixiu");
                    logReportModel.taixiu_win = rs.getLong("taixiu_win");
                    logReportModel.caothap = rs.getLong("caothap");
                    logReportModel.caothap_win = rs.getLong("caothap_win");
                    logReportModel.slot_bitcoin = rs.getLong("slot_bitcoin");
                    logReportModel.slot_bitcoin_win = rs.getLong("slot_bitcoin_win");
                    logReportModel.slot_taydu = rs.getLong("slot_taydu");
                    logReportModel.slot_taydu_win = rs.getLong("slot_taydu_win");
                    logReportModel.slot_angrybird = rs.getLong("slot_angrybird");
                    logReportModel.slot_angrybird_win = rs.getLong("slot_angrybird_win");
                    logReportModel.slot_thantai = rs.getLong("slot_thantai");
                    logReportModel.slot_thantai_win = rs.getLong("slot_thantai_win");
                    logReportModel.slot_thethao = rs.getLong("slot_thethao");
                    logReportModel.slot_thethao_win = rs.getLong("slot_thethao_win");
                    logReportModel.slot_chiemtinh = rs.getLong("slot_chiemtinh");
                    logReportModel.slot_chiemtinh_win = rs.getLong("slot_chiemtinh_win");
                    logReportModel.taixiu_st = rs.getLong("taixiu_st");
                    logReportModel.taixiu_st_win = rs.getLong("taixiu_st_win");
                    logReportModel.deposit = rs.getLong("deposit");
                    logReportModel.withdraw = rs.getLong("withdraw");
                    logReportModel.slot_bikini = rs.getLong("slot_bikini");
                    logReportModel.slot_bikini_win = rs.getLong("slot_bikini_win");
                    logReportModel.slot_galaxy = rs.getLong("slot_galaxy");
                    logReportModel.slot_galaxy_win = rs.getLong("slot_galaxy_win");
                    int vippoint = 0;
                    hoanTraModel = new HoanTraModel(logReportModel, vippoint);
                }
                rs.close();
            }
            catch (Exception e) {
                throw e;
            }
            finally {
                conn.close();
            }
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            // SUN-934: removed this.conn.close() — try-with-resources handles it
        }
        return hoanTraModel;
    }

    public int deleteHoanTra(Date date, Boolean send_success) throws SQLException {
        try (Connection conn = this.getConnection();){
            int result;
            String sql = "DELETE FROM vinplay.log_hoan_tra WHERE time = ? " + (send_success == null ? "" : " and send_success = ?");
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setDate(param++, date);
            if (send_success != null) {
                stm.setBoolean(param++, send_success);
            }
            int n = result = stm.executeUpdate();
            return n;
        }
    }

    public int deleteHoanTra(Date date, Boolean send_success, String nickname) throws SQLException {
        int result = 0;
        try (Connection conn = this.getConnection();){
            String sql = "DELETE FROM vinplay.log_hoan_tra WHERE time = ? and nick_name = ?" + (send_success == null ? "" : " and send_success = ?");
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setDate(param++, date);
            stm.setString(param++, nickname);
            if (send_success != null) {
                stm.setBoolean(param++, send_success);
            }
            result = stm.executeUpdate();
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            // SUN-934: removed this.conn.close() — try-with-resources handles it
        }
        return result;
    }

    public int deleteHoanTra(Date date) throws SQLException {
        return this.deleteHoanTra(date, null);
    }

    public int updateHoanTra(HoanTraModel hoanTraModel, Boolean isSuccess, String message) throws SQLException {
        try (Connection conn = this.getConnection();){
            String sql = "UPDATE vinplay.log_hoan_tra SET send_success = ?, message = ? where `time`=? and nick_name = ?";
            PreparedStatement stm2 = conn.prepareStatement(sql);
            int param = 1;
            stm2.setBoolean(param++, isSuccess);
            stm2.setString(param++, message);
            stm2.setString(param++, hoanTraModel.time);
            stm2.setString(param, hoanTraModel.nick_name);
            int n = stm2.executeUpdate();
            return n;
        }
    }

    public int insertHoanTraList(List<HoanTraModel> listHoanTraModel) throws SQLException {
        int countInsert = 0;
        try (Connection conn = this.getConnection();){
            String sql = "INSERT INTO vinplay.log_hoan_tra (nick_name,time,vip_point,total_money_sport,hoan_tra_sport,total_money_casino, hoan_tra_casino, total_money_game, hoan_tra_game, vip_index) VALUE (?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement(sql);
            for (int i = 0; i < listHoanTraModel.size(); ++i) {
                HoanTraModel hoanTraModel = listHoanTraModel.get(i);
                int param = 1;
                stm.setString(param++, hoanTraModel.nick_name);
                stm.setDate(param++, Date.valueOf(hoanTraModel.time));
                stm.setInt(param++, hoanTraModel.vip_point);
                stm.setLong(param++, hoanTraModel.total_money_sport);
                stm.setLong(param++, hoanTraModel.hoan_tra_sport);
                stm.setLong(param++, hoanTraModel.total_money_casino);
                stm.setLong(param++, hoanTraModel.hoan_tra_casino);
                stm.setLong(param++, hoanTraModel.total_money_game);
                stm.setLong(param++, hoanTraModel.hoan_tra_game);
                stm.setInt(param++, hoanTraModel.vip_index);
                stm.addBatch();
                if (i % 50 != 0) continue;
                int[] result = stm.executeBatch();
                countInsert += result.length;
            }
            int[] result = stm.executeBatch();
            int n = countInsert += result.length;
            return n;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean insertHoanTraList(HoanTraModel hoanTraModel) throws SQLException {
        boolean result = false;
        try (Connection conn = this.getConnection();){
            String sql = "INSERT INTO vinplay.log_hoan_tra (nick_name,time,vip_point,total_money_sport,hoan_tra_sport,total_money_casino, hoan_tra_casino, total_money_game, hoan_tra_game, vip_index) VALUE (?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setString(param++, hoanTraModel.nick_name);
            stm.setDate(param++, Date.valueOf(hoanTraModel.time));
            stm.setInt(param++, hoanTraModel.vip_point);
            stm.setLong(param++, hoanTraModel.total_money_sport);
            stm.setLong(param++, hoanTraModel.hoan_tra_sport);
            stm.setLong(param++, hoanTraModel.total_money_casino);
            stm.setLong(param++, hoanTraModel.hoan_tra_casino);
            stm.setLong(param++, hoanTraModel.total_money_game);
            stm.setLong(param++, hoanTraModel.hoan_tra_game);
            stm.setInt(param++, hoanTraModel.vip_index);
            stm.executeUpdate();
            result = true;
        }
        finally {
            try {
                // SUN-934: removed this.conn.close() — try-with-resources handles it
            }
            catch (Exception exception) {}
        }
        return result;
    }

    public int insertHoanTraHistory(HoanTraModel hoanTraModel, Boolean isSuccess, String responseAddHoantra) throws SQLException {
        try (Connection conn = this.getConnection();){
            int result;
            String sql = "INSERT INTO vinplay.log_hoan_tra_histories (nick_name,time,vip_point,total_money_sport,hoan_tra_sport,total_money_casino, hoan_tra_casino, total_money_game, hoan_tra_game, vip_index, send_success, message) VALUE (?,?,?,?,?,?,?,?,?,?,?, ?)";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setString(param++, hoanTraModel.nick_name);
            stm.setDate(param++, Date.valueOf(hoanTraModel.time));
            stm.setInt(param++, hoanTraModel.vip_point);
            stm.setLong(param++, hoanTraModel.total_money_sport);
            stm.setLong(param++, hoanTraModel.hoan_tra_sport);
            stm.setLong(param++, hoanTraModel.total_money_casino);
            stm.setLong(param++, hoanTraModel.hoan_tra_casino);
            stm.setLong(param++, hoanTraModel.total_money_game);
            stm.setLong(param++, hoanTraModel.hoan_tra_game);
            stm.setInt(param++, hoanTraModel.vip_index);
            stm.setBoolean(param++, isSuccess);
            stm.setString(param, responseAddHoantra);
            int n = result = stm.executeUpdate();
            return n;
        }
    }

    public List<HoanTraModel> getListHoanTra(java.util.Date date, String nick_name) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        String sql = "SELECT * FROM vinplay.log_hoan_tra where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
        try (Connection conn = this.getConnection();){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (date != null) {
                stm.setDate(param++, new Date(date.getTime()));
            }
            if (nick_name != null && !nick_name.isEmpty()) {
                stm.setString(param, nick_name);
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                HoanTraModel hoanTraModel = new HoanTraModel();
                hoanTraModel.id = rs.getInt("id");
                hoanTraModel.nick_name = rs.getString("nick_name");
                hoanTraModel.time = this.df.format(rs.getDate("time"));
                hoanTraModel.vip_point = 0;
                hoanTraModel.total_money_sport = rs.getLong("total_money_sport");
                hoanTraModel.hoan_tra_sport = rs.getLong("hoan_tra_sport");
                hoanTraModel.total_money_casino = rs.getLong("total_money_casino");
                hoanTraModel.hoan_tra_casino = rs.getLong("hoan_tra_casino");
                hoanTraModel.total_money_game = rs.getLong("total_money_game");
                hoanTraModel.hoan_tra_game = rs.getLong("hoan_tra_game");
                hoanTraModel.vip_index = rs.getInt("vip_index");
                hoanTraModel.send_success = rs.getBoolean("send_success");
                hoanTraModel.created_at = rs.getTimestamp("created_at");
                hoanTraModel.updated_at = rs.getTimestamp("updated_at");
                hoanTraModel.message = rs.getString("message");
                if (!hoanTraModel.isHoanTra()) continue;
                listHoanTraModel.add(hoanTraModel);
            }
            ArrayList<HoanTraModel> arrayList = listHoanTraModel;
            return arrayList;
        }
    }

    public List<HoanTraModel> getListHoanTra(java.util.Date date, String nick_name, int page, int maxItem) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        page = page - 1 < 0 ? 0 : page - 1;
        String sql = "SELECT * FROM vinplay.log_hoan_tra where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?") + " limit ?,?";
        try (Connection conn = this.getConnection();){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (date != null) {
                stm.setDate(param++, new Date(date.getTime()));
            }
            if (nick_name != null && !nick_name.isEmpty()) {
                stm.setString(param++, nick_name);
            }
            stm.setInt(param++, page * maxItem);
            stm.setInt(param, maxItem);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                HoanTraModel hoanTraModel = new HoanTraModel();
                hoanTraModel.id = rs.getInt("id");
                hoanTraModel.nick_name = rs.getString("nick_name");
                hoanTraModel.time = this.df.format(rs.getDate("time"));
                hoanTraModel.vip_point = 0;
                hoanTraModel.total_money_sport = rs.getLong("total_money_sport");
                hoanTraModel.hoan_tra_sport = rs.getLong("hoan_tra_sport");
                hoanTraModel.total_money_casino = rs.getLong("total_money_casino");
                hoanTraModel.hoan_tra_casino = rs.getLong("hoan_tra_casino");
                hoanTraModel.total_money_game = rs.getLong("total_money_game");
                hoanTraModel.hoan_tra_game = rs.getLong("hoan_tra_game");
                hoanTraModel.vip_index = rs.getInt("vip_index");
                hoanTraModel.send_success = rs.getBoolean("send_success");
                hoanTraModel.created_at = rs.getTimestamp("created_at");
                hoanTraModel.updated_at = rs.getTimestamp("updated_at");
                hoanTraModel.message = rs.getString("message");
                listHoanTraModel.add(hoanTraModel);
            }
            ArrayList<HoanTraModel> arrayList = listHoanTraModel;
            return arrayList;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Loose catch block
     */
    public long countListHoanTra(java.util.Date date, String nick_name) throws SQLException {
        long l;
        Throwable throwable;
        Connection conn;
        block26: {
            block27: {
                long count = 0L;
                conn = this.getConnection();
                throwable = null;
                String sql = "SELECT count(*) as cnt FROM vinplay.log_hoan_tra where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
                PreparedStatement stm = conn.prepareStatement(sql);
                int param = 1;
                if (date != null) {
                    stm.setDate(param++, new Date(date.getTime()));
                }
                if (nick_name != null && !nick_name.isEmpty()) {
                    stm.setString(param, nick_name);
                }
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    count = rs.getLong("cnt");
                }
                l = count;
                if (conn == null) break block26;
                if (throwable == null) break block27;
                try {
                    conn.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                break block26;
            }
            conn.close();
        }
        try {
            // SUN-934: removed this.conn.close() — try-with-resources handles it
        }
        catch (Exception exception) {
            // empty catch block
        }
        return l;
    }

    public List<HoanTraModel> getListHoanTraHistories(Date date, String nick_name, int page, int limit) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        try (Connection conn = this.getConnection();){
            String sql = "SELECT * FROM vinplay.log_hoan_tra_histories where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?") + " order by id desc limit ?,?";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (date != null) {
                stm.setDate(param++, date);
            }
            if (nick_name != null && !nick_name.isEmpty()) {
                stm.setString(param++, nick_name);
            }
            stm.setInt(param++, (page - 1) * limit);
            stm.setInt(param, limit);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                HoanTraModel hoanTraModel = new HoanTraModel();
                hoanTraModel.id = rs.getInt("id");
                hoanTraModel.nick_name = rs.getString("nick_name");
                hoanTraModel.time = this.df.format(rs.getDate("time"));
                hoanTraModel.vip_point = 0;
                hoanTraModel.total_money_sport = rs.getLong("total_money_sport");
                hoanTraModel.hoan_tra_sport = rs.getLong("hoan_tra_sport");
                hoanTraModel.total_money_casino = rs.getLong("total_money_casino");
                hoanTraModel.hoan_tra_casino = rs.getLong("hoan_tra_casino");
                hoanTraModel.total_money_game = rs.getLong("total_money_game");
                hoanTraModel.hoan_tra_game = rs.getLong("hoan_tra_game");
                hoanTraModel.vip_index = rs.getInt("vip_index");
                hoanTraModel.send_success = rs.getBoolean("send_success");
                hoanTraModel.created_at = rs.getTimestamp("created_at");
                hoanTraModel.updated_at = rs.getTimestamp("updated_at");
                hoanTraModel.message = rs.getString("message");
                listHoanTraModel.add(hoanTraModel);
            }
            ArrayList<HoanTraModel> arrayList = listHoanTraModel;
            return arrayList;
        }
    }

    public Map<String, Object> searchListHoanTraHistories(Date date, String nick_name, int page, int limit) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try (Connection conn = this.getConnection();){
            String sql = "SELECT * FROM vinplay.log_hoan_tra_histories where 1 =1 " + (date == null ? "" : " and time >= ? and time <= ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?") + " order by id desc limit ?,?";
            String sqlTotalRecords = "SELECT count(*) as totalrecords FROM vinplay.log_hoan_tra_histories where 1 =1 " + (date == null ? "" : " and time >= ? and time <= ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
            String sqlSum = "SELECT ifnull(sum(ifnull(total_money_sport,0)),0) sporttotalsum, ifnull(sum(ifnull(total_money_casino,0)),0) casinototalsum, ifnull(sum(ifnull(total_money_game,0)),0) egametotalsum, ifnull(sum(ifnull(hoan_tra_sport,0)),0) sportfundsum, ifnull(sum(ifnull(hoan_tra_casino,0)),0) casinofundsum, ifnull(sum(ifnull(hoan_tra_game,0)),0) egamefundsum FROM vinplay.log_hoan_tra_histories where 1 =1 " + (date == null ? "" : " and time >= ? and time <= ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
            PreparedStatement stm = conn.prepareStatement(sql);
            PreparedStatement stmTotalRecords = conn.prepareStatement(sqlTotalRecords);
            PreparedStatement stmSum = conn.prepareStatement(sqlSum);
            int param = 1;
            if (date != null) {
                stm.setString(param, date.toString() + " 00:00:00");
                stmTotalRecords.setString(param, date.toString() + " 00:00:00");
                stmSum.setString(param, date.toString() + " 00:00:00");
                stm.setString(++param, date.toString() + " 23:59:59");
                stmTotalRecords.setString(param, date.toString() + " 23:59:59");
                stmSum.setString(param, date.toString() + " 23:59:59");
                ++param;
            }
            if (nick_name != null && !nick_name.isEmpty()) {
                stm.setString(param, nick_name);
                stmTotalRecords.setString(param, nick_name);
                stmSum.setString(param, nick_name);
            }
            int n = ++param;
            stm.setInt(n, (page - 1) * limit);
            int n2 = ++param;
            ++param;
            stm.setInt(n2, limit);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                HoanTraModel hoanTraModel = new HoanTraModel();
                hoanTraModel.id = rs.getInt("id");
                hoanTraModel.nick_name = rs.getString("nick_name");
                hoanTraModel.time = this.df.format(rs.getDate("time"));
                hoanTraModel.vip_point = 0;
                hoanTraModel.total_money_sport = rs.getLong("total_money_sport");
                hoanTraModel.hoan_tra_sport = rs.getLong("hoan_tra_sport");
                hoanTraModel.total_money_casino = rs.getLong("total_money_casino");
                hoanTraModel.hoan_tra_casino = rs.getLong("hoan_tra_casino");
                hoanTraModel.total_money_game = rs.getLong("total_money_game");
                hoanTraModel.hoan_tra_game = rs.getLong("hoan_tra_game");
                hoanTraModel.vip_index = rs.getInt("vip_index");
                hoanTraModel.send_success = rs.getBoolean("send_success");
                hoanTraModel.created_at = rs.getTimestamp("created_at");
                hoanTraModel.updated_at = rs.getTimestamp("updated_at");
                hoanTraModel.message = rs.getString("message");
                listHoanTraModel.add(hoanTraModel);
            }
            result.put("records", listHoanTraModel);
            ResultSet rsSum = stmSum.executeQuery();
            while (rsSum.next()) {
                result.put("sportTotalSum", rsSum.getLong("sporttotalsum"));
                result.put("casinoTotalSum", rsSum.getLong("casinototalsum"));
                result.put("egameTotalSum", rsSum.getLong("egametotalsum"));
                result.put("sportFundSum", rsSum.getLong("sportfundsum"));
                result.put("casinoFundSum", rsSum.getLong("casinofundsum"));
                result.put("egameFundSum", rsSum.getLong("egamefundsum"));
            }
            ResultSet rsTotalRecords = stmTotalRecords.executeQuery();
            while (rsTotalRecords.next()) {
                result.put("totalrecords", rsTotalRecords.getLong("totalrecords"));
            }
        }
        catch (Exception e) {
            result = new HashMap();
            result.put("records", new ArrayList());
            result.put("sportTotalSum", 0);
            result.put("casinoTotalSum", 0);
            result.put("egameTotalSum", 0);
            result.put("sportFundSum", 0);
            result.put("casinoFundSum", 0);
            result.put("egameFundSum", 0);
            result.put("totalrecords", 0);
        }
        return result;
    }

    public long countListHoanTraHistories(Date date, String nick_name) throws SQLException {
        long count = 0L;
        try (Connection conn = this.getConnection();){
            String sql = "SELECT count(*) as cnt FROM vinplay.log_hoan_tra_histories where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (date != null) {
                stm.setDate(param++, date);
            }
            if (nick_name != null && !nick_name.isEmpty()) {
                stm.setString(param, nick_name);
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                count = rs.getLong("cnt");
            }
            long l = count;
            return l;
        }
    }

    public int[] addMoneyHoanTraToUser(List<HoanTraModel> listHoanTraModel) throws SQLException {
        int countSuccess = 0;
        int countFail = 0;
        HashMap<String, Long> lstNickName = new HashMap<String, Long>();
        for (HoanTraModel hoanTraModel : listHoanTraModel) {
            long moneyReturn = hoanTraModel.hoan_tra_casino + hoanTraModel.hoan_tra_sport + hoanTraModel.hoan_tra_game;
            if (moneyReturn <= 0L) continue;
            BaseResponseModel responseModel = this.addMoneyHoanTra(hoanTraModel);
            if (responseModel.isSuccess()) {
                this.updateHoanTra(hoanTraModel, true, "");
                this.insertHoanTraHistory(hoanTraModel, true, "");
                ++countSuccess;
                lstNickName.put(hoanTraModel.nick_name, moneyReturn);
                continue;
            }
            this.updateHoanTra(hoanTraModel, false, responseModel.toJson());
            this.insertHoanTraHistory(hoanTraModel, false, responseModel.toJson());
            ++countFail;
        }
        if (lstNickName != null && !lstNickName.isEmpty()) {
            MailBoxServiceImpl mailService = new MailBoxServiceImpl();
            lstNickName.forEach((arg_0, arg_1) -> ReturnMoney.lambda$addMoneyHoanTraToUser$0((MailBoxService)mailService, arg_0, arg_1));
        }
        return new int[]{countSuccess, countFail};
    }

    public BaseResponseModel addMoneyHoanTra(HoanTraModel hoanTraModel) {
        UserServiceImpl userService = new UserServiceImpl();
        BaseResponseModel baseResponseModel = userService.updateMoneyFromAdmin(hoanTraModel.nick_name, hoanTraModel.hoan_tra_casino + hoanTraModel.hoan_tra_sport + hoanTraModel.hoan_tra_game, "vin", Games.HOAN_TRA.getName(), Games.HOAN_TRA.getId() + "", this.gson.toJson(new HoanTraDescription(hoanTraModel.time)));
        return baseResponseModel;
    }

    public int[] insertHoanTra(Date date) throws SQLException {
        int deleteRecords = this.deleteHoanTra(date);
        List<HoanTraModel> hoanTraModels = this.getMoneyHoanTra(date);
        int countInsert = this.insertHoanTraList(hoanTraModels);
        return new int[]{countInsert, deleteRecords};
    }

    public boolean insertHoanTra(Date date, String nickname) throws SQLException {
        HoanTraModel hoanTraModel = this.getMoneyHoanTra(date, nickname);
        return this.insertHoanTraList(hoanTraModel);
    }

    public int[] addMoneyHoanTra(Date date) throws SQLException {
        List<HoanTraModel> hoanTraModels = this.getListHoanTra(date, null);
        int[] countSendHoanTra = this.addMoneyHoanTraToUser(hoanTraModels);
        this.deleteHoanTra(date, true);
        return countSendHoanTra;
    }

    public int[] addMoneyHoanTra(Date date, String nickname) throws SQLException {
        List<HoanTraModel> hoanTraModels = this.getListHoanTra(date, nickname);
        int[] countSendHoanTra = this.addMoneyHoanTraToUser(hoanTraModels);
        this.deleteHoanTra(date, true, nickname);
        return countSendHoanTra;
    }

    private static /* synthetic */ void lambda$addMoneyHoanTraToUser$0(MailBoxService mailService, String k, Long v) {
        mailService.sendMailBoxFromByNickNameAdmin(k, "Ho\u00e0n Tr\u1ea3 h\u00e0ng ng\u00e0y " + VinPlayUtils.getYesterday(), "Ch\u00fac m\u1eebng qu\u00fd kh\u00e1ch \u0111\u00e3 nh\u1eadn v\u1ec1 " + v + " ti\u1ec1n ho\u00e0n c\u01b0\u1ee3c v\u00e0o t\u00e0i kho\u1ea3n ch\u00ednh theo ch\u01b0\u01a1ng tr\u00ecnh ho\u00e0n tr\u1ea3 m\u1ed7i ng\u00e0y. \r\nCh\u00fac qu\u00fd kh\u00e1ch ch\u01a1i to th\u1eafng l\u1edbn c\u00f9ng Roy88");
    }
}

