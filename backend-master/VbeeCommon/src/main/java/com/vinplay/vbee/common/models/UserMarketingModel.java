/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class UserMarketingModel {
    public String userName;
    public String utmCampaign;
    public String utmMedium;
    public String utmSource;
    public String timeLogin;
    public String nickName;

    public UserMarketingModel(String nickName, String utmCampaign, String utmMedium, String utmSource, String timeLogin, String userName) {
        this.nickName = nickName;
        this.utmCampaign = utmCampaign;
        this.utmMedium = utmMedium;
        this.utmSource = utmSource;
        this.timeLogin = timeLogin;
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUtmCampaign() {
        return this.utmCampaign;
    }

    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }

    public String getUtmMedium() {
        return this.utmMedium;
    }

    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    public String getUtmSource() {
        return this.utmSource;
    }

    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    public String getTimeLogin() {
        return this.timeLogin;
    }

    public void setTimeLogin(String timeLogin) {
        this.timeLogin = timeLogin;
    }

    public UserMarketingModel(String nickName, String utmCampaign, String utmMedium, String utmSource, String timeLogin) {
        this.nickName = nickName;
        this.utmCampaign = utmCampaign;
        this.utmMedium = utmMedium;
        this.utmSource = utmSource;
        this.timeLogin = timeLogin;
    }
}

