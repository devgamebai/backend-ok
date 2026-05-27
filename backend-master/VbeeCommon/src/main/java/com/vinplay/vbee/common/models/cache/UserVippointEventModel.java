/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.vippoint.UserVPEventModel;
import java.io.Serializable;
import java.util.Date;

public class UserVippointEventModel
extends UserVPEventModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String nickname;
    private boolean isBot;
    private Date addVPTime;
    private Date subVPTime;
    private Date addVPVinTime;
    private int vpEvent;
    private int vpEventReal;
    private int vpAdd;
    private int vpSub;
    private int numAdd;
    private int numSub;
    private int place;
    private int placeMax;

    public UserVippointEventModel(int id, String nickname, boolean isBot) {
        this.id = id;
        this.nickname = nickname;
        this.isBot = isBot;
    }

    public UserVippointEventModel(int id, String nickname, boolean isBot, Date addVPTime, Date subVPTime, Date addVPVinTime, int vpEvent, int vpEventReal, int vpAdd, int vpSub, int numAdd, int numSub, int place, int placeMax) {
        this.id = id;
        this.nickname = nickname;
        this.isBot = isBot;
        this.addVPTime = addVPTime;
        this.subVPTime = subVPTime;
        this.addVPVinTime = addVPVinTime;
        this.vpEvent = vpEvent;
        this.vpEventReal = vpEventReal;
        this.vpAdd = vpAdd;
        this.vpSub = vpSub;
        this.numAdd = numAdd;
        this.numSub = numSub;
        this.place = place;
        this.placeMax = placeMax;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getNickname() {
        return this.nickname;
    }

    @Override
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isBot() {
        return this.isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public Date getAddVPTime() {
        return this.addVPTime;
    }

    public void setAddVPTime(Date addVPTime) {
        this.addVPTime = addVPTime;
    }

    public Date getSubVPTime() {
        return this.subVPTime;
    }

    public void setSubVPTime(Date subVPTime) {
        this.subVPTime = subVPTime;
    }

    public Date getAddVPVinTime() {
        return this.addVPVinTime;
    }

    public void setAddVPVinTime(Date addVPVinTime) {
        this.addVPVinTime = addVPVinTime;
    }

    @Override
    public int getVpEvent() {
        return this.vpEvent;
    }

    @Override
    public void setVpEvent(int vpEvent) {
        this.vpEvent = vpEvent;
    }

    public int getVpEventReal() {
        return this.vpEventReal;
    }

    public void setVpEventReal(int vpEventReal) {
        this.vpEventReal = vpEventReal;
    }

    @Override
    public int getVpAdd() {
        return this.vpAdd;
    }

    @Override
    public void setVpAdd(int vpAdd) {
        this.vpAdd = vpAdd;
    }

    @Override
    public int getVpSub() {
        return this.vpSub;
    }

    @Override
    public void setVpSub(int vpSub) {
        this.vpSub = vpSub;
    }

    @Override
    public int getNumAdd() {
        return this.numAdd;
    }

    @Override
    public void setNumAdd(int numAdd) {
        this.numAdd = numAdd;
    }

    @Override
    public int getNumSub() {
        return this.numSub;
    }

    @Override
    public void setNumSub(int numSub) {
        this.numSub = numSub;
    }

    @Override
    public int getPlace() {
        return this.place;
    }

    @Override
    public void setPlace(int place) {
        this.place = place;
    }

    @Override
    public int getPlaceMax() {
        return this.placeMax;
    }

    @Override
    public void setPlaceMax(int placeMax) {
        this.placeMax = placeMax;
    }
}

