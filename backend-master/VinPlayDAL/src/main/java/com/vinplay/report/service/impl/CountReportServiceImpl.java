/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.BaseResponse
 *  org.apache.log4j.Logger
 */
package com.vinplay.report.service.impl;

import com.vinplay.dal.entities.report.LogCountUserPlay;
import com.vinplay.report.service.CountReportService;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class CountReportServiceImpl
implements CountReportService {
    private static final Logger logger = Logger.getLogger((String)"backend");

    @Override
    public BaseResponse<List<LogCountUserPlay>> getLogReportModelSQL(String nickName, String fromTime, String endtime, int page, int totalrecord) {
        ArrayList<LogCountUserPlay> sltLogCountUserPlay = new ArrayList<LogCountUserPlay>();
        String sql = "SELECT * FROM vinplay.log_count_user_play WHERE 1=1";
        String sqlCount = "SELECT count(id)  as cnt FROM vinplay.log_count_user_play WHERE 1=1 ";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        long count = 0L;
        int num_start = (page - 1) * totalrecord;
        int index = 1;
        String limit = " LIMIT " + num_start + ", " + totalrecord + "";
        if (nickName != null && !"".equals(nickName)) {
            sql = sql + " and nick_name = ?";
            sqlCount = sqlCount + " and nick_name = ?";
        }
        if (fromTime != null && !"".equals(fromTime)) {
            sql = sql + " and time_report >= ?";
            sqlCount = sqlCount + " and time_report >= ?";
        }
        if (endtime != null && !"".equals(endtime)) {
            sql = sql + " and time_report <= ?";
            sqlCount = sqlCount + " and time_report <= ?";
        }
        sql = sql + " order by time_report DESC" + limit;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);
             PreparedStatement stmcount = conn.prepareStatement(sqlCount);){
            if (nickName != null && !"".equals(nickName)) {
                stm.setString(index, nickName);
                stmcount.setString(index, nickName);
                ++index;
            }
            if (fromTime != null && !"".equals(fromTime)) {
                stm.setString(index, fromTime);
                stmcount.setString(index, fromTime);
                ++index;
            }
            if (endtime != null && !"".equals(endtime)) {
                stm.setString(index, endtime);
                stmcount.setString(index, endtime);
                ++index;
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                LogCountUserPlay logCountUserPlay = new LogCountUserPlay();
                logCountUserPlay.id = rs.getInt("id");
                logCountUserPlay.nick_name = rs.getString("nick_name");
                logCountUserPlay.time = df.format(rs.getDate("time_report"));
                logCountUserPlay.wm = rs.getInt("wm");
                logCountUserPlay.ibc = rs.getInt("ibc");
                logCountUserPlay.ag = rs.getInt("ag");
                logCountUserPlay.tlmn = rs.getInt("tlmn");
                logCountUserPlay.bacay = rs.getInt("bacay");
                logCountUserPlay.xocdia = rs.getInt("xocdia");
                logCountUserPlay.minipoker = rs.getInt("minipoker");
                logCountUserPlay.slot_pokemon = rs.getInt("slot_pokemon");
                logCountUserPlay.baucua = rs.getInt("baucua");
                logCountUserPlay.taixiu = rs.getInt("taixiu");
                logCountUserPlay.caothap = rs.getInt("caothap");
                logCountUserPlay.slot_bitcoin = rs.getInt("slot_bitcoin");
                logCountUserPlay.slot_taydu = rs.getInt("slot_taydu");
                logCountUserPlay.slot_angrybird = rs.getInt("slot_angrybird");
                logCountUserPlay.slot_thantai = rs.getInt("slot_thantai");
                logCountUserPlay.slot_thethao = rs.getInt("slot_thethao");
                logCountUserPlay.deposit = rs.getInt("deposit");
                logCountUserPlay.withdraw = rs.getInt("withdraw");
                logCountUserPlay.cmd = rs.getInt("cmd");
                logCountUserPlay.slot_chiemtinh = rs.getInt("slot_chiemtinh");
                logCountUserPlay.taixiu_st = rs.getInt("taixiu_st");
                logCountUserPlay.fish = rs.getInt("fish");
                logCountUserPlay.slot_bikini = rs.getInt("slot_bikini");
                logCountUserPlay.slot_galaxy = rs.getInt("slot_galaxy");
                sltLogCountUserPlay.add(logCountUserPlay);
            }
            ResultSet rsCount = stmcount.executeQuery();
            if (rsCount.next()) {
                count = rs.getInt("cnt");
            }
        }
        catch (Exception e) {
            logger.error((e + ""));
        }
        return new BaseResponse(true, "0", "success", sltLogCountUserPlay, count);
    }
}

