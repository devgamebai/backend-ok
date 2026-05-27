/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;
import java.util.Date;

public class SocialModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accessToken;
    private String socialId;
    private Date loginTime;

    public SocialModel(String accessToken, String socialId, Date loginTime) {
        this.accessToken = accessToken;
        this.socialId = socialId;
        this.loginTime = loginTime;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSocialId() {
        return this.socialId;
    }

    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    public Date getLoginTime() {
        return this.loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }
}

