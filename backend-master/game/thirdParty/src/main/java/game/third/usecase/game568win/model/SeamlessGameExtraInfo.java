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

public class SeamlessGameExtraInfo {
    @JsonProperty(value="FeatureBuyStatus")
    private int featureBuyStatus;
    @JsonProperty(value="EndRoundStatus")
    private int endRoundStatus;

    public static SeamlessGameExtraInfo fromJson(String seamlessGameExtraInfo) {
        if (seamlessGameExtraInfo == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return (SeamlessGameExtraInfo)objectMapper.readValue(seamlessGameExtraInfo, SeamlessGameExtraInfo.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getFeatureBuyStatus() {
        return this.featureBuyStatus;
    }

    public void setFeatureBuyStatus(int featureBuyStatus) {
        this.featureBuyStatus = featureBuyStatus;
    }

    public int getEndRoundStatus() {
        return this.endRoundStatus;
    }

    public void setEndRoundStatus(int endRoundStatus) {
        this.endRoundStatus = endRoundStatus;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}

