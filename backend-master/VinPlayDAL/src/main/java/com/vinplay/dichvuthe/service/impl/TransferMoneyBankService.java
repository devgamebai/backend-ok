/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dichvuthe.service.impl;

import com.vinplay.dichvuthe.entities.TransferMoneyBankModel;
import com.vinplay.dichvuthe.service.impl.CashOutServiceImpl;
import java.util.TimerTask;

public class TransferMoneyBankService
extends TimerTask {
    private TransferMoneyBankModel model;

    public TransferMoneyBankService(TransferMoneyBankModel model) {
        this.model = model;
    }

    @Override
    public void run() {
        CashOutServiceImpl service = new CashOutServiceImpl();
    }
}

