/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.livecasino.api.dao;

import com.vinplay.livecasino.api.response.LiveCasinoUserResponse;

public interface LiveCasinoService {
    public boolean insertUserCasino(String var1, String var2);

    public LiveCasinoUserResponse getUserCasino(String var1);
}

