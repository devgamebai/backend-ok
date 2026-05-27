/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class UserAdminInfo {
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private String identification;
    private long vinTotal;
    private long xuTotal;
    private long safe;
    private long rechargeMoney;
    private int vippoint;
    private int vippointSave;
    private long loginOtp;
    private boolean bot;
    private String createTime;
    private String securityTime;
    private int status;
    private boolean hasMobileSecurity;
    private boolean hasEmailSecurity;
    private String google_id;
    private String facebook_id;
    private String birthday;

    public UserAdminInfo() {
    }

    public UserAdminInfo(String username, String nickname, String email, String mobile, String identification, long vinTotal, long xuTotal, long safe, long rechargeMoney, int vippoint, int vippointSave, long loginOtp, boolean bot, String createTime, String securityTime, int status, String googleid, String facebookid, String birthday) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.mobile = mobile;
        this.identification = identification;
        this.vinTotal = vinTotal;
        this.xuTotal = xuTotal;
        this.safe = safe;
        this.rechargeMoney = rechargeMoney;
        this.vippoint = vippoint;
        this.vippointSave = vippointSave;
        this.loginOtp = loginOtp;
        this.bot = bot;
        this.createTime = createTime;
        this.securityTime = securityTime;
        this.status = status;
        if ((status & 16) != 0) {
            this.setHasMobileSecurity(true);
        }
        if ((status & 32) != 0) {
            this.setHasEmailSecurity(true);
        }
        this.google_id = googleid;
        this.facebook_id = facebookid;
        this.birthday = birthday;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getGoogle_id() {
        return this.google_id;
    }

    public void setGoogle_id(String google_id) {
        this.google_id = google_id;
    }

    public String getFacebook_id() {
        return this.facebook_id;
    }

    public void setFacebook_id(String facebook_id) {
        this.facebook_id = facebook_id;
    }

    public String getSecurityTime() {
        return this.securityTime;
    }

    public void setSecurityTime(String securityTime) {
        this.securityTime = securityTime;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getIdentification() {
        return this.identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public long getVinTotal() {
        return this.vinTotal;
    }

    public void setVinTotal(long vinTotal) {
        this.vinTotal = vinTotal;
    }

    public long getXuTotal() {
        return this.xuTotal;
    }

    public void setXuTotal(long xuTotal) {
        this.xuTotal = xuTotal;
    }

    public long getSafe() {
        return this.safe;
    }

    public void setSafe(long safe) {
        this.safe = safe;
    }

    public long getRechargeMoney() {
        return this.rechargeMoney;
    }

    public void setRechargeMoney(long rechargeMoney) {
        this.rechargeMoney = rechargeMoney;
    }

    public int getVippoint() {
        return this.vippoint;
    }

    public void setVippoint(int vippoint) {
        this.vippoint = vippoint;
    }

    public int getVippointSave() {
        return this.vippointSave;
    }

    public void setVippointSave(int vippointSave) {
        this.vippointSave = vippointSave;
    }

    public long getLoginOtp() {
        return this.loginOtp;
    }

    public void setLoginOtp(long loginOtp) {
        this.loginOtp = loginOtp;
    }

    public boolean isBot() {
        return this.bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public boolean isHasMobileSecurity() {
        return this.hasMobileSecurity;
    }

    public void setHasMobileSecurity(boolean hasMobileSecurity) {
        this.hasMobileSecurity = hasMobileSecurity;
    }

    public boolean isHasEmailSecurity() {
        return this.hasEmailSecurity;
    }

    public void setHasEmailSecurity(boolean hasEmailSecurity) {
        this.hasEmailSecurity = hasEmailSecurity;
    }
}

