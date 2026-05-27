/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoonEventResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.event.EventModel;
import com.vinplay.dal.entities.event.MoonEventModel;
import com.vinplay.vbee.common.response.MoonEventResponse;
import java.sql.SQLException;
import java.util.List;

public interface EventDAO {
    public long countlistEvent(String var1, Long var2, int var3, String var4, String var5);

    public List<EventModel> listEvent(String var1, Long var2, int var3, String var4, String var5, int var6, int var7);

    public Boolean addNewEvent(String var1, String var2, Long var3, String var4);

    public int addNewEventByAgent(String var1, String var2, Long var3, String var4, String var5);

    public EventModel eventDetail(Integer var1);

    public Boolean updateEventById(Integer var1, String var2, String var3, Long var4, String var5);

    public Boolean updateEventByName(String var1, String var2, Long var3, String var4);

    public Boolean deleteEvent(Integer var1);

    public Boolean deleteEvent(String var1);

    public List<MoonEventResponse> getListEventsMoon();

    public MoonEventModel buyPackMoon(String var1, int var2) throws SQLException;
}

