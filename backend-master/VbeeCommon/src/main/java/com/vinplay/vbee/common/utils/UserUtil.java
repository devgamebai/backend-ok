/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import com.vinplay.vbee.common.models.FreezeModel;
import com.vinplay.vbee.common.models.PotModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.cache.UserMoneyModel;
import com.vinplay.vbee.common.models.cache.UserVippointEventModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserUtil {
    public static UserModel parseResultSetToUserModel(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("user_name");
        String nickname = rs.getString("nick_name");
        String password = rs.getString("password");
        String email = rs.getString("email");
        String facebookId = rs.getString("facebook_id");
        String googleId = rs.getString("google_id");
        String mobile = rs.getString("mobile");
        String birthday = rs.getDate("birthday") != null ? VinPlayUtils.parseDateToString(rs.getDate("birthday")) : null;
        boolean gender = rs.getBoolean("gender");
        String address = rs.getString("address");
        long vin = rs.getLong("vin");
        long xu = 0L;
        long vinTotal = 0L;
        long xuTotal = 0L;
        long safe = 0L;
        long rechargeMoney = 0L;
        int vipPoint = 0;
        int daiLy = rs.getInt("dai_ly");
        int status = rs.getInt("status");
        String avatar = rs.getString("avatar");
        String identification = rs.getString("identification");
        int vipPointSave = 0;
        int moneyVP = 0;
        long loginOtp = rs.getLong("login_otp");
        boolean bot = rs.getInt("is_bot") == 1;
        String sCreateTime = rs.getString("create_time");
        String sSecurityTime = rs.getString("security_time");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date createTime = null;
        Date securityTime = null;
        try {
            createTime = format.parse(sCreateTime);
            if (sSecurityTime != null && !sSecurityTime.isEmpty() && !sSecurityTime.toLowerCase().equals("null")) {
                securityTime = format.parse(sSecurityTime);
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        UserModel userModel = new UserModel(id, username, nickname, password, email, facebookId, googleId, mobile, birthday, gender, address, vin, xu, vinTotal, xuTotal, safe, rechargeMoney, vipPoint, daiLy, status, avatar, identification, vipPointSave, createTime, moneyVP, securityTime, loginOtp, bot, false, 0, null, false);
        // The UserModel positional constructor doesn't take referral_code, but
        // callers (LoginProcessor, PortalUtils.loginSuccess) expect it present
        // so the session key can expose the player's registration referral.
        try {
            userModel.setReferralCode(rs.getString("referral_code"));
        } catch (Exception ignored) {
            // column may be absent on selective SELECTs — leave field null
        }
        return userModel;
    }

    public static PotModel parseResultSetToPotModel(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String potName = rs.getString("pot_name");
        long value = rs.getLong("value");
        PotModel model = new PotModel(id, potName, value);
        return model;
    }

    public static FreezeModel parseResultSetToFreezeModel(ResultSet rs) throws SQLException {
        String sessionId = rs.getString("session_id");
        int userId = rs.getInt("user_id");
        String nickname = rs.getString("nick_name");
        String gamename = rs.getString("game_name");
        String roomId = rs.getString("room_id");
        long money = rs.getLong("money");
        String moneyType = rs.getString("money_type");
        int status = rs.getInt("status");
        String sCreateTime = rs.getString("create_time");
        Date createTime = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            createTime = format.parse(sCreateTime);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        FreezeModel model = new FreezeModel(sessionId, nickname, gamename, roomId, money, moneyType, createTime, null, status);
        model.setUserId(userId);
        return model;
    }

    public static UserVippointEventModel parseResultSetToUserVPEventModel(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String nickname = rs.getString("nick_name");
        boolean isBot = rs.getInt("is_bot") == 1;
        int vpReal = rs.getInt("vp_real");
        int vpEvent = rs.getInt("vp_event");
        int vpAdd = rs.getInt("vp_add");
        int numAdd = rs.getInt("num_add");
        int vpSub = rs.getInt("vp_sub");
        int numSub = rs.getInt("num_sub");
        int place = rs.getInt("place");
        int placeMax = rs.getInt("place_max");
        UserVippointEventModel model = new UserVippointEventModel(userId, nickname, isBot, null, null, null, vpEvent, vpReal, vpAdd, vpSub, numAdd, numSub, place, placeMax);
        return model;
    }

    public static UserMoneyModel getMoneyModel(UserModel model) {
        return new UserMoneyModel(model.getId(), model.getUsername(), model.getNickname(), model.isBot(), model.getVin(), model.getXu(), model.getVinTotal(), model.getXuTotal(), model.getSafe(), model.getRechargeMoney(), model.getVippoint(), model.getVippointSave(), model.getMoneyVP());
    }

    public static UserCacheModel getUserCacheModel(UserModel m) {
        return (UserCacheModel)m;
    }

    public static UserModel getUserModel(UserCacheModel userCache, UserMoneyModel userMoney) {
        return new UserModel(userCache.getId(), userCache.getUsername(), userCache.getNickname(), userCache.getPassword(), userCache.getEmail(), userCache.getFacebookId(), userCache.getGoogleId(), userCache.getMobile(), userCache.getBirthday(), userCache.isGender(), userCache.getAddress(), userMoney.getVin(), userMoney.getXu(), userMoney.getVinTotal(), userMoney.getXuTotal(), userMoney.getSafe(), userMoney.getRechargeMoney(), userMoney.getVippoint(), userCache.getDaily(), userCache.getStatus(), userCache.getAvatar(), userCache.getIdentification(), userMoney.getVippointSave(), userCache.getCreateTime(), userMoney.getMoneyVP(), userCache.getSecurityTime(), userCache.getLoginOtp(), userCache.isBot(), false, 0, null, false);
    }

    public static UserModel getUserModelFromUserCache(UserCacheModel userCache) {
        return new UserModel(userCache.getId(), userCache.getUsername(), userCache.getNickname(), userCache.getPassword(), userCache.getEmail(), userCache.getFacebookId(), userCache.getGoogleId(), userCache.getMobile(), userCache.getBirthday(), userCache.isGender(), userCache.getAddress(), 0L, 0L, 0L, 0L, 0L, 0L, 0, userCache.getDaily(), userCache.getStatus(), userCache.getAvatar(), userCache.getIdentification(), 0, userCache.getCreateTime(), 0, userCache.getSecurityTime(), userCache.getLoginOtp(), userCache.isBot(), false, 0, null, false);
    }
}

