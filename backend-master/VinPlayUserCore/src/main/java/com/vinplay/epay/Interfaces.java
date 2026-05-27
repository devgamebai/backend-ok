/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.epay;

import com.vinplay.epay.CheckOrdesrCDVResult;
import com.vinplay.epay.CheckTransResult;
import com.vinplay.epay.DownloadSoftpinResult;
import com.vinplay.epay.PaymentCdvResult;
import com.vinplay.epay.QueryBalanceResult;
import com.vinplay.epay.TopupResult;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Interfaces
extends Remote {
    public TopupResult topup(String var1, String var2, String var3, String var4, int var5, String var6) throws RemoteException;

    public int echo() throws RemoteException;

    public int checkStore(String var1, String var2, int var3, String var4) throws RemoteException;

    public PaymentCdvResult paymentCDV(String var1, String var2, String var3, int var4, String var5, long var6, int var8, String var9) throws RemoteException;

    public DownloadSoftpinResult downloadSoftpin(String var1, String var2, String var3, int var4, int var5, String var6) throws RemoteException;

    public CheckTransResult checkTrans(String var1, String var2, int var3, String var4) throws RemoteException;

    public QueryBalanceResult queryBalance(String var1, String var2) throws RemoteException;

    public CheckOrdesrCDVResult checkOrdersCDV(String var1, String var2, String var3) throws RemoteException;

    public DownloadSoftpinResult reDownloadSoftpin(String var1, String var2, String var3) throws RemoteException;
}

