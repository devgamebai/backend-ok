/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.minigame.LogRutLocMessge
 *  com.vinplay.vbee.common.messages.minigame.LogTanLocMessage
 *  com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.ThanhDuMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage
 *  com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateFundMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage
 *  com.vinplay.vbee.common.messages.minigame.UpdatePotMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.minigame.LogRutLocMessge;
import com.vinplay.vbee.common.messages.minigame.LogTanLocMessage;
import com.vinplay.vbee.common.messages.minigame.ResultTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.ThanhDuMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuDetailMessage;
import com.vinplay.vbee.common.messages.minigame.TransactionTaiXiuMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateFundMessage;
import com.vinplay.vbee.common.messages.minigame.UpdateLuotRutLocMessage;
import com.vinplay.vbee.common.messages.minigame.UpdatePotMessage;
import java.sql.SQLException;

public interface TaiXiuDao {
    public boolean saveResultTaiXiu(ResultTaiXiuMessage var1) throws SQLException;

    public boolean saveTransactionTaiXiu(TransactionTaiXiuMessage var1) throws SQLException;

    public boolean saveTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage var1) throws SQLException;

    public boolean updateTransactionTaiXiuDetail(TransactionTaiXiuDetailMessage var1) throws SQLException;

    public boolean updateThanhDu(ThanhDuMessage var1) throws SQLException;

    public boolean updatePot(UpdatePotMessage var1) throws SQLException;

    public boolean updateFund(UpdateFundMessage var1) throws SQLException;

    public void logTanLoc(LogTanLocMessage var1);

    public void logRutLoc(LogRutLocMessge var1);

    public boolean updateLuotRutLoc(UpdateLuotRutLocMessage var1) throws SQLException;
}

