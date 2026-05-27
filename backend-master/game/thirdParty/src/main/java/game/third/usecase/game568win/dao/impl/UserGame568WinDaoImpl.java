/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package game.third.usecase.game568win.dao.impl;

import com.vinplay.vbee.common.pools.ConnectionPool;
import game.third.usecase.game568win.dao.UserGame568WinDao;
import game.third.usecase.game568win.entities.UserGame568Win;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserGame568WinDaoImpl
implements UserGame568WinDao {
    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public boolean checkUserExist(String username) throws Exception {
        String sql = "SELECT COUNT(*) FROM users_game568win WHERE username = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery();){
                if (!rs.next()) return false;
                boolean bl = rs.getInt(1) > 0;
                return bl;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public UserGame568Win get(String username) throws Exception {
        String sql = "SELECT * FROM users_game568win WHERE username = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery();){
                if (!rs.next()) return null;
                UserGame568Win user = new UserGame568Win();
                user.setServerId(rs.getString("serverId"));
                user.setUsername(rs.getString("username"));
                user.setAgent(rs.getString("agent"));
                user.setUserGroup(rs.getString("userGroup"));
                user.setDisplayName(rs.getString("displayName"));
                UserGame568Win userGame568Win = user;
                return userGame568Win;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean create(UserGame568Win user) throws Exception {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 3 blocks at once
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

