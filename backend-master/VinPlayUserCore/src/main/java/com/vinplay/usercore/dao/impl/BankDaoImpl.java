/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.payment.entities.Bank;
import com.vinplay.usercore.dao.BankDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BankDaoImpl
implements BankDao {
    @Override
    public List<Bank> findAll() {
        ArrayList<Bank> lstBank = new ArrayList<Bank>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM banks WHERE status = 1 order by bank_name";
            PreparedStatement stm = conn.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Bank bank = new Bank(rs);
                bank.setAddby("");
                lstBank.add(bank);
            }
            rs.close();
            stm.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
        return lstBank;
    }

    @Override
    public Bank get(long id) {
        Bank bank = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM banks WHERE id = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setLong(1, id);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                bank = new Bank(rs);
                bank.setAddby("");
            }
            rs.close();
            stm.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return bank;
    }

    @Override
    public Bank getByBankCode(String code) {
        Bank bank = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM banks WHERE code = ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, code);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                bank = new Bank(rs);
                bank.setAddby("");
            }
            rs.close();
            stm.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return bank;
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean addBank(Bank bank) {
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
    public boolean editBank(Bank bank) {
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
    public boolean deleteBank(long id) {
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
    public int count(String bankName, String bankCode) throws SQLException {
        int count = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT count(*) as count FROM banks WHERE 1=1 ";
            int index = 1;
            if (bankName != null && !"".equals(bankName)) {
                sql = sql + " and bank_name like ?";
            }
            if (bankCode != null && !"".equals(bankCode)) {
                sql = sql + " and code like ?";
            }
            PreparedStatement stm = conn.prepareStatement(sql);
            if (bankName != null && !"".equals(bankName)) {
                stm.setString(index, '%' + bankName + '%');
                ++index;
            }
            if (bankCode != null && !"".equals(bankCode)) {
                stm.setString(index, '%' + bankCode + '%');
                ++index;
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                count = rs.getInt("count");
            }
            rs.close();
            stm.close();
        }
        return count;
    }

    @Override
    public List<Bank> search(String bankName, String bankCode, int page, int totalrecord) throws SQLException {
        ArrayList<Bank> lstBank = new ArrayList<Bank>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM banks WHERE 1=1 ";
            int num_start = (page - 1) * totalrecord;
            int index = 1;
            String limit = " LIMIT " + num_start + ", " + totalrecord + "";
            if (bankName != null && !"".equals(bankName)) {
                sql = sql + " and bank_name like ?";
            }
            if (bankCode != null && !"".equals(bankCode)) {
                sql = sql + " and code like ?";
            }
            sql = sql + " order by id DESC" + limit;
            PreparedStatement stm = conn.prepareStatement(sql);
            if (bankName != null && !"".equals(bankName)) {
                stm.setString(index, '%' + bankName + '%');
                ++index;
            }
            if (bankCode != null && !"".equals(bankCode)) {
                stm.setString(index, '%' + bankCode + '%');
                ++index;
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Bank bank = new Bank(rs);
                lstBank.add(bank);
            }
            rs.close();
            stm.close();
        }
        return lstBank;
    }
}

