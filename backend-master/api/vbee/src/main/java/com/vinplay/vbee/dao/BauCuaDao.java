/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.baucua.ResultBauCuaMsg
 *  com.vinplay.vbee.common.messages.minigame.baucua.ToiChonCaMsg
 *  com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaDetailMsg
 *  com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaMsg
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.minigame.baucua.ResultBauCuaMsg;
import com.vinplay.vbee.common.messages.minigame.baucua.ToiChonCaMsg;
import com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaDetailMsg;
import com.vinplay.vbee.common.messages.minigame.baucua.TransactionBauCuaMsg;

public interface BauCuaDao {
    public void saveTransactionBauCua(TransactionBauCuaMsg var1);

    public void saveTransactionBauCuaDetail(TransactionBauCuaDetailMsg var1);

    public void saveResultBauCua(ResultBauCuaMsg var1);

    public void updateToiChonCa(ToiChonCaMsg var1);
}

