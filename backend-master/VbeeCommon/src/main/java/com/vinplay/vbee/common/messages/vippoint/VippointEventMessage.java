/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages.vippoint;

import com.vinplay.vbee.common.messages.BaseMessage;

public class VippointEventMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String nickname;
    private int vpReal;
    private int vpEvent;
    private int vpAdd;
    private int numAdd;
    private int vpSub;
    private int numSub;
    private int place;
    private int placeMax;
    private int vp;
    private int type;

    public VippointEventMessage(int userId, String nickname, int vpReal, int vpEvent, int vpAdd, int numAdd, int vpSub, int numSub, int place, int placeMax, int vp, int type) {
        this.userId = userId;
        this.nickname = nickname;
        this.vpReal = vpReal;
        this.vpEvent = vpEvent;
        this.vpAdd = vpAdd;
        this.numAdd = numAdd;
        this.vpSub = vpSub;
        this.numSub = numSub;
        this.place = place;
        this.placeMax = placeMax;
        this.vp = vp;
        this.type = type;
    }

    public VippointEventMessage() {
    }

    public int getPlaceMax() {
        return this.placeMax;
    }

    public void setPlaceMax(int placeMax) {
        this.placeMax = placeMax;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getVpReal() {
        return this.vpReal;
    }

    public void setVpReal(int vpReal) {
        this.vpReal = vpReal;
    }

    public int getVpEvent() {
        return this.vpEvent;
    }

    public void setVpEvent(int vpEvent) {
        this.vpEvent = vpEvent;
    }

    public int getVpAdd() {
        return this.vpAdd;
    }

    public void setVpAdd(int vpAdd) {
        this.vpAdd = vpAdd;
    }

    public int getNumAdd() {
        return this.numAdd;
    }

    public void setNumAdd(int numAdd) {
        this.numAdd = numAdd;
    }

    public int getVpSub() {
        return this.vpSub;
    }

    public void setVpSub(int vpSub) {
        this.vpSub = vpSub;
    }

    public int getNumSub() {
        return this.numSub;
    }

    public void setNumSub(int numSub) {
        this.numSub = numSub;
    }

    public int getPlace() {
        return this.place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getVp() {
        return this.vp;
    }

    public void setVp(int vp) {
        this.vp = vp;
    }
}

