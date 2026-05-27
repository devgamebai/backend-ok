/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.epay.megacard;

import com.vinplay.epay.megacard.ChargeReponse;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Services
extends Remote {
    public ChargeReponse cardCharging(String var1, String var2, String var3, String var4, String var5, String var6, String var7) throws RemoteException;
}

