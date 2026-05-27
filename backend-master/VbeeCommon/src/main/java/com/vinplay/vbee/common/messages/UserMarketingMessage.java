/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

public class UserMarketingMessage {
    public String userName;
    public String nickName;
    public int numLogin;
    public String timeLogin;
    public String utmCampaign;
    public String utmMedium;
    public String utmSource;

    public UserMarketingMessage() {
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

    public int getNumLogin() {
        return this.numLogin;
    }

    public void setNumLogin(int numLogin) {
        this.numLogin = numLogin;
    }

    public String getTimeLogin() {
        return this.timeLogin;
    }

    public void setTimeLogin(String timeLogin) {
        this.timeLogin = timeLogin;
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

    public UserMarketingMessage(String userName, String nickName, int numLogin, String timeLogin, String utmCampaign, String utmMedium, String utmSource) {
        this.userName = userName;
        this.nickName = nickName;
        this.numLogin = numLogin;
        this.timeLogin = timeLogin;
        this.utmCampaign = utmCampaign;
        this.utmMedium = utmMedium;
        this.utmSource = utmSource;
    }
}

