/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.payment.dao.Impl;

import com.payment.dao.HistoryApplyForDao;
import com.payment.entities.HistoryApplyForEntity;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryApplyForDaoImpl
implements HistoryApplyForDao {
    /*
     * Exception decompiling
     */
    @Override
    public boolean create(HistoryApplyForEntity entity) throws SQLException {
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

    /*
     * Exception decompiling
     */
    @Override
    public boolean updateStatus(String requestId, int status, long cash) throws SQLException {
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
    public HistoryApplyForEntity getByRequestId(String requestId) throws SQLException {
        String sql = "SELECT * FROM history_applyfor WHERE key_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);){
            stm.setString(1, requestId);
            try (ResultSet rs = stm.executeQuery();){
                if (rs.next()) {
                    HistoryApplyForEntity entity = new HistoryApplyForEntity();
                    entity.setFid(rs.getString("fid"));
                    entity.setKeyId(rs.getString("key_id"));
                    entity.setNickName(rs.getString("nick_name"));
                    entity.setCash(rs.getInt("cash"));
                    entity.setCashReal(rs.getInt("cash_real"));
                    entity.setType(rs.getString("type"));
                    entity.setText(rs.getString("text"));
                    entity.setStatus(rs.getInt("status"));
                    entity.setDay(rs.getInt("day"));
                    entity.setMonth(rs.getInt("month"));
                    entity.setYear(rs.getInt("year"));
                    entity.setTime(rs.getLong("time"));
                    entity.setCashBack(rs.getInt("cash_back"));
                    HistoryApplyForEntity historyApplyForEntity = entity;
                    return historyApplyForEntity;
                }
            }
        }
        return null;
    }

    @Override
    public List<HistoryApplyForEntity> getAllByNickname(String nickname) throws SQLException {
        String sql = "SELECT * FROM history_applyfor WHERE nick_name = ? ORDER BY time DESC";
        ArrayList<HistoryApplyForEntity> list = new ArrayList<HistoryApplyForEntity>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stm = conn.prepareStatement(sql);){
            stm.setString(1, nickname);
            try (ResultSet rs = stm.executeQuery();){
                while (rs.next()) {
                    HistoryApplyForEntity entity = new HistoryApplyForEntity();
                    entity.setFid(rs.getString("fid"));
                    entity.setKeyId(rs.getString("key_id"));
                    entity.setNickName(rs.getString("nick_name"));
                    entity.setCash(rs.getLong("cash"));
                    entity.setCashReal(rs.getLong("cash_real"));
                    entity.setType(rs.getString("type"));
                    entity.setText(rs.getString("text"));
                    entity.setStatus(rs.getInt("status"));
                    entity.setDay(rs.getInt("day"));
                    entity.setMonth(rs.getInt("month"));
                    entity.setYear(rs.getInt("year"));
                    entity.setTime(rs.getLong("time"));
                    entity.setCashBack(rs.getLong("cash_back"));
                    list.add(entity);
                }
            }
        }
        return list;
    }
}

