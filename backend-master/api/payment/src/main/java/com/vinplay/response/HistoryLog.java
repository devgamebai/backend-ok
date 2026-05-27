/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.response;

public class HistoryLog {
    private String transactionId;
    private long amount;
    private String type = "";
    private String providerName;
    private String description;
    private String createdAt;
    private Status status = Status.UNKNOWN;

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public long getAmount() {
        return this.amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProviderName() {
        return this.providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static enum Status {
        PENDING(0),
        SUCCESS(1),
        FAILED(2),
        CANCEL(3),
        UNKNOWN(4);

        private final int value;

        private Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
}

