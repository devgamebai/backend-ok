/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.caothap.LSGDCaoThap;
import com.vinplay.dal.entities.caothap.TopCaoThap;
import com.vinplay.dal.entities.caothap.VinhDanhCaoThap;
import java.sql.SQLException;
import java.util.List;

public interface CaoThapDAO {
    public long[] getPotCaoThap(String var1) throws SQLException;

    public long[] getFundCaoThap(String var1) throws SQLException;

    public int countLichSuGiaoDich(String var1, int var2);

    public List<LSGDCaoThap> getLichSuGiaoDich(String var1, int var2, int var3);

    public int countVinhDanh(int var1);

    public List<VinhDanhCaoThap> getBangVinhDanh(int var1, int var2);

    public List<TopCaoThap> getTop(String var1, String var2);

    public long getLastReferenceId();
}

