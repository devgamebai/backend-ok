/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.vbee.common.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vbee.common.models.ServerConfig;
import java.util.List;

public class AppConfigResponse {
    private String version;
    private int update;
    private String urlUpdate;
    private String messageUpdate;
    private ServerConfig minigame;
    private ServerConfig sam;
    private ServerConfig bacay;
    private ServerConfig binh;
    private ServerConfig tlmn;
    private ServerConfig tala;
    private ServerConfig lieng;
    private ServerConfig xito;
    private ServerConfig baicao;
    private ServerConfig xocxoc;
    private ServerConfig poker;
    private String phone;
    private String facebook;
    private List<String> urlHelp;

    public String getMessageUpdate() {
        return this.messageUpdate;
    }

    public void setMessageUpdate(String messageUpdate) {
        this.messageUpdate = messageUpdate;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException mapper) {
            return "{\"success\":false,\"errorCode\":\"1001\"}";
        }
    }

    public AppConfigResponse(String version, int update, String urlUpdate, String messageUpdate, ServerConfig minigame, ServerConfig sam, ServerConfig bacay, ServerConfig binh, ServerConfig tlmn, ServerConfig tala, ServerConfig lieng, ServerConfig xito, ServerConfig baicao, ServerConfig xocxoc, ServerConfig poker, String phone, String facebook, List<String> urlHelp) {
        this.version = version;
        this.update = update;
        this.urlUpdate = urlUpdate;
        this.messageUpdate = messageUpdate;
        this.minigame = minigame;
        this.sam = sam;
        this.bacay = bacay;
        this.binh = binh;
        this.tlmn = tlmn;
        this.tala = tala;
        this.lieng = lieng;
        this.xito = xito;
        this.baicao = baicao;
        this.xocxoc = xocxoc;
        this.poker = poker;
        this.phone = phone;
        this.facebook = facebook;
        this.urlHelp = urlHelp;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFacebook() {
        return this.facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public List<String> getUrlHelp() {
        return this.urlHelp;
    }

    public void setUrlHelp(List<String> urlHelp) {
        this.urlHelp = urlHelp;
    }

    public ServerConfig getTlmn() {
        return this.tlmn;
    }

    public void setTlmn(ServerConfig tlmn) {
        this.tlmn = tlmn;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getUpdate() {
        return this.update;
    }

    public void setUpdate(int update) {
        this.update = update;
    }

    public String getUrlUpdate() {
        return this.urlUpdate;
    }

    public void setUrlUpdate(String urlUpdate) {
        this.urlUpdate = urlUpdate;
    }

    public ServerConfig getMinigame() {
        return this.minigame;
    }

    public void setMinigame(ServerConfig minigame) {
        this.minigame = minigame;
    }

    public ServerConfig getTala() {
        return this.tala;
    }

    public void setTala(ServerConfig tala) {
        this.tala = tala;
    }

    public ServerConfig getLieng() {
        return this.lieng;
    }

    public void setLieng(ServerConfig lieng) {
        this.lieng = lieng;
    }

    public ServerConfig getXito() {
        return this.xito;
    }

    public void setXito(ServerConfig xito) {
        this.xito = xito;
    }

    public ServerConfig getBaicao() {
        return this.baicao;
    }

    public void setBaicao(ServerConfig baicao) {
        this.baicao = baicao;
    }

    public ServerConfig getXocxoc() {
        return this.xocxoc;
    }

    public void setXocxoc(ServerConfig xocxoc) {
        this.xocxoc = xocxoc;
    }

    public ServerConfig getPoker() {
        return this.poker;
    }

    public void setPoker(ServerConfig poker) {
        this.poker = poker;
    }

    public ServerConfig getSam() {
        return this.sam;
    }

    public void setSam(ServerConfig sam) {
        this.sam = sam;
    }

    public ServerConfig getBacay() {
        return this.bacay;
    }

    public void setBacay(ServerConfig bacay) {
        this.bacay = bacay;
    }

    public ServerConfig getBinh() {
        return this.binh;
    }

    public void setBinh(ServerConfig binh) {
        this.binh = binh;
    }
}

