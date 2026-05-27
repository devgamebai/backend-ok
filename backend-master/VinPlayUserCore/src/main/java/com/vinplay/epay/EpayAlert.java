/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.epay;

import java.util.Date;

public class EpayAlert {
    public static int disconnectCashout = 0;
    public static int disconnectTopup = 0;
    public static Date alertDisconnectCashoutTime = new Date();
    public static Date alertDisconnectTopupTime = new Date();
    public static int timeoutCashout = 0;
    public static int timeoutTopup = 0;
    public static Date alertTimeoutCashoutTime = new Date();
    public static Date alertTimeoutTopupTime = new Date();
    public static Date alertOutOfMoneyTime = new Date();
}

