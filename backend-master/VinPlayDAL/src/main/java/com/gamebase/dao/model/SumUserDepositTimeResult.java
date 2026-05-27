/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.dao.model;

public class SumUserDepositTimeResult {
    private Long sum;
    private Long count;

    public SumUserDepositTimeResult(Long sum, Long count) {
        this.sum = sum;
        this.count = count;
    }

    public Long getSum() {
        return this.sum;
    }

    public Long getCount() {
        return this.count;
    }
}

