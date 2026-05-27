/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vmq.service;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceProvider
extends Remote {
    public int sendMT(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10) throws RemoteException;

    public int sendMTEx(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10) throws RemoteException;

    public int sendMTEx2(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10, int var11, int var12, int var13) throws RemoteException;

    public int sendMTPush(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10) throws RemoteException;
}

