/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package com.vinplay.giftcode;

import bitzero.util.common.business.Debug;
import com.vinplay.dal.entities.giftcode.GiftCodeBundleModel;
import com.vinplay.giftcode.GiftCodeModel;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserBankServiceImpl;
import com.vinplay.usercore.service.impl.UserBonusServiceImpl;
import com.vinplay.vbee.common.models.UserBonusModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GiftCodeUtil {
    public static void main(String[] args) {
        String string_date = "2021-06-03 23:59:59";
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d = f.parse(string_date);
            long milliseconds = d.getTime();
            System.out.println(milliseconds);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static int isUsedGiftCode(GiftCodeModel giftCodeModel, String userName, String ip, UserService userService) {
        if (giftCodeModel == null) {
            return 100;
        }
        try {
            Date date = VinPlayUtils.getDateTime(VinPlayUtils.getCurrentDateTime());
            if (date.getTime() > giftCodeModel.exprired.getTime() || date.getTime() < giftCodeModel.from.getTime()) {
                return 1;
            }
            if (giftCodeModel.time_used >= giftCodeModel.max_use) {
                return 2;
            }
            if (GiftCodeUtil.isUserUsedGiftCode(giftCodeModel.id, userName)) {
                return 4;
            }
            int giftcodeType = giftCodeModel.type;
            switch (giftCodeModel.type) {
                case 0: {
                    if (giftCodeModel.user_name.equalsIgnoreCase(userName)) break;
                    return 3;
                }
                case 2: {
                    if (!userService.isXacThucSDT(userName)) {
                        return 6;
                    }
                    if (!GiftCodeUtil.isUserUsedGiftCode(giftCodeModel.id, userName)) break;
                    return 4;
                }
                case 1: {
                    break;
                }
                case 3: {
                    if (!GiftCodeUtil.userUsedGiftCodeInEvent(giftCodeModel.event, userName)) break;
                    return 5;
                }
            }
            int eventId = giftCodeModel.event;
            BaseResponse<String> isValid = GiftCodeUtil.validation(userName, ip, giftCodeModel.money, giftCodeModel.giftcode, userService, eventId, giftcodeType);
            if (!isValid.isSuccess()) {
                return Integer.valueOf(isValid.getErrorCode());
            }
            GiftCodeUtil.insertUserUsedGiftCode(giftCodeModel.id, userName, giftCodeModel.event);
            GiftCodeUtil.updateNumberUsedGiftCode(giftCodeModel.id, giftCodeModel.time_used);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
            return 100;
        }
        return 0;
    }

    private static BaseResponse<String> validation(String nickName, String ip, int money, String giftCode, UserService userService, int eventId, int giftcodeType) {
        String clientIp = "";
        if (ip != null && !"".equals(ip)) {
            String[] arrayIp = ip.split(",");
            clientIp = arrayIp[0].trim();
        }
        UserBonusServiceImpl userBonusService = new UserBonusServiceImpl();
        double amount = money;
        UserBonusModel model = new UserBonusModel(nickName, eventId, amount, null, clientIp, "Khuy\u1ebfn m\u00e3i GIFTCODE EVENT" + eventId + " " + giftCode);
        if (giftcodeType == 3 && userBonusService.isReceivedBonus(nickName, eventId)) {
            return new BaseResponse<String>("99", "Qu\u00fd kh\u00e1ch \u0111\u00e3 \u0111\u01b0\u1ee3c nh\u1eadn giftcode \u0111\u1ee3t n\u00e0y r\u1ed3i");
        }
        UserBankServiceImpl userBankService = new UserBankServiceImpl();
        try {
            if (!userService.isXacThucSDT(nickName)) {
                return new BaseResponse<String>("19", "Qu\u00fd kh\u00e1ch vui l\u00f2ng x\u00e1c th\u1ef1c S\u0110T \u0111\u1ec3 nh\u1eadn giftcode");
            }
        }
        catch (Exception e2) {
            return new BaseResponse<String>("5", e2.getMessage());
        }
        try {
            userBonusService.insertBonus(model);
            return new BaseResponse<String>("0", "Ch\u00fac m\u1eebng qu\u00fd kh\u00e1ch \u0111\u00e3 nh\u1eadn \u0111\u01b0\u1ee3c giftcode ", null);
        }
        catch (Exception e) {
            return new BaseResponse<String>("5", e.getMessage());
        }
    }

    public static boolean userUsedGiftCodeInEvent(int event, String userName) throws SQLException {
        boolean value = false;
        String sql = "SELECT * FROM gift_code_useds WHERE event=? AND username=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            int param = 1;
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(param++, event);
            stm.setString(param++, userName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                value = true;
            }
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
        return value;
    }

    public static synchronized void updateNumberUsedGiftCode(int giftcodeID, int currentTimeUse) throws SQLException {
        int timeInsert = currentTimeUse + 1;
        String sql = "UPDATE vinplay.gift_codes SET time_used=? WHERE id=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setInt(param++, timeInsert);
            stm.setInt(param++, giftcodeID);
            stm.executeUpdate();
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
    }

    public static void insertUserUsedGiftCode(int giftcodeID, String userName, int event) throws SQLException {
        String sql = "INSERT INTO vinplay.gift_code_useds (giftcode_id,username,event)  VALUES (?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setInt(param++, giftcodeID);
            stm.setString(param++, userName);
            stm.setInt(param++, event);
            stm.executeUpdate();
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static boolean isUserUsedGiftCode(int giftCodeID, String userName) throws SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "SELECT * FROM gift_code_useds WHERE giftcode_id=? AND username=?";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setInt(param++, giftCodeID);
            stm.setString(param++, userName);
            ResultSet rs = stm.executeQuery();
            if (!rs.next()) return false;
            conn.close();
            boolean bl = true;
            return bl;
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
        return false;
    }

    public static GiftCodeModel getGiftCode(String giftCode) {
        GiftCodeModel giftCodeModel = null;
        String sql = "SELECT * FROM vinplay.gift_codes WHERE giftcode=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, giftCode);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                giftCodeModel = GiftCodeUtil.parseGiftCodeModel(rs);
            }
            rs.close();
            stm.close();
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
        return giftCodeModel;
    }

    public static boolean giftCodeIsExits(String giftCode) {
        GiftCodeModel giftCodeModel = GiftCodeUtil.getGiftCode(giftCode);
        return giftCodeModel != null;
    }

    public static Set<String> giftCodesExist(Collection<String> giftCodes) throws SQLException {
        HashSet<String> existing = new HashSet<String>();
        if (giftCodes == null || giftCodes.isEmpty()) {
            return existing;
        }
        ArrayList<String> list = new ArrayList<String>(giftCodes);
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
        }
        String sql = "SELECT giftcode FROM vinplay.gift_codes WHERE giftcode IN (" + placeholders + ")";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            for (int i = 0; i < list.size(); ++i) {
                stm.setString(i + 1, (String)list.get(i));
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                existing.add(rs.getString("giftcode"));
            }
        }
        return existing;
    }

    public static void insertGiftCodeBulk(List<GiftCodeModel> list) throws SQLException {
        if (list == null || list.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO gift_codes (giftcode,`type`,money,time_used,max_use,`from`,exprired,created_by,event,user_name) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            for (GiftCodeModel m : list) {
                int param = 1;
                stm.setString(param++, m.giftcode);
                stm.setInt(param++, m.type);
                stm.setLong(param++, m.money);
                stm.setInt(param++, m.time_used);
                stm.setInt(param++, m.max_use);
                stm.setTimestamp(param++, m.from);
                stm.setTimestamp(param++, m.exprired);
                stm.setString(param++, m.created_by);
                stm.setInt(param++, m.event);
                stm.setString(param++, m.user_name);
                stm.addBatch();
            }
            stm.executeBatch();
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{e});
            throw e;
        }
    }

    public static void insertGiftCode(GiftCodeModel giftCodeModel) throws SQLException {
        String sql = "INSERT INTO gift_codes (giftcode,`type`,money,time_used,max_use,`from`,exprired,created_by,event,user_name) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setString(param++, giftCodeModel.giftcode);
            stm.setInt(param++, giftCodeModel.type);
            stm.setLong(param++, giftCodeModel.money);
            stm.setInt(param++, giftCodeModel.time_used);
            stm.setInt(param++, giftCodeModel.max_use);
            stm.setTimestamp(param++, giftCodeModel.from);
            stm.setTimestamp(param++, giftCodeModel.exprired);
            stm.setString(param++, giftCodeModel.created_by);
            stm.setInt(param++, giftCodeModel.event);
            stm.setString(param++, giftCodeModel.user_name);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{e});
            throw e;
        }
    }

    public static GiftCodeModel parseGiftCodeModel(ResultSet rs) throws SQLException {
        GiftCodeModel giftCodeModel = new GiftCodeModel();
        giftCodeModel.id = rs.getInt("id");
        giftCodeModel.giftcode = rs.getString("giftcode");
        giftCodeModel.type = rs.getInt("type");
        giftCodeModel.money = rs.getInt("money");
        giftCodeModel.time_used = rs.getInt("time_used");
        giftCodeModel.max_use = rs.getInt("max_use");
        giftCodeModel.from = rs.getTimestamp("from");
        giftCodeModel.exprired = rs.getTimestamp("exprired");
        giftCodeModel.created_at = rs.getTimestamp("created_at");
        giftCodeModel.created_by = rs.getString("created_by");
        giftCodeModel.event = rs.getInt("event");
        giftCodeModel.user_name = rs.getString("user_name");
        return giftCodeModel;
    }

    public static void deleteGiftCode(String giftCode) throws SQLException {
        String sql = "DELETE FROM gift_codes where giftcode = ?;";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setString(param++, giftCode);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{e});
            throw e;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static long insertGiftCodeBundle(GiftCodeBundleModel newBundle) throws SQLException {
        String sql = "INSERT INTO gift_code_bundles (name,created_by) VALUES (?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql, 1);
            int param = 1;
            stm.setString(param++, newBundle.getName());
            stm.setString(param++, newBundle.getCreated_by());
            Debug.trace((Object[])new Object[]{String.format("insertGiftCodeBundle - name=%s, created_by=%s", newBundle.getName(), newBundle.getCreated_by())});
            stm.executeUpdate();
            long bundleId = 0L;
            try (ResultSet generatedKeys = stm.getGeneratedKeys();){
                if (!generatedKeys.next()) throw new SQLException("Creating giftcode bundle failed, no ID obtained.");
                bundleId = generatedKeys.getLong(1);
            }
            stm.close();
            Debug.trace((Object[])new Object[]{"insertGiftCodeBundle - success, bundleId=" + bundleId});
            long l = bundleId;
            return l;
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{e});
            throw e;
        }
    }

    public static void insertGiftCodeWithBundle(GiftCodeModel giftCodeModel) throws SQLException {
        String sql = "INSERT INTO gift_codes (giftcode,`type`,money,time_used,max_use,`from`,exprired,created_by,event,user_name,bundle_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql, 1);
            int param = 1;
            stm.setString(param++, giftCodeModel.giftcode);
            stm.setInt(param++, giftCodeModel.type);
            stm.setLong(param++, giftCodeModel.money);
            stm.setInt(param++, giftCodeModel.time_used);
            stm.setInt(param++, giftCodeModel.max_use);
            stm.setTimestamp(param++, giftCodeModel.from);
            stm.setTimestamp(param++, giftCodeModel.exprired);
            stm.setString(param++, giftCodeModel.created_by);
            stm.setInt(param++, giftCodeModel.event);
            stm.setString(param++, giftCodeModel.user_name);
            stm.setLong(param++, giftCodeModel.bundle_id);
            Debug.trace((Object[])new Object[]{String.format("insertGiftCodeWithBundle - giftcode=%s, type=%d, money=%d, from=%s, to=%s, created_by=%s, event=%d, user_name=%s, bundle_id=%d", giftCodeModel.giftcode, giftCodeModel.type, giftCodeModel.money, giftCodeModel.from, giftCodeModel.exprired, giftCodeModel.created_by, giftCodeModel.event, giftCodeModel.user_name, giftCodeModel.bundle_id)});
            stm.executeUpdate();
            stm.close();
            Debug.trace((Object[])new Object[]{"insertGiftCodeWithBundle - insert success, giftcode=" + giftCodeModel.giftcode});
        }
        catch (SQLException e) {
            Debug.trace((Object[])new Object[]{e});
            throw e;
        }
    }
}

