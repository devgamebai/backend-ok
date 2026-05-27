/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.LogReportUserDao;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class LogReportUserDaoImpl
implements LogReportUserDao {
    private static final Logger logger = Logger.getLogger(LogReportUserDaoImpl.class);

    /*
     * Exception decompiling
     */
    @Override
    public boolean updateDepositLogReportByDateAndUser(String nickname, String currentDate, Long money) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public List<LogReportModel> getAllDepositLogReportByDateAndUser(String currentDate) {
        ArrayList<LogReportModel> result = new ArrayList<LogReportModel>();
        String sql = "SELECT * FROM vinplay.log_report_user WHERE time_report=? and deposit > 0;";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setDate(param++, Date.valueOf(currentDate));
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                LogReportModel logReportModel = new LogReportModel();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                logReportModel.id = rs.getLong("id");
                logReportModel.time = df.format(rs.getDate("time_report"));
                logReportModel.nick_name = rs.getString("nick_name");
                logReportModel.wm = rs.getLong("wm");
                logReportModel.wm_win = rs.getLong("wm_win");
                logReportModel.ibc = rs.getLong("ibc");
                logReportModel.ibc_win = rs.getLong("ibc_win");
                logReportModel.ag = rs.getLong("ag");
                logReportModel.ag_win = rs.getLong("ag_win");
                logReportModel.cmd = rs.getLong("cmd");
                logReportModel.cmd_win = rs.getLong("cmd_win");
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
                logReportModel.fish = rs.getLong("fish");
                logReportModel.fish_win = rs.getLong("fish_win");
                logReportModel.deposit = rs.getLong("deposit");
                logReportModel.withdraw = rs.getLong("withdraw");
                logReportModel.totalRefund = rs.getLong("t_refund");
                logReportModel.totalBonus = rs.getLong("t_bonus");
                logReportModel.code = rs.getString("code");
                logReportModel.slot_bikini = rs.getLong("slot_bikini");
                logReportModel.slot_bikini_win = rs.getLong("slot_bikini_win");
                logReportModel.slot_galaxy = rs.getLong("slot_galaxy");
                logReportModel.slot_galaxy_win = rs.getLong("slot_galaxy_win");
                result.add(logReportModel);
            }
            rs.close();
            stm.close();
            if (conn != null) {
                conn.close();
            }
        }
        catch (Exception e) {
            logger.error(("getLogReportModelSQL: " + e.getMessage()));
            return null;
        }
        return result;
    }
}

