/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.service;

import game.third.usecase.core.obj.BalanceResponse;
import game.third.usecase.core.obj.LaunchGameResponse;
import game.third.usecase.core.obj.TCGBaseResponse;
import game.third.usecase.service.exception.ProcessException;
import game.third.usecase.service.exception.RemoteException;
import game.third.usecase.service.exception.TransportException;

public interface TCGamingAPICommon {
    public TCGBaseResponse registerMember(String var1, String var2, String var3) throws RemoteException, ProcessException, TransportException;

    public BalanceResponse getBalanceMember(String var1, int var2) throws RemoteException, ProcessException, TransportException;

    public TCGBaseResponse fundTransferIn(String var1, int var2, double var3, String var5) throws RemoteException, ProcessException, TransportException;

    public TCGBaseResponse fundTransferOutAll(String var1, int var2, String var3) throws RemoteException, ProcessException, TransportException;

    public LaunchGameResponse launchGame(String var1, int var2, String var3, String var4, String var5) throws RemoteException, ProcessException, TransportException;
}

