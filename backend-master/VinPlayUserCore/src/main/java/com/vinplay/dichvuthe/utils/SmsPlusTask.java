/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.utils;

import com.vinplay.dichvuthe.utils.SMSUtils;
import java.util.TimerTask;

public class SmsPlusTask
extends TimerTask {
    @Override
    public void run() {
        try {
            SMSUtils.queryTransactionPending();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

