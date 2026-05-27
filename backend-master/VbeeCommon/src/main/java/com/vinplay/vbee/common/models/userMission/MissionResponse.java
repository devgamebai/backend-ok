/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.userMission;

import java.io.Serializable;

public class MissionResponse
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String misNa;
    private int misWin;
    private int misMax;
    private int recReLev;
    private int compAllLev;
    private long moBo;

    public MissionResponse() {
    }

    public MissionResponse(String misNa, int misWin, int misMax, int recReLev, int compAllLev, long moBo) {
        this.misNa = misNa;
        this.misWin = misWin;
        this.misMax = misMax;
        this.recReLev = recReLev;
        this.compAllLev = compAllLev;
        this.moBo = moBo;
    }

    public int getCompAllLev() {
        return this.compAllLev;
    }

    public void setCompAllLev(int compAllLev) {
        this.compAllLev = compAllLev;
    }

    public String getMisNa() {
        return this.misNa;
    }

    public void setMisNa(String misNa) {
        this.misNa = misNa;
    }

    public int getMisWin() {
        return this.misWin;
    }

    public void setMisWin(int misWin) {
        this.misWin = misWin;
    }

    public int getMisMax() {
        return this.misMax;
    }

    public void setMisMax(int misMax) {
        this.misMax = misMax;
    }

    public int getRecReLev() {
        return this.recReLev;
    }

    public void setRecReLev(int recReLev) {
        this.recReLev = recReLev;
    }

    public long getMoBo() {
        return this.moBo;
    }

    public void setMoBo(long moBo) {
        this.moBo = moBo;
    }
}

