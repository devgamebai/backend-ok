/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.json.JSONException
 */
package com.vinplay.usercore.service;

import com.vinplay.gamebai.entities.BossXocDiaModel;
import com.vinplay.gamebai.entities.XocDiaBoss;
import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.xocdia.TopJackpotXocDia;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.json.JSONException;

public interface XocDiaService {
    public boolean saveRoomBoss(XocDiaBoss var1) throws SQLException;

    public boolean updateRoomBoss(String var1, String var2, long var3, int var5, long var6, int var8) throws SQLException;

    public XocDiaBoss getRoomBoss(String var1, int var2) throws SQLException;

    public Map<Integer, XocDiaBoss> getListRoomBossActive() throws SQLException;

    public List<String> getListBossActive() throws SQLException;

    public List<String> getListSessionActive() throws SQLException;

    public List<BossXocDiaModel> getListRoomBoss(String var1, int var2, int var3, int var4) throws SQLException, JSONException;

    public List<TopWin> getTopAward() throws SQLException;

    public List<TopJackpotXocDia> getHistoryJackpotXocDia() throws SQLException;

    public List<TopWin> getTopJackpotAward(String var1) throws SQLException;

    void setKetQuaXocDia(short[] ketQua);
    short[] getKetQuaXocDia();
    short[] suaKetQuaXocDia();
}

