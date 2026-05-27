/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.gsc.dao;

import game.third.usecase.gsc.response.LiveCasinoUserResponse;

public interface GscUserDao {
    public boolean insertUserCasino(String var1, String var2);

    public LiveCasinoUserResponse getUserCasino(String var1);
}

