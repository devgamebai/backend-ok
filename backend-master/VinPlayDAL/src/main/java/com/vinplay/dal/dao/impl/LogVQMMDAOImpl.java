/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.VQMMResponse
 */
package com.vinplay.dal.dao.impl;

import com.vinplay.dal.dao.LogVQMMDAO;
import com.vinplay.vbee.common.response.VQMMResponse;
import java.util.ArrayList;
import java.util.List;

public class LogVQMMDAOImpl
implements LogVQMMDAO {
    @Override
    public List<VQMMResponse> searchVQMM(String nickName, String timeStart, String timeEnd, int page) {
        ArrayList<VQMMResponse> vqmm = new ArrayList<VQMMResponse>();
        return vqmm;
    }

    @Override
    public int countSearchVQMM(String nickName, String timeStart, String timeEnd) {
        ArrayList vqmm = new ArrayList();
        return vqmm.size();
    }
}

