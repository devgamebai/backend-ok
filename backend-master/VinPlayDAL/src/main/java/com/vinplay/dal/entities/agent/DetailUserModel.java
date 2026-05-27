/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.agent;

import java.util.Date;

public class DetailUserModel {
    private String user_name;
    private String nick_name;
    private String email;
    private String google_id;
    private String facebook_id;
    private String mobile;
    private String identification;
    private Integer avatar;
    private Date birthday;
    private Boolean gender;
    private String address;
    private Long vin;
    private Long xu;
    private Long vin_total;
    private Long xu_total;
    private Long safe;
    private Long recharge_money;
    private Integer vip_point;
    private Integer vip_point_save;
    private Integer money_vp;
    private Integer dai_ly;
    private Integer status;
    private Date create_time;
    private Date security_time;
    private Long login_otp;
    private Integer is_bot;
    private Date update_pw_time;
    private Boolean is_verify_mobile;
    private String referral_code;
    private Long t_nap;
    private Long t_rut;
    private Date rut_times;
    private Date nap_times;
    private Integer totalUserOfAgency;

    public DetailUserModel() {
    }

    public DetailUserModel(String user_name, String nick_name, String email, String google_id, String facebook_id, String mobile, String identification, Integer avatar, Date birthday, Boolean gender, String address, Long vin, Long xu, Long vin_total, Long xu_total, Long safe, Long recharge_money, Integer vip_point, Integer vip_point_save, Integer money_vp, Integer dai_ly, Integer status, Date create_time, Date security_time, Long login_otp, Integer is_bot, Date update_pw_time, Boolean is_verify_mobile, String referral_code, Long t_nap, Long t_rut, Integer totalUserOfAgency) {
        this.user_name = user_name;
        this.nick_name = nick_name;
        this.email = email;
        this.google_id = google_id;
        this.facebook_id = facebook_id;
        this.mobile = mobile;
        this.identification = identification;
        this.avatar = avatar;
        this.birthday = birthday;
        this.gender = gender;
        this.address = address;
        this.vin = vin;
        this.xu = xu;
        this.vin_total = vin_total;
        this.xu_total = xu_total;
        this.safe = safe;
        this.recharge_money = recharge_money;
        this.vip_point = vip_point;
        this.vip_point_save = vip_point_save;
        this.money_vp = money_vp;
        this.dai_ly = dai_ly;
        this.status = status;
        this.create_time = create_time;
        this.security_time = security_time;
        this.login_otp = login_otp;
        this.is_bot = is_bot;
        this.update_pw_time = update_pw_time;
        this.is_verify_mobile = is_verify_mobile;
        this.referral_code = referral_code;
        this.t_nap = t_nap;
        this.t_rut = t_rut;
        this.totalUserOfAgency = totalUserOfAgency;
    }

    public DetailUserModel(String user_name, String nick_name, String email, String google_id, String facebook_id, String mobile, String identification, Integer avatar, Date birthday, Boolean gender, String address, Long vin, Long xu, Long vin_total, Long xu_total, Long safe, Long recharge_money, Integer vip_point, Integer vip_point_save, Integer money_vp, Integer dai_ly, Integer status, Date create_time, Date security_time, Long login_otp, Integer is_bot, Date update_pw_time, Boolean is_verify_mobile, String referral_code, Long t_nap, Long t_rut, Date rut_times, Date nap_times, Integer totalUserOfAgency) {
        this.user_name = user_name;
        this.nick_name = nick_name;
        this.email = email;
        this.google_id = google_id;
        this.facebook_id = facebook_id;
        this.mobile = mobile;
        this.identification = identification;
        this.avatar = avatar;
        this.birthday = birthday;
        this.gender = gender;
        this.address = address;
        this.vin = vin;
        this.xu = xu;
        this.vin_total = vin_total;
        this.xu_total = xu_total;
        this.safe = safe;
        this.recharge_money = recharge_money;
        this.vip_point = vip_point;
        this.vip_point_save = vip_point_save;
        this.money_vp = money_vp;
        this.dai_ly = dai_ly;
        this.status = status;
        this.create_time = create_time;
        this.security_time = security_time;
        this.login_otp = login_otp;
        this.is_bot = is_bot;
        this.update_pw_time = update_pw_time;
        this.is_verify_mobile = is_verify_mobile;
        this.referral_code = referral_code;
        this.t_nap = t_nap;
        this.t_rut = t_rut;
        this.rut_times = rut_times;
        this.nap_times = nap_times;
        this.totalUserOfAgency = totalUserOfAgency;
    }

    public Date getRut_times() {
        return this.rut_times;
    }

    public void setRut_times(Date rut_times) {
        this.rut_times = rut_times;
    }

    public Date getNap_times() {
        return this.nap_times;
    }

    public void setNap_times(Date nap_times) {
        this.nap_times = nap_times;
    }

    public String getUser_name() {
        return this.user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getNick_name() {
        return this.nick_name;
    }

    public void setNick_name(String nick_name) {
        this.nick_name = nick_name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Integer getAvatar() {
        return this.avatar;
    }

    public void setAvatar(Integer avatar) {
        this.avatar = avatar;
    }

    public Date getBirthday() {
        return this.birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Boolean getGender() {
        return this.gender;
    }

    public void setGender(Boolean gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getVin() {
        return this.vin;
    }

    public void setVin(Long vin) {
        this.vin = vin;
    }

    public Long getXu() {
        return this.xu;
    }

    public void setXu(Long xu) {
        this.xu = xu;
    }

    public Long getVin_total() {
        return this.vin_total;
    }

    public void setVin_total(Long vin_total) {
        this.vin_total = vin_total;
    }

    public Long getXu_total() {
        return this.xu_total;
    }

    public void setXu_total(Long xu_total) {
        this.xu_total = xu_total;
    }

    public Long getSafe() {
        return this.safe;
    }

    public void setSafe(Long safe) {
        this.safe = safe;
    }

    public Long getRecharge_money() {
        return this.recharge_money;
    }

    public void setRecharge_money(Long recharge_money) {
        this.recharge_money = recharge_money;
    }

    public Integer getVip_point() {
        return this.vip_point;
    }

    public void setVip_point(Integer vip_point) {
        this.vip_point = vip_point;
    }

    public Integer getVip_point_save() {
        return this.vip_point_save;
    }

    public void setVip_point_save(Integer vip_point_save) {
        this.vip_point_save = vip_point_save;
    }

    public Integer getMoney_vp() {
        return this.money_vp;
    }

    public void setMoney_vp(Integer money_vp) {
        this.money_vp = money_vp;
    }

    public Integer getDai_ly() {
        return this.dai_ly;
    }

    public void setDai_ly(Integer dai_ly) {
        this.dai_ly = dai_ly;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreate_time() {
        return this.create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public Date getSecurity_time() {
        return this.security_time;
    }

    public void setSecurity_time(Date security_time) {
        this.security_time = security_time;
    }

    public Long getLogin_otp() {
        return this.login_otp;
    }

    public void setLogin_otp(Long login_otp) {
        this.login_otp = login_otp;
    }

    public Integer getIs_bot() {
        return this.is_bot;
    }

    public void setIs_bot(Integer is_bot) {
        this.is_bot = is_bot;
    }

    public Date getUpdate_pw_time() {
        return this.update_pw_time;
    }

    public void setUpdate_pw_time(Date update_pw_time) {
        this.update_pw_time = update_pw_time;
    }

    public Boolean getIs_verify_mobile() {
        return this.is_verify_mobile;
    }

    public void setIs_verify_mobile(Boolean is_verify_mobile) {
        this.is_verify_mobile = is_verify_mobile;
    }

    public String getReferral_code() {
        return this.referral_code;
    }

    public void setReferral_code(String referral_code) {
        this.referral_code = referral_code;
    }

    public Long getT_nap() {
        return this.t_nap;
    }

    public void setT_nap(Long t_nap) {
        this.t_nap = t_nap;
    }

    public Long getT_rut() {
        return this.t_rut;
    }

    public void setT_rut(Long t_rut) {
        this.t_rut = t_rut;
    }

    public Integer getTotalUserOfAgency() {
        return this.totalUserOfAgency;
    }

    public void setTotalUserOfAgency(Integer totalUserOfAgency) {
        this.totalUserOfAgency = totalUserOfAgency;
    }
}

