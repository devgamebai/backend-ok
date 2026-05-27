/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.brandname.service;

import com.vinplay.brandname.enties.DataSms;
import com.vinplay.brandname.enties.ListPhone;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAPISMS
extends Remote {
    public String pushMsg2Phone(String var1, String var2, String var3, String var4, String var5) throws RemoteException;

    public String pushMsg2PhoneDLVR(String var1, String var2, String var3, String var4, Integer var5, String var6, String var7) throws RemoteException;

    public String pushMsgAdvDirect(String var1, String var2, String var3, String var4, String var5) throws RemoteException;

    public String sendOTP(String var1, String var2, String var3, String var4, String var5) throws RemoteException;

    public String pushMsg2ListPhone(String var1, String var2, String[] var3, String var4, String var5) throws RemoteException;

    public String pushMsg2ListPhoneDLVR(String var1, String var2, ListPhone[] var3, Integer var4, String var5, String var6) throws RemoteException;

    public String pushBulkSms(String var1, DataSms[] var2, String var3, String var4) throws RemoteException;

    public String getTelco(String var1, String var2, String var3) throws RemoteException;
}

