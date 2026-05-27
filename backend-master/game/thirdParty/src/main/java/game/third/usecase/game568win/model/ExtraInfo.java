/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.usecase.game568win.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class ExtraInfo {
    @JsonProperty(value="sportType")
    private String sportType;
    @JsonProperty(value="marketType")
    private String marketType;
    @JsonProperty(value="league")
    private String league;
    @JsonProperty(value="match")
    private String match;
    @JsonProperty(value="betOption")
    private String betOption;
    @JsonProperty(value="kickoffTime")
    private String kickOffTime;
    @JsonProperty(value="isHalfWonLose")
    private boolean isHalfWonLose;

    public static ExtraInfo fromJson(String extraInfo) {
        if (extraInfo == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (ExtraInfo)mapper.readValue(extraInfo, ExtraInfo.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getSportType() {
        return this.sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

    public String getMarketType() {
        return this.marketType;
    }

    public void setMarketType(String marketType) {
        this.marketType = marketType;
    }

    public String getLeague() {
        return this.league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getMatch() {
        return this.match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getBetOption() {
        return this.betOption;
    }

    public void setBetOption(String betOption) {
        this.betOption = betOption;
    }

    public String getKickOffTime() {
        return this.kickOffTime;
    }

    public void setKickOffTime(String kickOffTime) {
        this.kickOffTime = kickOffTime;
    }

    public boolean isHalfWonLose() {
        return this.isHalfWonLose;
    }

    public void setHalfWonLose(boolean halfWonLose) {
        this.isHalfWonLose = halfWonLose;
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}

