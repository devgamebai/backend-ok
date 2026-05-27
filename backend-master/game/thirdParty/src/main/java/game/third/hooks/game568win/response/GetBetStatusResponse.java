/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.hooks.game568win.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.third.hooks.game568win.response.BaseResponse;

public class GetBetStatusResponse
extends BaseResponse {
    private String transferCode;
    private String transactionId;
    private String status;
    private double winLoss;
    private double stake;

    public String getTransferCode() {
        return this.transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getWinLoss() {
        return this.winLoss;
    }

    public void setWinLoss(double winLoss) {
        this.winLoss = winLoss;
    }

    public double getStake() {
        return this.stake;
    }

    public void setStake(double stake) {
        this.stake = stake;
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}

