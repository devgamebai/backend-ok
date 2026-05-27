/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtc.tempuri;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GoodsPaygateSoap
extends Remote {
    public String requestTransaction(String var1, String var2, String var3, String var4) throws RemoteException;
}

