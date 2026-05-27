/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.agent;

import java.util.Date;

public class UserAgentLoginModel {
    private Integer id;
    private String username;
    private String nickname;
    private String nameagent;
    private String address;
    private String phone;
    private String email;
    private Integer active;
    private String site;
    private Integer level;
    private String code;
    private Date last_login_time;
    private Integer login_times;

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

    public Integer getActive() {
        return this.active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public String getSite() {
        return this.site;
    }

    public void setSite(String site) {
        this.site = site;
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
}

