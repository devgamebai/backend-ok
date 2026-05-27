/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.StringUtils
 *  org.apache.commons.lang.time.DateUtils
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.usercore.dao.AttendanceConfigDao;
import com.vinplay.usercore.entities.AttendanceConfig;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

public class AttendanceConfigDaoImpl
implements AttendanceConfigDao {
    @Override
    public String insert(String startDate, long money) throws SQLException {
        try {
            if (StringUtils.isBlank((String)startDate)) {
                return "Ng\u00e0y b\u1eaft \u0111\u1ea7u chu k\u1ef3 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
            }
            if (0L == money) {
                return "S\u1ed1 ti\u1ec1n kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Date end = DateUtils.addDays((Date)simpleDateFormat.parse(startDate + " 00:00:00"), (int)7);
            AttendanceConfig attendanceConfig = new AttendanceConfig();
            attendanceConfig.setStart_date(startDate);
            attendanceConfig.setEnd_date(simpleDateFormat.format(end));
            attendanceConfig.setMoney(money);
            attendanceConfig.setCreate_at(simpleDateFormat.format(new Date()));
            return this.insert(attendanceConfig);
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    @Override
    public String insert(AttendanceConfig attendanceConfig) throws SQLException {
        String result = "failed";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        if (StringUtils.isBlank((String)attendanceConfig.getStart_date())) {
            return "Ng\u00e0y b\u1eaft \u0111\u1ea7u chu k\u1ef3 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
        }
        if (StringUtils.isBlank((String)attendanceConfig.getEnd_date())) {
            return "Ng\u00e0y k\u1ebft th\u00fac chu k\u1ef3 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
        }
        if (0L == attendanceConfig.getMoney()) {
            return "S\u1ed1 ti\u1ec1n kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
        }
        if (StringUtils.isBlank((String)attendanceConfig.getCreate_at())) {
            attendanceConfig.setCreate_at(simpleDateFormat.format(new Date()));
        }
        String sql = "INSERT INTO vinplay.attendance_config (start_date,end_date,money,create_at) VALUE (?,?,?,?)";
        PreparedStatement stmt = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            stmt = conn.prepareStatement(sql);
            int param = 1;
            stmt.setString(param++, attendanceConfig.getStart_date());
            stmt.setString(param++, attendanceConfig.getEnd_date());
            stmt.setLong(param++, attendanceConfig.getMoney());
            stmt.setString(param, attendanceConfig.getCreate_at());
            int ex = stmt.executeUpdate();
            stmt.close();
            result = ex > 0 ? "success" : "failed";
        }
        return result;
    }

    @Override
    public AttendanceConfig getLastest() throws SQLException {
        AttendanceConfig attendanceConfig = new AttendanceConfig();
        String sql = "select * from attendance_config order by start_date desc, end_date desc limit 1";
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                attendanceConfig.setId(rs.getInt("id"));
                attendanceConfig.setStart_date(rs.getString("start_date"));
                attendanceConfig.setEnd_date(rs.getString("end_date"));
                attendanceConfig.setMoney(rs.getLong("money"));
                attendanceConfig.setCreate_at(rs.getString("create_at"));
            }
            rs.close();
            stmt.close();
            AttendanceConfig attendanceConfig2 = attendanceConfig.getId() < 1 ? null : attendanceConfig;
            return attendanceConfig2;
        }
        catch (SQLException e) {
            e.printStackTrace();
            attendanceConfig = null;
            throw e;
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean isCheckSameIP() {
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
}

