/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.service.impl.SlotMachineServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.processors.slot;

import com.vinplay.api.processors.minigame.response.LSGDPokeGoResponse;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LSGDSlotProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private Logger logger = Logger.getLogger((String)"api");

    public String execute(Param<HttpServletRequest> param) {
        LSGDPokeGoResponse response = new LSGDPokeGoResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest)param.get();
        String username = request.getParameter("un");
        int page = Integer.parseInt(request.getParameter("p"));
        String gameName = Games.KHO_BAU.getName();
        if (request.getParameter("gn") != null) {
            String gnParam = request.getParameter("gn");
            // Normalize: try enum constant name first (e.g. "CHIEMTINH" → Games.CHIEMTINH),
            // then fall back to getName() lookup (e.g. "ChiemTinh"), then raw value
            try {
                gameName = Games.valueOf(gnParam).getName();
            } catch (IllegalArgumentException e) {
                Games g = Games.findGameByName(gnParam);
                gameName = (g != null) ? g.getName() : gnParam;
            }
        }
        SlotMachineServiceImpl service = new SlotMachineServiceImpl();
        try {
            List results = service.getLSGD(gameName, username, page);
            int pageSize = gameName.equalsIgnoreCase(Games.KHO_BAU.getName()) ? 5 : 10;
            int totalPages = 1;
            try {
                com.mongodb.client.MongoDatabase db = com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory.getDB();
                long totalDocs = db.getCollection("log_" + gameName)
                    .countDocuments(new org.bson.Document("user_name", username));
                totalPages = Math.max(1, (int) Math.ceil((double) totalDocs / pageSize));
            } catch (Exception countEx) {
                totalPages = results.isEmpty() ? 0 : 100;
            }
            response.setTotalPages(totalPages);
            response.setResults(results);
            response.setSuccess(true);
            response.setErrorCode("0");
        }
        catch (Exception e) {
            this.logger.error("LSGD Slot Error: ", (Throwable)e);
            return e.getMessage();
        }
        return response.toJson();
    }
}

