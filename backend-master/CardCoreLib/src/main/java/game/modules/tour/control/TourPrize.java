/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.tour.control;

import bitzero.util.common.business.CommonHandle;
import game.modules.tour.control.PrizeRate;
import game.modules.tour.control.config.TourConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TourPrize {
    public static int getPrize(PrizeRate prizeRate, int rank, long prizePool) {
        if (prizeRate != null && rank > 0 && prizeRate.rate.length > rank) {
            double rate = prizeRate.rate[rank];
            return (int)(rate * (double)prizePool / 100.0);
        }
        return 0;
    }

    public static PrizeRate findPrizeRate(int numberOfPlayers) {
        try {
            JSONArray prizeRateJsonArray = TourConfig.instance().config.getJSONArray("prizeRates");
            for (int i = 0; i < prizeRateJsonArray.length(); ++i) {
                JSONObject prizeRateJson = prizeRateJsonArray.getJSONObject(i);
                int minPlayers = prizeRateJson.getJSONArray("players").getInt(0);
                int maxPlayers = prizeRateJson.getJSONArray("players").getInt(1);
                if (numberOfPlayers < minPlayers || numberOfPlayers > maxPlayers) continue;
                PrizeRate prizeRate = new PrizeRate();
                prizeRate.init(prizeRateJson);
                return prizeRate;
            }
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        return null;
    }
}

