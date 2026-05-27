package com.vinplay.vbee.common.messages;

public class DepositMessage extends BaseMessage {

    private static final long serialVersionUID = 1L;

    private long txId;
    private String txCode;
    private long userId;
    private String nickName;
    private long amount;
    private String bankName;
    private String bankNumber;
    private String holderName; // player's bank account holder name

    public DepositMessage() {
    }

    public DepositMessage(long txId, String txCode, long userId, String nickName,
            long amount, String bankName, String bankNumber) {
        this.txId = txId;
        this.txCode = txCode;
        this.userId = userId;
        this.nickName = nickName;
        this.amount = amount;
        this.bankName = bankName;
        this.bankNumber = bankNumber;
    }

    public DepositMessage(long txId, String txCode, long userId, String nickName,
            long amount, String bankName, String bankNumber, String holderName) {
        this(txId, txCode, userId, nickName, amount, bankName, bankNumber);
        this.holderName = holderName;
    }

    public long getTxId() {
        return this.txId;
    }

    public void setTxId(long txId) {
        this.txId = txId;
    }

    public String getTxCode() {
        return this.txCode;
    }

    public void setTxCode(String txCode) {
        this.txCode = txCode;
    }

    public long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getAmount() {
        return this.amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getBankName() {
        return this.bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankNumber() {
        return this.bankNumber;
    }

    public void setBankNumber(String bankNumber) {
        this.bankNumber = bankNumber;
    }

    public String getHolderName() {
        return this.holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }
}
