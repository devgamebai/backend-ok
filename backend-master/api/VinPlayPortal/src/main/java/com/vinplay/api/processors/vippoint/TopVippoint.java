/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.VippointDaoImpl
 *  com.vinplay.usercore.utils.VippointUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  com.vinplay.vippoint.entiies.EventVPMapModel
 *  com.vinplay.vippoint.entiies.EventVPTopIntelModel
 *  com.vinplay.vippoint.entiies.EventVPTopStrongModel
 *  com.vinplay.vippoint.entiies.EventVPTopVipModel
 *  org.json.JSONException
 */
package com.vinplay.api.processors.vippoint;

import com.vinplay.api.processors.vippoint.TopEventVippointTask;
import com.vinplay.usercore.dao.impl.VippointDaoImpl;
import com.vinplay.usercore.utils.VippointUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.vippoint.entiies.EventVPMapModel;
import com.vinplay.vippoint.entiies.EventVPTopIntelModel;
import com.vinplay.vippoint.entiies.EventVPTopStrongModel;
import com.vinplay.vippoint.entiies.EventVPTopVipModel;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONException;

public class TopVippoint {
    public static List<EventVPMapModel> maps;
    public static List<EventVPTopIntelModel> intel;
    public static List<EventVPTopStrongModel> strong;
    public static List<EventVPTopVipModel> vips;

    public static void init() throws SQLException, JSONException, ParseException {
        VippointUtils.init();
        TopVippoint.getTop();
        Timer timer = new Timer();
        timer.schedule((TimerTask)new TopEventVippointTask(), 0L, 60000L);
    }

    public static void getTop() throws SQLException {
        VippointDaoImpl dao = new VippointDaoImpl();
        maps = dao.getEventMaps();
        intel = dao.getEventIntel();
        strong = dao.getEventStrong();
        vips = dao.getEventVips();
    }

    public static List<EventVPTopIntelModel> getIntel(String platform) {
        if (VinPlayUtils.isMobile((String)platform)) {
            ArrayList<EventVPTopIntelModel> res = new ArrayList<EventVPTopIntelModel>();
            for (EventVPTopIntelModel model : intel) {
                EventVPTopIntelModel modelTmp = new EventVPTopIntelModel(model.getStt(), model.getNickname(), model.getPlace(), model.getVippoint(), model.getBonus(), model.getPrizeVin(), "");
                res.add(modelTmp);
            }
            return res;
        }
        return intel;
    }

    public static List<EventVPTopStrongModel> getStrong(String platform) {
        if (VinPlayUtils.isMobile((String)platform)) {
            ArrayList<EventVPTopStrongModel> res = new ArrayList<EventVPTopStrongModel>();
            for (EventVPTopStrongModel model : strong) {
                EventVPTopStrongModel modelTmp = new EventVPTopStrongModel(model.getStt(), model.getNickname(), model.getVippointSub(), model.getCount(), model.getPlace(), model.getPrizeVin(), "");
                res.add(modelTmp);
            }
            return res;
        }
        return strong;
    }
}

