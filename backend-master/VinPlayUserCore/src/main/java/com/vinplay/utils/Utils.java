/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.ServletInputStream
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.utils;

import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.service.impl.MarketingServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.MarketingModel;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class Utils {
    public static UserCacheModel genUserCacheModel(String nickName) {
        UserServiceImpl userService = new UserServiceImpl();
        UserCacheModel userCache = null;
        try {
            UserServiceImpl ser;
            UserCacheModel uc2;
            UserModel userModel = userService.getUserByNickName(nickName);
            MarketingServiceImpl mktService = new MarketingServiceImpl();
            MarketingModel mktModel = mktService.getMKTInfo(userModel.getUsername());
            VippointDaoImpl vpDao = new VippointDaoImpl();
            UserVPEventModel vpModel = vpDao.getUserVPByNickName(userModel.getNickname());
            userCache = new UserCacheModel(userModel.getId(), userModel.getUsername(), userModel.getNickname(), userModel.getPassword(), userModel.getEmail(), userModel.getFacebookId(), userModel.getGoogleId(), userModel.getMobile(), userModel.getBirthday(), userModel.isGender(), userModel.getAddress(), userModel.getVin(), userModel.getXu(), userModel.getVinTotal(), userModel.getXuTotal(), userModel.getSafe(), userModel.getRechargeMoney(), userModel.getVippoint(), userModel.getDaily(), userModel.getStatus(), userModel.getAvatar(), userModel.getIdentification(), userModel.getVippointSave(), userModel.getCreateTime(), userModel.getMoneyVP(), userModel.getSecurityTime(), userModel.getLoginOtp(), userModel.isBot(), userModel.isVerifyMobile(), 0, null, vpModel.getVpEvent(), vpModel.getVpReal(), vpModel.getVpAdd(), vpModel.getVpSub(), vpModel.getNumAdd(), vpModel.getNumSub(), vpModel.getPlace(), vpModel.getPlaceMax(), userModel.getUsertype(), userModel.getReferralCode(), userModel.isLive());
            userCache.setClient(userModel.getClient());
            String accessToken = VinPlayUtils.genAccessToken(userModel.getId());
            userCache.setAccessToken(accessToken);
            userCache.setLastMessageId(0L);
            userCache.setLastActive(new Date());
            userCache.setOnline(0);
            userCache.setIp("ip");
            if (mktModel != null) {
                userCache.setCampaign(mktModel.campaign);
                userCache.setMedium(mktModel.medium);
                userCache.setSource(mktModel.source);
            }
            if ((uc2 = (ser = new UserServiceImpl()).checkMoneyNegative(userCache)) != null) {
                userCache = uc2;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return userCache;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String getBody(HttpServletRequest request) {
        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            ServletInputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader((InputStream)inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        body = stringBuilder.toString();
        return body;
    }
}

