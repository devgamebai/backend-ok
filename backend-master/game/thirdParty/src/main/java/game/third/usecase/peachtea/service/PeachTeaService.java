/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.peachtea.service;

import game.third.usecase.peachtea.resoponse.BalanceResponse;
import game.third.usecase.peachtea.resoponse.LaunchGameResponse;

public interface PeachTeaService {
    public LaunchGameResponse launchGame(String var1, String var2, String var3);

    public String getNickNameByToken(String var1);

    public BalanceResponse getBalanceByToken(String var1);
}

