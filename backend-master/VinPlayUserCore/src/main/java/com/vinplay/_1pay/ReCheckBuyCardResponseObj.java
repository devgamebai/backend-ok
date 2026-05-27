/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay._1pay;

import com.vinplay._1pay.StatusObj;
import com.vinplay._1pay.TransactionObj;

public class ReCheckBuyCardResponseObj {
    private StatusObj status;
    private TransactionObj transaction;
    private String dataSign;

    public ReCheckBuyCardResponseObj(StatusObj status, TransactionObj transaction, String dataSign) {
        this.status = status;
        this.transaction = transaction;
        this.dataSign = dataSign;
    }

    public ReCheckBuyCardResponseObj() {
    }

    public StatusObj getStatus() {
        return this.status;
    }

    public void setStatus(StatusObj status) {
        this.status = status;
    }

    public TransactionObj getTransaction() {
        return this.transaction;
    }

    public void setTransaction(TransactionObj transaction) {
        this.transaction = transaction;
    }

    public String getDataSign() {
        return this.dataSign;
    }

    public void setDataSign(String dataSign) {
        this.dataSign = dataSign;
    }
}

