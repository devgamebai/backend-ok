package com.vinplay.vbee.common.models.minigame;

import java.util.List;

public class HistorySicbo {
    private List<HistorySicboDetails> bets;
    private int[] gameSessionResult;
    private long gameSessionId;
    private String createDate;

    public List<HistorySicboDetails> getBets() {
        return this.bets;
    }

    public void setBets(List<HistorySicboDetails> bets) {
        this.bets = bets;
    }

    public int[] getGameSessionResult() {
        return this.gameSessionResult;
    }

    public void setGameSessionResult(int[] gameSessionResult) {
        this.gameSessionResult = gameSessionResult;
    }

    public long getGameSessionId() {
        return this.gameSessionId;
    }

    public void setGameSessionId(long gameSessionId) {
        this.gameSessionId = gameSessionId;
    }

    public String getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
}
