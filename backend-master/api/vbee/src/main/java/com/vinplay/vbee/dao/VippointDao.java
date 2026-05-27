/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.vippoint.VippointEventMessage
 */
package com.vinplay.vbee.dao;

import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import java.sql.SQLException;

public interface VippointDao {
    public boolean updateVippointEvent(VippointEventMessage var1, int var2) throws SQLException;
}

