/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.livecasino.api.wsclient;

import com.vinplay.livecasino.api.core.obj.BalanceResponse;
import com.vinplay.livecasino.api.core.obj.LaunchGameResponse;
import com.vinplay.livecasino.api.core.obj.TCGBaseResponse;
import com.vinplay.livecasino.api.wsclient.exception.ProcessException;
import com.vinplay.livecasino.api.wsclient.exception.RemoteException;
import com.vinplay.livecasino.api.wsclient.exception.TransportException;

public interface TCGamingAPICommon {
    public TCGBaseResponse registerMember(String var1, String var2, String var3) throws RemoteException, ProcessException, TransportException;

    public BalanceResponse getBalanceMember(String var1, int var2) throws RemoteException, ProcessException, TransportException;

    public TCGBaseResponse fundTransferIn(String var1, int var2, double var3, String var5) throws RemoteException, ProcessException, TransportException;

    public TCGBaseResponse fundTransferOutAll(String var1, int var2, String var3) throws RemoteException, ProcessException, TransportException;

    public LaunchGameResponse launchGame(String var1, int var2, String var3, String var4, String var5) throws RemoteException, ProcessException, TransportException;
}

