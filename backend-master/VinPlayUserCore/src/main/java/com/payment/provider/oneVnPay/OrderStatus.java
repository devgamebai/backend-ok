/*
 * Decompiled with CFR 0.152.
 */
package com.payment.provider.oneVnPay;

public enum OrderStatus {
    SUCCESS(1),
    FAILURE(2),
    ABNORMAL_TRANSACTION(3),
    INSUFFICIENT_BALANCE(4),
    REJECT(5);

    private int status;

    private OrderStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public static OrderStatus fromStatus(int status) {
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.getStatus() != status) continue;
            return orderStatus;
        }
        return null;
    }
}

