/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.NganLuongFollowFaceValue
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.DoiSoatNganLuongDAOImpl;
import com.vinplay.dal.service.DoiSoatNganLuongService;
import com.vinplay.vbee.common.response.NganLuongFollowFaceValue;
import java.util.List;

public class DoiSoatNganLuongServiceImpl
implements DoiSoatNganLuongService {
    @Override
    public List<NganLuongFollowFaceValue> getDoiSoatData(String timeStart, String timeEnd) {
        DoiSoatNganLuongDAOImpl dao = new DoiSoatNganLuongDAOImpl();
        return dao.getDoiSoatData(timeStart, timeEnd);
    }
}

