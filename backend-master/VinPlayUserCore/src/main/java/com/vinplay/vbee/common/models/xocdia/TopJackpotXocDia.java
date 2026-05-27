/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models.xocdia;

public class TopJackpotXocDia {
    private String createdAt;
    private Integer dice;
    private Long accumulate;
    private String matchId;

    public TopJackpotXocDia(String createdAt, Integer dice, Long accumulate, String matchId) {
        this.createdAt = createdAt;
        this.dice = dice;
        this.accumulate = accumulate;
        this.matchId = matchId;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getDice() {
        return this.dice;
    }

    public void setDice(Integer dice) {
        this.dice = dice;
    }

    public Long getAccumulate() {
        return this.accumulate;
    }

    public void setAccumulate(Long accumulate) {
        this.accumulate = accumulate;
    }

    public String getMatchId() {
        return this.matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
}

