/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.HoanTraModel
 *  com.vinplay.vbee.common.models.LogReportModel
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.hoantra.service.impl;

import com.vinplay.hoantra.service.HoanTraService;
import com.vinplay.vbee.common.models.HoanTraModel;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class HoanTraServiceImpl
implements HoanTraService {
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    // SUN-934: removed instance-field `private Connection conn` (zombie pattern).
    // Previously cached a connection that was never reliably returned to the pool.
    // Now each method gets a fresh connection via try-with-resources (RAII).
    private Connection getConnection() {
        return ConnectionPool.getInstance().getConnection("mysqlpoolname");
    }

    @Override
    public int[] generateAllHoanTra(Date date) throws SQLException {
        int deleteRecords = this.deleteHoanTra(date, null);
        List<HoanTraModel> hoanTraModels = this.getMoneyHoanTra(date);
        int countInsert = this.insertHoanTraList(hoanTraModels);
        return new int[]{countInsert, deleteRecords};
    }

    @Override
    public int deleteHoanTra(Date date, Boolean send_success) throws SQLException {
        String sql = "DELETE FROM vinplay.log_hoan_tra WHERE time = ? " + (send_success == null ? "" : " and send_success = ?");
        try (Connection conn = this.getConnection();){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setDate(1, date);
            if (send_success != null) {
                stm.setBoolean(2, send_success);
            }
            int n = stm.executeUpdate();
            return n;
        }
    }

    @Override
    public List<HoanTraModel> getMoneyHoanTra(Date date) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        try (Connection conn = this.getConnection();){
            String sql = "SELECT 0 AS vip_point, 0 AS vip_point_save, 0 AS money_vp, l.*  FROM vinplay.log_report_user l JOIN vinplay.users u ON l.nick_name = u.nick_name  WHERE l.time_report = ? and u.dai_ly = 0";
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
                logReportModel.ibc = rs.getLong("ibc_win");
                logReportModel.ag = rs.getLong("ag");
                logReportModel.ag_win = rs.getLong("ag_win");
                logReportModel.tlmn = rs.getLong("tlmn");
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
                logReportModel.deposit = rs.getLong("deposit");
                logReportModel.withdraw = rs.getLong("withdraw");
                logReportModel.slot_bikini = rs.getLong("slot_bikini");
                logReportModel.slot_bikini_win = rs.getLong("slot_bikini_win");
                logReportModel.slot_galaxy = rs.getLong("slot_galaxy");
                logReportModel.slot_galaxy_win = rs.getLong("slot_galaxy_win");
                int vippoint = 0;
                HoanTraModel hoanTraModel = new HoanTraModel(logReportModel, vippoint);
                listHoanTraModel.add(hoanTraModel);
            }
            ArrayList<HoanTraModel> arrayList = listHoanTraModel;
            return arrayList;
        }
    }

    @Override
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

    @Override
    public int insertHoanTraList(List<HoanTraModel> listHoanTraModel) throws SQLException {
        int countInsert = 0;
        String sql = "INSERT INTO vinplay.log_hoan_tra (nick_name,time,vip_point,total_money_sport,hoan_tra_sport,total_money_casino, hoan_tra_casino, total_money_game, hoan_tra_game, vip_index) VALUE (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = this.getConnection();){
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

    @Override
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

    @Override
    public List<HoanTraModel> getListHoanTra(Date date, String nick_name) throws SQLException {
        ArrayList<HoanTraModel> listHoanTraModel = new ArrayList<HoanTraModel>();
        String sql = "SELECT * FROM vinplay.log_hoan_tra where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
        try (Connection conn = this.getConnection();){
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

    @Override
    public long countListHoanTra(Date date, String nick_name) throws SQLException {
        long count = 0L;
        try (Connection conn = this.getConnection();){
            String sql = "SELECT count(*) as cnt FROM vinplay.log_hoan_tra where 1 =1 " + (date == null ? "" : " and time = ?") + (nick_name == null || nick_name.isEmpty() ? "" : " and nick_name = ?");
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

    @Override
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

    @Override
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
}

