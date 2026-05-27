/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.agent;

import java.util.Date;

public class UserAgentModel {
    private Integer id;
    private String username;
    private String nickname;
    private String password;
    private String nameagent;
    private String address;
    private String phone;
    private String email;
    private String facebook;
    private String key;
    private String status;
    private Integer parentid;
    private String namebank;
    private String nameaccount;
    private String numberaccount;
    private Integer show;
    private Integer active;
    private Date createtime;
    private Date updatetime;
    private Integer order;
    private Integer sms;
    private Integer percent_bonus_vincard;
    private Double commission_rate;
    private String site;
    private Date last_login_time;
    private Integer login_times;
    private Integer level;
    private String code;
    private String ancestors;

    public UserAgentModel() {
    }

    public UserAgentModel(Integer id, String username, String nickname, String password, String nameagent, String address, String phone, String email, String facebook, String key, String status, Integer parentid, String namebank, String nameaccount, String numberaccount, Integer show, Integer active, Date createtime, Date updatetime, Integer order, Integer sms, Integer percent_bonus_vincard, String site, Date last_login_time, Integer login_times, Integer level, String code, String ancestors) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.nameagent = nameagent;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.facebook = facebook;
        this.key = key;
        this.status = status;
        this.parentid = parentid;
        this.namebank = namebank;
        this.nameaccount = nameaccount;
        this.numberaccount = numberaccount;
        this.show = show;
        this.active = active;
        this.createtime = createtime;
        this.updatetime = updatetime;
        this.order = order;
        this.sms = sms;
        this.percent_bonus_vincard = percent_bonus_vincard;
        this.site = site;
        this.last_login_time = last_login_time;
        this.login_times = login_times;
        this.level = level;
        this.code = code;
        this.ancestors = ancestors;
    }

    public Date getLast_login_time() {
        return this.last_login_time;
    }

    public void setLast_login_time(Date last_login_time) {
        this.last_login_time = last_login_time;
    }

    public Integer getLogin_times() {
        return this.login_times;
    }

    public void setLogin_times(Integer login_times) {
        this.login_times = login_times;
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
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

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNameagent() {
        return this.nameagent;
    }

    public void setNameagent(String nameagent) {
        this.nameagent = nameagent;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFacebook() {
        return this.facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getParentid() {
        return this.parentid;
    }

    public void setParentid(Integer parentid) {
        this.parentid = parentid;
    }

    public String getNamebank() {
        return this.namebank;
    }

    public void setNamebank(String namebank) {
        this.namebank = namebank;
    }

    public String getNameaccount() {
        return this.nameaccount;
    }

    public void setNameaccount(String nameaccount) {
        this.nameaccount = nameaccount;
    }

    public String getNumberaccount() {
        return this.numberaccount;
    }

    public void setNumberaccount(String numberaccount) {
        this.numberaccount = numberaccount;
    }

    public Integer getShow() {
        return this.show;
    }

    public void setShow(Integer show) {
        this.show = show;
    }

    public Integer getActive() {
        return this.active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public Date getCreatetime() {
        return this.createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getUpdatetime() {
        return this.updatetime;
    }

    public void setUpdatetime(Date updatetime) {
        this.updatetime = updatetime;
    }

    public Integer getOrder() {
        return this.order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getSms() {
        return this.sms;
    }

    public void setSms(Integer sms) {
        this.sms = sms;
    }

    public Integer getPercent_bonus_vincard() {
        return this.percent_bonus_vincard;
    }

    public void setPercent_bonus_vincard(Integer percent_bonus_vincard) {
        this.percent_bonus_vincard = percent_bonus_vincard;
    }

    public Double getCommission_rate() {
        return this.commission_rate;
    }

    public void setCommission_rate(Double commission_rate) {
        this.commission_rate = commission_rate;
    }

    public String getSite() {
        return this.site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getAncestors() {
        return this.ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }
}

