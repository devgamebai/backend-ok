/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.TopTourModel
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.gamebai;

import com.vinplay.api.processors.gamebai.response.TopGameTourResponse;
import com.vinplay.gamebai.entities.TopTourModel;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.Calendar;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TopGameTourProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        TopGameTourResponse res = new TopGameTourResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String gamename = request.getParameter("gn");
        String type = request.getParameter("type");
        String date = request.getParameter("date");
        String week = request.getParameter("week");
        String month = request.getParameter("month");
        String year = request.getParameter("year");
        try {
            if (type != null && year != null && gamename != null && !gamename.isEmpty()) {
                String s;
                Calendar st = Calendar.getInstance();
                Calendar et = Calendar.getInstance();
                int iDate = 0;
                int iWeek = 0;
                int iMonth = 0;
                int iYear = 0;
                int maxTour = 0;
                int size = 0;
                switch (s = type) {
                    case "1": {
                        iDate = Integer.parseInt(date);
                        iMonth = Integer.parseInt(month) - 1;
                        iYear = Integer.parseInt(year);
                        st.set(5, iDate);
                        st.set(2, iMonth);
                        st.set(1, iYear);
                        et.set(5, iDate);
                        et.set(2, iMonth);
                        et.set(1, iYear);
                        maxTour = 3;
                        size = 100;
                        break;
                    }
                    case "2": {
                        iWeek = Integer.parseInt(week);
                        iYear = Integer.parseInt(year);
                        st.set(3, iWeek);
                        st.set(1, iYear);
                        st.set(7, 1);
                        et.set(3, iWeek);
                        et.set(1, iYear);
                        et.set(7, 7);
                        maxTour = 10;
                        size = 100;
                        break;
                    }
                    case "3": {
                        iMonth = Integer.parseInt(month) - 1;
                        iYear = Integer.parseInt(year);
                        st.set(2, iMonth);
                        st.set(1, iYear);
                        st.set(5, st.getActualMinimum(5));
                        et.set(2, iMonth);
                        et.set(1, iYear);
                        et.set(5, et.getActualMaximum(5));
                        maxTour = 20;
                        size = 100;
                        break;
                    }
                    case "4": {
                        iYear = Integer.parseInt(year);
                        st.set(1, iYear);
                        st.set(2, st.getActualMinimum(2));
                        st.set(6, st.getActualMinimum(6));
                        et.set(1, iYear);
                        et.set(2, et.getActualMaximum(2));
                        et.set(6, et.getActualMaximum(6));
                        maxTour = 100;
                        size = 1000;
                        break;
                    }
                }
                st.set(11, 0);
                st.set(12, 0);
                st.set(13, 0);
                et.set(11, 23);
                et.set(12, 59);
                et.set(13, 59);
                GameTourServiceImpl ser = new GameTourServiceImpl();
                List topTours = ser.getTop(gamename, st, et, maxTour, size);
                res.setTops(topTours);
                res.setSuccess(true);
                res.setErrorCode("0");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res.toJson();
    }
}

