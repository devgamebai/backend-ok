/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.payment.entities.Bank;
import com.vinplay.usercore.dao.AgentBankDao;
import com.vinplay.usercore.entities.AgentBank;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class AgentBankDaoImpl
implements AgentBankDao {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<AgentBank> findAll() {
        ArrayList<AgentBank> agentBanks = new ArrayList<AgentBank>();
        String sql = "SELECT * FROM agentbanks ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement stm = conn.prepareStatement(sql);
             ResultSet rs = stm.executeQuery()) {
            while (rs.next()) {
                try {
                    AgentBank agentBank = new AgentBank(rs);
                    agentBanks.add(agentBank);
                }
                catch (Exception exception) {}
            }
            return agentBanks;
        }
        catch (Exception e) {
            return new ArrayList<AgentBank>();
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public AgentBank getById(long id) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 1[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
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
    public AgentBank getByBankNumber(String bankNumber) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 1[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
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
    public AgentBank getByBankCode(String agentCode, String bankCode) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 1[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
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
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean create(AgentBank agentBank) {
        Bank bank = GameCommon.LIST_BANK_NAME.stream().filter(x -> agentBank.getBank_code().equals(x.getCode())).findAny().orElse(null);
        if (bank == null) {
            return false;
        }
        String sql = " INSERT INTO agentbanks (agent_code,bank_acount,bank_code,bank_name,bank_number,bank_branch) VALUES (?,?,?,?,?,?) ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, agentBank.getAgent_code());
            stm.setString(2, agentBank.getBank_acount());
            stm.setString(3, agentBank.getBank_code());
            stm.setString(4, bank == null ? agentBank.getBank_code() : bank.getBank_name());
            stm.setString(5, agentBank.getBank_number());
            stm.setString(6, agentBank.getBank_branch());
            stm.executeUpdate();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean update(AgentBank agentBank) {
        Bank bank = GameCommon.LIST_BANK_NAME.stream().filter(x -> agentBank.getBank_code().equals(x.getCode())).findAny().orElse(null);
        String sql = " UPDATE agentbanks SET agent_code=?, bank_acount=?, bank_code=?, bank_name=?, bank_number=?, bank_branch=? WHERE id = ? ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, agentBank.getAgent_code());
            stm.setString(2, agentBank.getBank_acount());
            stm.setString(3, agentBank.getBank_code());
            stm.setString(4, bank == null ? agentBank.getBank_code() : bank.getBank_name());
            stm.setString(5, agentBank.getBank_number());
            stm.setString(6, agentBank.getBank_branch());
            stm.setLong(7, agentBank.getId());
            stm.executeUpdate();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean delete(long id) {
        String sql = " delete from  agentbanks  WHERE id = ? ";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
             PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setLong(1, id);
            stm.executeUpdate();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public Map<String, Object> search(String keyword, String agentCode, int pageIndex, int limit) throws SQLException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");){
            pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
            String paginateCondition = pageIndex == -1 || limit == -1 ? "" : " limit ?,?";
            String keywordCondition = "";
            if (!StringUtils.isBlank((CharSequence)keyword)) {
                keywordCondition = " and ((bank_acount like ?) or (bank_code like ?) or (bank_name like ?) or (bank_number like ?) or (bank_branch like ?) )";
            }
            String agentCodeCondition = "";
            if (!StringUtils.isBlank((CharSequence)agentCode)) {
                agentCodeCondition = " and (agent_code = ?)";
            }
            String sql = "select * from agentbanks where (1=1)" + keywordCondition + agentCodeCondition + " ORDER BY id" + paginateCondition;
            String sqlCount = "select count(id) total from agentbanks where (1=1)" + keywordCondition + agentCodeCondition;
            PreparedStatement stmt = conn.prepareStatement(sql);
            PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
            int index = 1;
            if (!StringUtils.isBlank((CharSequence)keyword)) {
                for (int i = index; i < 6; ++index, ++i) {
                    stmt.setString(index, "%" + keyword + "%");
                    stmtCount.setString(index, "%" + keyword + "%");
                }
            }
            if (!StringUtils.isBlank((CharSequence)agentCode)) {
                stmt.setString(index, agentCode);
                stmtCount.setString(index, agentCode);
                ++index;
            }
            if (-1 != pageIndex && -1 != limit) {
                stmt.setInt(index++, pageIndex * limit);
                stmt.setInt(index++, limit);
            }
            ArrayList<AgentBank> agentBanks = new ArrayList<AgentBank>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    AgentBank agentBank = new AgentBank(rs);
                    agentBanks.add(agentBank);
                }
                catch (Exception agentBank) {}
            }
            map.put("agentBanks", agentBanks);
            ResultSet rsCount = stmtCount.executeQuery();
            while (rsCount.next()) {
                map.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getInt("total"));
            }
            rs.close();
            rsCount.close();
            stmt.close();
            stmtCount.close();
            if (conn != null) {
                conn.close();
            }
            HashMap<String, Object> hashMap = map;
            return hashMap;
        }
        catch (SQLException e) {
            map.put("agentBanks", new ArrayList());
            map.put("total", 0);
            return map;
        }
    }
}

