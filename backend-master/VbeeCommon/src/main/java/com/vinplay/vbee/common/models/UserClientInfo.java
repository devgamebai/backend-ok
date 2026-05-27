/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

public class UserClientInfo {
    private long userId;
    private String nickname;
    private String avatar;
    private long vinTotal;
    private long xuTotal;
    private int vippoint;
    private int vippointSave;
    private String createTime;
    private String ipAddress;
    private boolean certificate;
    private int luckyRotate;
    private int daiLy;
    private int mobileSecure;
    private String birthday;
    private int appSecure;
    private String email;
    private boolean verifyMobile;
    private String username;
    private String address;
    private String mobile;
    private String referralCode; // player's registration referral code (from users.referral_code)

    public UserClientInfo(String nickname, String avatar, long vinTotal, long xuTotal, int vippoint, int vippointSave, String createTime, String ipAddress, boolean certificate, int luckyRotate, int daiLy, int mobileSecure, String birthday, int appSecure, String email, boolean verifyMobile, String username, String address) {
        this.userId = 0;
        this.nickname = nickname;
        this.avatar = avatar;
        // SUN-748 pattern #4 — clamp through setters so negative cumulative-P&L
        // values can't leak onto the login sessionKey (see setter javadoc).
        this.vinTotal = com.vinplay.vbee.common.response.BalanceGuard.clamp(vinTotal, "UserClientInfo.vinTotal");
        this.xuTotal = com.vinplay.vbee.common.response.BalanceGuard.clamp(xuTotal, "UserClientInfo.xuTotal");
        this.vippoint = vippoint;
        this.vippointSave = vippointSave;
        this.createTime = createTime;
        this.ipAddress = ipAddress;
        this.certificate = certificate;
        this.luckyRotate = luckyRotate;
        this.daiLy = daiLy;
        this.mobileSecure = mobileSecure;
        this.birthday = birthday;
        this.appSecure = appSecure;
        this.email = email;
        this.verifyMobile = verifyMobile;
        this.username = username;
        this.address = address;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public int getMobileSecure() {
        return this.mobileSecure;
    }

    public void setMobileSecure(int mobileSecure) {
        this.mobileSecure = mobileSecure;
    }

    public int getDaiLy() {
        return this.daiLy;
    }

    public void setDaiLy(int daiLy) {
        this.daiLy = daiLy;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getVinTotal() {
        return this.vinTotal;
    }

    public void setVinTotal(long vinTotal) {
        // SUN-748 pattern #4: UserClientInfo ships on login sessionKey and the
        // client treats this field as the current wallet balance. Clamp negative
        // values so losing players don't see -X on the login screen if anyone
        // ever pipes users.vin_total (cumulative P&L) through this setter again.
        this.vinTotal = com.vinplay.vbee.common.response.BalanceGuard.clamp(vinTotal, "UserClientInfo.vinTotal");
    }

    public long getXuTotal() {
        return this.xuTotal;
    }

    public void setXuTotal(long xuTotal) {
        this.xuTotal = com.vinplay.vbee.common.response.BalanceGuard.clamp(xuTotal, "UserClientInfo.xuTotal");
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

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isCertificate() {
        return this.certificate;
    }

    public void setCertificate(boolean certificate) {
        this.certificate = certificate;
    }

    public int getLuckyRotate() {
        return this.luckyRotate;
    }

    public void setLuckyRotate(int luckyRotate) {
        this.luckyRotate = luckyRotate;
    }

    public int getAppSecure() { return this.appSecure; }
    public void setAppSecure(int appSecure) { this.appSecure = appSecure; }
    public String getEmail() { return this.email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isVerifyMobile() { return this.verifyMobile; }
    public void setVerifyMobile(boolean verifyMobile) { this.verifyMobile = verifyMobile; }
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getAddress() { return this.address; }
    public void setAddress(String address) { this.address = address; }
    public String getMobile() { return this.mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public long getUserId() { return this.userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getReferralCode() { return this.referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
}

