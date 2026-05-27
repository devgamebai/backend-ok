/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.gamebai.entities.PokerTourInfoGeneral
 *  com.vinplay.gamebai.entities.PokerTourState
 *  com.vinplay.gamebai.entities.PokerTourType
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors.gamebai;

import com.vinplay.api.processors.gamebai.response.PokerTourGeneralResponse;
import com.vinplay.gamebai.entities.PokerTourInfoGeneral;
import com.vinplay.gamebai.entities.PokerTourState;
import com.vinplay.gamebai.entities.PokerTourType;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class GetListPokerTourProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        PokerTourGeneralResponse response = new PokerTourGeneralResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        try {
            Integer ticket = null;
            PokerTourState state = null;
            PokerTourType type = null;
            int tk = Integer.parseInt(request.getParameter("tk"));
            int st = Integer.parseInt(request.getParameter("st"));
            int t = Integer.parseInt(request.getParameter("type"));
            int page = Integer.parseInt(request.getParameter("p"));
            int rows = Integer.parseInt(request.getParameter("s"));
            if (page > 0 && rows > 0) {
                if (tk >= 0) {
                    ticket = tk;
                }
                if (st > 0) {
                    state = PokerTourState.getById((int)st);
                }
                if (t > 0) {
                    type = PokerTourType.getById((int)t);
                }
                GameTourServiceImpl ser = new GameTourServiceImpl();
                response.setTotalPages(10);
                response.setTours(ser.getPokerTourListGeneral(ticket, state, type, page, rows));
                response.setSuccess(true);
                response.setErrorCode("0");
            }
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return response.toJson();
    }
}

