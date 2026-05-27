/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.UserModel;
import java.io.Serializable;
import java.util.Date;

public class UserCacheModel
extends UserModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accessToken;
    private Date lastActive;
    private long lastMessageId;
    private int online;
    private int cashout;
    private Date cashoutTime;
    private int rechargeFail;
    private Date rechargeFailTime;
    private int rechargeMegaCardFail;
    private Date rechargeMegaCardFailTime;
    private int rechargeVCFail;
    private Date rechargeVCFailTime;
    private long rechargeVCInDay;
    private Date rechargeVCTime;
    private int apiOTPFail;
    private Date apiOTPFailTime;
    private Date apiOTPTime;
    private String ip;
    private String otpApp;
    private String campaign;
    private String medium;
    private String source;
    private int iapInDay;
    private Date iapTime;
    private int codeGameFail;
    private Date codeGameFailTime;
    private Date addVPTime;
    private Date subVPTime;
    private Date addVPVinTime;
    private int vpEvent;
    private int vpEventReal;
    private int vpAdd;
    private int vpSub;
    private int numAdd;
    private int numSub;
    private int place;
    private int placeMax;
    private Date lastActiveVin;
    private Date lastActiveXu;
    private String Platfrom;
    private long exchangeMoneyInDay;
    private Date exchangeMoneyTime;

    public UserCacheModel() {
    }

    /** Backward-compatible constructor for precompiled JARs (38 params). */
    public UserCacheModel(int id, String username, String nickname, String password, String email, String facebookId, String googleId, String mobile, String birthday, boolean gender, String address, long vin, long xu, long vinTotal, long xuTotal, long safe, long rechargeMoney, int vippoint, int daily, int status, String avatar, String identification, int vippointSave, Date createTime, int moneyVP, Date securityTime, long loginOtp, boolean bot, int cashout, Date cashoutTime, int vpEvent, int vpEventReal, int vpAdd, int vpSub, int numAdd, int numSub, int place, int placeMax) {
        this(id, username, nickname, password, email, facebookId, googleId, mobile, birthday, gender, address, vin, xu, vinTotal, xuTotal, safe, rechargeMoney, vippoint, daily, status, avatar, identification, vippointSave, createTime, moneyVP, securityTime, loginOtp, bot, false, cashout, cashoutTime, vpEvent, vpEventReal, vpAdd, vpSub, numAdd, numSub, place, placeMax, 0, null, false);
    }

    public UserCacheModel(int id, String username, String nickname, String password, String email, String facebookId, String googleId, String mobile, String birthday, boolean gender, String address, long vin, long xu, long vinTotal, long xuTotal, long safe, long rechargeMoney, int vippoint, int daily, int status, String avatar, String identification, int vippointSave, Date createTime, int moneyVP, Date securityTime, long loginOtp, boolean bot, boolean verifyMobile, int cashout, Date cashoutTime, int vpEvent, int vpEventReal, int vpAdd, int vpSub, int numAdd, int numSub, int place, int placeMax, int usertype, String teleId, boolean isLive) {
        super(id, username, nickname, password, email, facebookId, googleId, mobile, birthday, gender, address, vin, xu, vinTotal, xuTotal, safe, rechargeMoney, vippoint, daily, status, avatar, identification, vippointSave, createTime, moneyVP, securityTime, loginOtp, bot, verifyMobile, usertype, teleId, isLive);
        this.cashout = cashout;
        this.cashoutTime = cashoutTime;
        this.vpEvent = vpEvent;
        this.vpEventReal = vpEventReal;
        this.vpAdd = vpAdd;
        this.vpSub = vpSub;
        this.numAdd = numAdd;
        this.numSub = numSub;
        this.place = place;
        this.placeMax = placeMax;
    }

    public void setPlatfrom(String value) {
        this.Platfrom = value;
    }

    public String getPlatfrom() {
        return this.Platfrom;
    }

    public long getExchangeMoneyInDay() {
        return this.exchangeMoneyInDay;
    }

    public void setExchangeMoneyInDay(long exchangeMoneyInDay) {
        this.exchangeMoneyInDay = exchangeMoneyInDay;
    }

    public Date getExchangeMoneyTime() {
        return this.exchangeMoneyTime;
    }

    public void setExchangeMoneyTime(Date exchangeMoneyTime) {
        this.exchangeMoneyTime = exchangeMoneyTime;
    }

    public int getRechargeMegaCardFail() {
        return this.rechargeMegaCardFail;
    }

    public void setRechargeMegaCardFail(int rechargeMegaCardFail) {
        this.rechargeMegaCardFail = rechargeMegaCardFail;
    }

    public Date getRechargeMegaCardFailTime() {
        return this.rechargeMegaCardFailTime;
    }

    public void setRechargeMegaCardFailTime(Date rechargeMegaCardFailTime) {
        this.rechargeMegaCardFailTime = rechargeMegaCardFailTime;
    }

    public long getRechargeVCInDay() {
        return this.rechargeVCInDay;
    }

    public void setRechargeVCInDay(long rechargeVCInDay) {
        this.rechargeVCInDay = rechargeVCInDay;
    }

    public Date getRechargeVCTime() {
        return this.rechargeVCTime;
    }

    public void setRechargeVCTime(Date rechargeVCTime) {
        this.rechargeVCTime = rechargeVCTime;
    }

    public int getRechargeVCFail() {
        return this.rechargeVCFail;
    }

    public void setRechargeVCFail(int rechargeVCFail) {
        this.rechargeVCFail = rechargeVCFail;
    }

    public Date getRechargeVCFailTime() {
        return this.rechargeVCFailTime;
    }

    public void setRechargeVCFailTime(Date rechargeVCFailTime) {
        this.rechargeVCFailTime = rechargeVCFailTime;
    }

    public int getPlaceMax() {
        return this.placeMax;
    }

    public void setPlaceMax(int placeMax) {
        this.placeMax = placeMax;
    }

    public String getOtpApp() {
        return this.otpApp;
    }

    public void setOtpApp(String otpApp) {
        this.otpApp = otpApp;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Date getRechargeFailTime() {
        return this.rechargeFailTime;
    }

    public void setRechargeFailTime(Date rechargeFailTime) {
        this.rechargeFailTime = rechargeFailTime;
    }

    public int getRechargeFail() {
        return this.rechargeFail;
    }

    public void setRechargeFail(int rechargeFail) {
        this.rechargeFail = rechargeFail;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Date getLastActive() {
        return this.lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public long getLastMessageId() {
        return this.lastMessageId;
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public int getOnline() {
        return this.online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public int getCashout() {
        return this.cashout;
    }

    public void setCashout(int cashout) {
        this.cashout = cashout;
    }

    public Date getCashoutTime() {
        return this.cashoutTime;
    }

    public void setCashoutTime(Date cashoutTime) {
        this.cashoutTime = cashoutTime;
    }

    public String getCampaign() {
        return this.campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    public String getMedium() {
        return this.medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getIapInDay() {
        return this.iapInDay;
    }

    public void setIapInDay(int iapInDay) {
        this.iapInDay = iapInDay;
    }

    public Date getIapTime() {
        return this.iapTime;
    }

    public void setIapTime(Date iapTime) {
        this.iapTime = iapTime;
    }

    public Date getAddVPTime() {
        return this.addVPTime;
    }

    public void setAddVPTime(Date addVPTime) {
        this.addVPTime = addVPTime;
    }

    public Date getSubVPTime() {
        return this.subVPTime;
    }

    public void setSubVPTime(Date subVPTime) {
        this.subVPTime = subVPTime;
    }

    public Date getAddVPVinTime() {
        return this.addVPVinTime;
    }

    public void setAddVPVinTime(Date addVPVinTime) {
        this.addVPVinTime = addVPVinTime;
    }

    public int getVpEvent() {
        return this.vpEvent;
    }

    public void setVpEvent(int vpEvent) {
        this.vpEvent = vpEvent;
    }

    public int getVpEventReal() {
        return this.vpEventReal;
    }

    public void setVpEventReal(int vpEventReal) {
        this.vpEventReal = vpEventReal;
    }

    public int getVpAdd() {
        return this.vpAdd;
    }

    public void setVpAdd(int vpAdd) {
        this.vpAdd = vpAdd;
    }

    public int getVpSub() {
        return this.vpSub;
    }

    public void setVpSub(int vpSub) {
        this.vpSub = vpSub;
    }

    public int getNumAdd() {
        return this.numAdd;
    }

    public void setNumAdd(int numAdd) {
        this.numAdd = numAdd;
    }

    public int getNumSub() {
        return this.numSub;
    }

    public void setNumSub(int numSub) {
        this.numSub = numSub;
    }

    public int getPlace() {
        return this.place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public Date getLastActiveVin() {
        return this.lastActiveVin;
    }

    public void setLastActiveVin(Date lastActiveVin) {
        this.lastActiveVin = lastActiveVin;
    }

    public Date getLastActiveXu() {
        return this.lastActiveXu;
    }

    public void setLastActiveXu(Date lastActiveXu) {
        this.lastActiveXu = lastActiveXu;
    }

    public int getApiOTPFail() {
        return this.apiOTPFail;
    }

    public void setApiOTPFail(int apiOTPFail) {
        this.apiOTPFail = apiOTPFail;
    }

    public Date getApiOTPFailTime() {
        return this.apiOTPFailTime;
    }

    public void setApiOTPFailTime(Date apiOTPFailTime) {
        this.apiOTPFailTime = apiOTPFailTime;
    }

    public Date getApiOTPTime() {
        return this.apiOTPTime;
    }

    public void setApiOTPTime(Date apiOTPTime) {
        this.apiOTPTime = apiOTPTime;
    }

    public int getCodeGameFail() {
        return this.codeGameFail;
    }

    public void setCodeGameFail(int codeGameFail) {
        this.codeGameFail = codeGameFail;
    }

    public Date getCodeGameFailTime() {
        return this.codeGameFailTime;
    }

    public void setCodeGameFailTime(Date codeGameFailTime) {
        this.codeGameFailTime = codeGameFailTime;
    }

    // Stubs for precompiled LobbyModule compatibility
    public boolean isVerifyMobile() { return false; }
    public void setVerifyMobile(boolean v) {}
    public boolean isVerifyEmail() { return false; }
    public boolean isCanLoginSandbox() { return true; }
    public boolean isHasMobileSecurity() { return false; }
    public boolean isHasEmailSecurity() { return false; }
    public boolean isHasAppSecurity() { return false; }
    public boolean isHasLoginSecurity() { return false; }
    public int getUsertype() { return 0; }
    // getReferralCode() is inherited from UserModel (reads users.referral_code).
    // Previously overridden here to return "" as a precompiled-LobbyModule stub,
    // which prevented the real referral code from reaching the session key.
}

