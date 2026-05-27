/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.fish;

public class FishTransaction {
    private String orderId;
    private String prefix;
    private String nickname;
    private String param;
    private Long timeStamp;
    private String action;
    private Long money;
    private String key;
    private String status;
    private String urlApi;

    public FishTransaction() {
    }

    public FishTransaction(String orderId, String prefix, String nickname, String param, Long timeStamp, String action, Long money, String key, String status, String urlApi) {
        this.orderId = orderId;
        this.prefix = prefix;
        this.nickname = nickname;
        this.param = param;
        this.timeStamp = timeStamp;
        this.action = action;
        this.money = money;
        this.key = key;
        this.status = status;
        this.urlApi = urlApi;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getParam() {
        return this.param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public Long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getMoney() {
        return this.money;
    }

    public void setMoney(Long money) {
        this.money = money;
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

    public String getUrlApi() {
        return this.urlApi;
    }

    public void setUrlApi(String urlApi) {
        this.urlApi = urlApi;
    }
}

