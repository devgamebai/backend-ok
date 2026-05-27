/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;
import java.util.Date;

public class UserModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String username;
    private String nickname;
    private String password;
    private String email;
    private String facebookId;
    private String googleId;
    private String mobile;
    private String birthday;
    private boolean gender;
    private String address;
    private long vin;
    private long xu;
    private long vinTotal;
    private long xuTotal;
    private long safe;
    private long rechargeMoney;
    private int vippoint;
    private int daily;
    private Date createTime;
    private int status;
    private boolean banLogin;
    private boolean banCashOut;
    private boolean canLoginSandbox;
    private boolean banTransferMoney;
    private boolean hasMobileSecurity;
    private boolean hasEmailSecurity;
    private boolean hasAppSecurity;
    private boolean hasLoginSecurity;
    private String avatar;
    private String identification;
    private int vippointSave;
    private int moneyVP;
    private Date securityTime;
    private long loginOtp;
    private boolean bot;
    private boolean verifyMobile;
    private int usertype;
    private String teleId;
    private String client;
    private long manualQuota;
    private String referralCode;
    private String otpSender;
    private long giftTotal;
    private int vpLevelReceive;
    private boolean live;

    public UserModel() {
    }

    /** Backward-compatible constructor for precompiled JARs (28 params). */
    public UserModel(int id, String username, String nickname, String password, String email, String facebookId, String googleId, String mobile, String birthday, boolean gender, String address, long vin, long xu, long vinTotal, long xuTotal, long safe, long rechargeMoney, int vippoint, int daily, int status, String avatar, String identification, int vippointSave, Date createTime, int moneyVP, Date securityTime, long loginOtp, boolean bot) {
        this(id, username, nickname, password, email, facebookId, googleId, mobile, birthday, gender, address, vin, xu, vinTotal, xuTotal, safe, rechargeMoney, vippoint, daily, status, avatar, identification, vippointSave, createTime, moneyVP, securityTime, loginOtp, bot, false, 0, null, false);
    }

    public UserModel(int id, String username, String nickname, String password, String email, String facebookId, String googleId, String mobile, String birthday, boolean gender, String address, long vin, long xu, long vinTotal, long xuTotal, long safe, long rechargeMoney, int vippoint, int daily, int status, String avatar, String identification, int vippointSave, Date createTime, int moneyVP, Date securityTime, long loginOtp, boolean bot, boolean verifyMobile, int usertype, String teleId, boolean isLive) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.facebookId = facebookId;
        this.googleId = googleId;
        this.mobile = mobile;
        this.birthday = birthday;
        this.gender = gender;
        this.address = address;
        this.vin = vin;
        this.xu = xu;
        this.vinTotal = vinTotal;
        this.xuTotal = xuTotal;
        this.safe = safe;
        this.rechargeMoney = rechargeMoney;
        this.vippoint = vippoint;
        this.daily = daily;
        this.status = status;
        this.avatar = avatar;
        this.identification = identification;
        this.vippointSave = vippointSave;
        this.createTime = createTime;
        this.moneyVP = moneyVP;
        this.securityTime = securityTime;
        this.loginOtp = loginOtp;
        this.bot = bot;
        this.verifyMobile = verifyMobile;
        this.usertype = usertype;
        this.teleId = teleId;
        this.live = isLive;
        if ((status & 1) != 0) {
            this.setBanLogin(true);
        }
        if ((status & 2) != 0) {
            this.setBanCashOut(true);
        }
        if ((status & 4) != 0) {
            this.setCanLoginSandbox(true);
        }
        if ((status & 8) != 0) {
            this.setBanTransferMoney(true);
        }
        if ((status & 16) != 0) {
            this.setHasMobileSecurity(true);
        }
        if ((status & 32) != 0) {
            this.setHasEmailSecurity(true);
        }
        if ((status & 64) != 0) {
            this.setHasAppSecurity(true);
        }
        if ((status & 128) != 0) {
            this.setHasLoginSecurity(true);
        }
    }

    public boolean isBot() {
        return this.bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public boolean isHasLoginSecurity() {
        return this.hasLoginSecurity;
    }

    public void setHasLoginSecurity(boolean hasLoginSecurity) {
        this.hasLoginSecurity = hasLoginSecurity;
    }

    public long getLoginOtp() {
        return this.loginOtp;
    }

    public void setLoginOtp(long loginOtp) {
        this.loginOtp = loginOtp;
    }

    public boolean isHasAppSecurity() {
        return this.hasAppSecurity;
    }

    public void setHasAppSecurity(boolean hasAppSecurity) {
        this.hasAppSecurity = hasAppSecurity;
    }

    public String getGoogleId() {
        return this.googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public Date getSecurityTime() {
        return this.securityTime;
    }

    public void setSecurityTime(Date securityTime) {
        this.securityTime = securityTime;
    }

    public int getMoneyVP() {
        return this.moneyVP;
    }

    public void setMoneyVP(int moneyVP) {
        this.moneyVP = moneyVP;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getVippointSave() {
        return this.vippointSave;
    }

    public void setVippointSave(int vippointSave) {
        this.vippointSave = vippointSave;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getIdentification() {
        return this.identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public String toJson() {
        return "";
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getFacebookId() {
        return this.facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public String getMobile() {
        return this.mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getBirthday() {
        return this.birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public boolean isGender() {
        return this.gender;
    }

    public void setGender(boolean gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getVin() {
        return this.vin;
    }

    public void setVin(long vin) {
        this.vin = vin;
    }

    public long getXu() {
        return this.xu;
    }

    public void setXu(long xu) {
        this.xu = xu;
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

    public int getDaily() {
        return this.daily;
    }

    public void setDaily(int daily) {
        this.daily = daily;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isBanLogin() {
        return this.banLogin;
    }

    public void setBanLogin(boolean banLogin) {
        this.banLogin = banLogin;
    }

    public boolean isBanCashOut() {
        return this.banCashOut;
    }

    public void setBanCashOut(boolean banCashOut) {
        this.banCashOut = banCashOut;
    }

    public boolean isCanLoginSandbox() {
        return this.canLoginSandbox;
    }

    public void setCanLoginSandbox(boolean canLoginSandbox) {
        this.canLoginSandbox = canLoginSandbox;
    }

    public boolean isBanTransferMoney() {
        return this.banTransferMoney;
    }

    public void setBanTransferMoney(boolean banTransferMoney) {
        this.banTransferMoney = banTransferMoney;
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

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public long getMoney(String moneyType) {
        return moneyType.equalsIgnoreCase("vin") ? this.getVin() : this.getXu();
    }

    public void setMoney(String moneyType, long money) {
        if (moneyType.equalsIgnoreCase("vin")) {
            this.setVin(money);
        } else {
            this.setXu(money);
        }
    }

    /**
     * Returns the cumulative profit/loss counter for the given money type,
     * NOT the current balance. This reads the {@code vin_total}/{@code xu_total}
     * column, which tracks lifetime game winnings/losses (can be negative for
     * losing players). Do NOT send this to the client as "current balance" —
     * use {@link #getMoney(String)} (or {@link #getVin()} / {@link #getXu()})
     * for the real wallet balance.
     *
     * <p>Renamed from {@code getCurrentMoney(String)} on 2026-04-10 to kill
     * the SUN-748 / SUN-753 class of bugs where the old name made developers
     * mistake this for the current balance.
     */
    public long getTotalPnl(String moneyType) {
        return moneyType.equalsIgnoreCase("vin") ? this.getVinTotal() : this.getXuTotal();
    }

    /**
     * Sets the cumulative profit/loss counter ({@code vin_total}/{@code xu_total})
     * for the given money type. Use {@link #setMoney(String, long)} for the
     * actual balance.
     *
     * <p>Renamed from {@code setCurrentMoney(String, long)} on 2026-04-10 to
     * kill the SUN-748 / SUN-753 class of bugs.
     */
    public void setTotalPnl(String moneyType, long money) {
        if (moneyType.equalsIgnoreCase("vin")) {
            this.setVinTotal(money);
        } else {
            this.setXuTotal(money);
        }
    }

    public boolean isVerifyMobile() { return this.verifyMobile; }
    public void setVerifyMobile(boolean v) { this.verifyMobile = v; }
    public int getUsertype() { return this.usertype; }
    public void setUsertype(int v) { this.usertype = v; }
    public String getTeleId() { return this.teleId; }
    public void setTeleId(String v) { this.teleId = v; }
    public String getClient() { return this.client; }
    public void setClient(String v) { this.client = v; }
    public long getManualQuota() { return this.manualQuota; }
    public void setManualQuota(long v) { this.manualQuota = v; }
    public String getReferralCode() { return this.referralCode; }
    public void setReferralCode(String v) { this.referralCode = v; }
    public String getOtpSender() { return this.otpSender; }
    public void setOtpSender(String v) { this.otpSender = v; }
    public long getGiftTotal() { return this.giftTotal; }
    public void setGiftTotal(long v) { this.giftTotal = v; }
    public long getGift_total() { return this.giftTotal; }
    public void setGift_total(long v) { this.giftTotal = v; }
    public int getVpLevelReceive() { return this.vpLevelReceive; }
    public void setVpLevelReceive(int v) { this.vpLevelReceive = v; }
    public boolean isLive() { return this.live; }
    public void setLive(boolean v) { this.live = v; }
}

