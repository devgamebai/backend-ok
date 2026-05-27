/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.payment.entities.AdminBank;
import com.vinplay.usercore.dao.AdminBankDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminBankDaoImpl
implements AdminBankDao {
    @Override
    public List<AdminBank> search(String bankName, String customerName, String bankNumber, String branch, int page, int limit) throws SQLException {
        ArrayList<AdminBank> list = new ArrayList<AdminBank>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            StringBuilder sql = new StringBuilder("SELECT * FROM admin_bank WHERE 1=1 ");
            int index = 1;
            int offset = (page - 1) * limit;
            if (bankName != null && !"".equals(bankName)) {
                sql.append(" AND bank_name LIKE ? ");
            }
            if (customerName != null && !"".equals(customerName)) {
                sql.append(" AND customer_name LIKE ? ");
            }
            if (bankNumber != null && !"".equals(bankNumber)) {
                sql.append(" AND bank_number LIKE ? ");
            }
            if (branch != null && !"".equals(branch)) {
                sql.append(" AND branch LIKE ? ");
            }
            sql.append(" ORDER BY id DESC LIMIT ?, ?");
            PreparedStatement stm = conn.prepareStatement(sql.toString());
            if (bankName != null && !"".equals(bankName)) {
                stm.setString(index++, "%" + bankName + "%");
            }
            if (customerName != null && !"".equals(customerName)) {
                stm.setString(index++, "%" + customerName + "%");
            }
            if (bankNumber != null && !"".equals(bankNumber)) {
                stm.setString(index++, "%" + bankNumber + "%");
            }
            if (branch != null && !"".equals(branch)) {
                stm.setString(index++, "%" + branch + "%");
            }
            stm.setInt(index++, offset);
            stm.setInt(index, limit);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                list.add(new AdminBank(rs));
            }
            rs.close();
            stm.close();
        }
        return list;
    }

    @Override
    public int count(String bankName, String customerName, String bankNumber, String branch) throws SQLException {
        int count = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS c FROM admin_bank WHERE 1=1 ");
            int index = 1;
            if (bankName != null && !"".equals(bankName)) {
                sql.append(" AND bank_name LIKE ? ");
            }
            if (customerName != null && !"".equals(customerName)) {
                sql.append(" AND customer_name LIKE ? ");
            }
            if (bankNumber != null && !"".equals(bankNumber)) {
                sql.append(" AND bank_number LIKE ? ");
            }
            if (branch != null && !"".equals(branch)) {
                sql.append(" AND branch LIKE ? ");
            }
            PreparedStatement stm = conn.prepareStatement(sql.toString());
            if (bankName != null && !"".equals(bankName)) {
                stm.setString(index++, "%" + bankName + "%");
            }
            if (customerName != null && !"".equals(customerName)) {
                stm.setString(index++, "%" + customerName + "%");
            }
            if (bankNumber != null && !"".equals(bankNumber)) {
                stm.setString(index++, "%" + bankNumber + "%");
            }
            if (branch != null && !"".equals(branch)) {
                stm.setString(index++, "%" + branch + "%");
            }
            if ((rs = stm.executeQuery()).next()) {
                count = rs.getInt("c");
            }
            rs.close();
            stm.close();
        }
        return count;
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean insert(AdminBank bank) throws SQLException {
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
    public boolean update(AdminBank bank) throws SQLException {
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
    public boolean delete(long id) throws SQLException {
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

