/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectWriter
 *  com.fasterxml.jackson.databind.SerializationFeature
 */
package com.vinplay.common.game3rd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ThirdPartyResponse<T> {
    private long totalRecord;
    private long totalBet;
    private long totalValidBet;
    private long totalPayout;
    private long totalPlayer;
    private T listTrans;

    public ThirdPartyResponse() {
    }

    public ThirdPartyResponse(long totalRecord, long totalBet, long totalValidBet, long totalPayout, T listTrans) {
        this.totalRecord = totalRecord;
        this.totalBet = totalBet;
        this.totalValidBet = totalValidBet;
        this.totalPayout = totalPayout;
        this.listTrans = listTrans;
    }

    public ThirdPartyResponse(long totalRecord, long totalBet, long totalValidBet, long totalPayout, long totalPlayer, T listTrans) {
        this.totalRecord = totalRecord;
        this.totalBet = totalBet;
        this.totalValidBet = totalValidBet;
        this.totalPayout = totalPayout;
        this.totalPlayer = totalPlayer;
        this.listTrans = listTrans;
    }

    public long getTotalPlayer() {
        return this.totalPlayer;
    }

    public void setTotalPlayer(long totalPlayer) {
        this.totalPlayer = totalPlayer;
    }

    public long getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(long totalRecord) {
        this.totalRecord = totalRecord;
    }

    public long getTotalBet() {
        return this.totalBet;
    }

    public void setTotalBet(long totalBet) {
        this.totalBet = totalBet;
    }

    public long getTotalValidBet() {
        return this.totalValidBet;
    }

    public void setTotalValidBet(long totalValidBet) {
        this.totalValidBet = totalValidBet;
    }

    public long getTotalPayout() {
        return this.totalPayout;
    }

    public void setTotalPayout(long totalPayout) {
        this.totalPayout = totalPayout;
    }

    public T getListTrans() {
        return this.listTrans;
    }

    public void setListTrans(T listTrans) {
        this.listTrans = listTrans;
    }

    public String toJson() {
        ObjectWriter ow = new ObjectMapper().writer();
        ow.with(SerializationFeature.INDENT_OUTPUT);
        try {
            String json = ow.writeValueAsString(this);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

