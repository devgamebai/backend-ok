/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.entities;

public class LogTransferAgentModel {
    private int id;
    private String transaction_no;
    private String agent_level1;
    private String nick_name_send;
    private String nick_name_receive;
    private long money_send;
    private long money_receive;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTransaction_no() {
        return this.transaction_no;
    }

    public void setTransaction_no(String transaction_no) {
        this.transaction_no = transaction_no;
    }

    public String getAgent_level1() {
        return this.agent_level1;
    }

    public void setAgent_level1(String agent_level1) {
        this.agent_level1 = agent_level1;
    }

    public String getNick_name_send() {
        return this.nick_name_send;
    }

    public void setNick_name_send(String nick_name_send) {
        this.nick_name_send = nick_name_send;
    }

    public String getNick_name_receive() {
        return this.nick_name_receive;
    }

    public void setNick_name_receive(String nick_name_receive) {
        this.nick_name_receive = nick_name_receive;
    }

    public long getMoney_send() {
        return this.money_send;
    }

    public void setMoney_send(long money_send) {
        this.money_send = money_send;
    }

    public long getMoney_receive() {
        return this.money_receive;
    }

    public void setMoney_receive(long money_receive) {
        this.money_receive = money_receive;
    }
}

