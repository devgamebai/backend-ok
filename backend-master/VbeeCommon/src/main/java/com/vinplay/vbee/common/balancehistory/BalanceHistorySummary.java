package com.vinplay.vbee.common.balancehistory;

import org.json.JSONObject;

public class BalanceHistorySummary {
    public long totalNap;
    public long totalRut;
    public long totalCashback;
    public long totalTransferIn;
    public long totalTransferOut;
    public long totalGameBet;
    public long totalGameWin;
    public long totalGiftcode;
    public long totalAdminAdjust;
    public long totalOther;

    public long netChange() {
        return totalNap + totalRut + totalCashback
             + totalTransferIn + totalTransferOut
             + totalGameBet + totalGameWin
             + totalGiftcode + totalAdminAdjust + totalOther;
    }

    public void addTo(BalanceHistoryCategory c, long delta) {
        switch (c) {
            case NAP:          totalNap          += delta; break;
            case RUT:          totalRut          += delta; break;
            case CASHBACK:     totalCashback     += delta; break;
            case TRANSFER_IN:  totalTransferIn   += delta; break;
            case TRANSFER_OUT: totalTransferOut  += delta; break;
            case GAME_BET:     totalGameBet      += delta; break;
            case GAME_WIN:     totalGameWin      += delta; break;
            case GIFTCODE:     totalGiftcode     += delta; break;
            case ADMIN_ADJUST: totalAdminAdjust  += delta; break;
            case OTHER:        totalOther        += delta; break;
        }
    }

    public JSONObject toJson() {
        JSONObject j = new JSONObject();
        j.put("total_nap", totalNap);
        j.put("total_rut", totalRut);
        j.put("total_cashback", totalCashback);
        j.put("total_transfer_in", totalTransferIn);
        j.put("total_transfer_out", totalTransferOut);
        j.put("total_game_bet", totalGameBet);
        j.put("total_game_win", totalGameWin);
        j.put("total_giftcode", totalGiftcode);
        j.put("total_admin_adjust", totalAdminAdjust);
        j.put("total_other", totalOther);
        j.put("net_change", netChange());
        return j;
    }
}
