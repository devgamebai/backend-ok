/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class UserExtraInfoModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nickname;
    private String platfrom;

    public UserExtraInfoModel(String nickname, String platform) {
        this.nickname = nickname;
        this.platfrom = platform;
    }

    public String getPlatfrom() {
        return this.platfrom;
    }

    public void setPlatfrom(String platfrom) {
        this.platfrom = platfrom;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

