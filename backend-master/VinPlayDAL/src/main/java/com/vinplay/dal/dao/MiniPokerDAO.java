/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.minipoker.LSGDMiniPoker;
import com.vinplay.dal.entities.minipoker.VinhDanhMiniPoker;
import java.util.List;

public interface MiniPokerDAO {
    public int countLichSuGiaoDich(String var1, int var2);

    public List<LSGDMiniPoker> getLichSuGiaoDich(String var1, int var2, int var3);

    public List<VinhDanhMiniPoker> getBangVinhDanh(int var1, int var2);
}

