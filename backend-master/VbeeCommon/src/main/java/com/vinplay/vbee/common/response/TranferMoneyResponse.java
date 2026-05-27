/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

public class TranferMoneyResponse {
    public String nick_send;
    public String nick_receive;
    public long money_send;
    public long money_receive;
    public long fee;
    public int status;
    public String trans_time;
    public String des_send;
    public String des_receive;
    public String transaction_no;
    public int is_freeze_money;
    public String agent_level1;

    public TranferMoneyResponse() {
    }

    public TranferMoneyResponse(String nickSend, String nickReceive, long moneySend, long moneyReceive, long fee, int status, String transTime, String desSend, String desReceive, String transactionNo, int isFreezeMoney, String agent_level1) {
        this.nick_send = nickSend;
        this.nick_receive = nickReceive;
        this.money_send = moneySend;
        this.money_receive = moneyReceive;
        this.fee = fee;
        this.status = status;
        this.trans_time = transTime;
        this.des_send = desSend;
        this.des_receive = desReceive;
        this.transaction_no = transactionNo;
        this.is_freeze_money = isFreezeMoney;
        this.agent_level1 = agent_level1;
    }

    public String getAgent_level1() {
        return this.agent_level1;
    }

    public void setAgent_level1(String agent_level1) {
        this.agent_level1 = agent_level1;
    }

    public String getDes_send() {
        return this.des_send;
    }

    public void setDes_send(String des_send) {
        this.des_send = des_send;
    }

    public String getDes_receive() {
        return this.des_receive;
    }

    public void setDes_receive(String des_receive) {
        this.des_receive = des_receive;
    }

    public String getTransaction_no() {
        return this.transaction_no;
    }

    public void setTransaction_no(String transaction_no) {
        this.transaction_no = transaction_no;
    }

    public int getIs_freeze_money() {
        return this.is_freeze_money;
    }

    public void setIs_freeze_money(int is_freeze_money) {
        this.is_freeze_money = is_freeze_money;
    }

    public String getNick_send() {
        return this.nick_send;
    }

    public void setNick_send(String nick_send) {
        this.nick_send = nick_send;
    }

    public String getNick_receive() {
        return this.nick_receive;
    }

    public void setNick_receive(String nick_receive) {
        this.nick_receive = nick_receive;
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

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getTrans_time() {
        return this.trans_time;
    }

    public void setTrans_time(String trans_time) {
        this.trans_time = trans_time;
    }
}

