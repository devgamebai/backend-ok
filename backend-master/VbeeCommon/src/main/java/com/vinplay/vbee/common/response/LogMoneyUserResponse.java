/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.response;

import java.io.Serializable;

public class LogMoneyUserResponse
implements Serializable {
    private static final long serialVersionUID = 1L;
    public long transId;
    public String serviceName;
    public String description;
    public long currentMoney;
    public long moneyExchange;
    public String transactionTime;
}

