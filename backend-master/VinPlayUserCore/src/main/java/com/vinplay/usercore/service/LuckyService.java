/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.LuckyResponse
 *  com.vinplay.vbee.common.response.LuckyVipResponse
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.vqmm.LuckyHistory;
import com.vinplay.usercore.entities.vqmm.LuckyVipHistory;
import com.vinplay.vbee.common.response.LuckyResponse;
import com.vinplay.vbee.common.response.LuckyVipResponse;
import java.sql.SQLException;
import java.util.List;

public interface LuckyService {
    public int receiveRotateDaily(int var1, String var2) throws SQLException;

    public LuckyResponse getResultLuckyRotation(String var1, String var2);

    public List<LuckyHistory> getLuckyHistory(String var1, int var2);

    public List<LuckyVipHistory> getLuckyVipHistory(String var1, int var2);

    public LuckyVipResponse rotateLuckyVip(String var1, boolean var2);
}

