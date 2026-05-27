/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.userMission;

import java.io.Serializable;

public class MissionObj
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String misNa;
    private int misLev;
    private int misWin;
    private int misMax;
    private boolean compMis;
    private boolean compAllLev;
    private int recReLev;

    public MissionObj() {
    }

    public MissionObj(String misNa, int misLev, int misWin, int misMax, boolean compMis, boolean compAllLev, int recReLev) {
        this.misNa = misNa;
        this.misLev = misLev;
        this.misWin = misWin;
        this.misMax = misMax;
        this.compMis = compMis;
        this.compAllLev = compAllLev;
        this.recReLev = recReLev;
    }

    public String getMisNa() {
        return this.misNa;
    }

    public void setMisNa(String misNa) {
        this.misNa = misNa;
    }

    public int getMisLev() {
        return this.misLev;
    }

    public void setMisLev(int misLev) {
        this.misLev = misLev;
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

    public boolean isCompMis() {
        return this.compMis;
    }

    public void setCompMis(boolean compMis) {
        this.compMis = compMis;
    }

    public boolean isCompAllLev() {
        return this.compAllLev;
    }

    public void setCompAllLev(boolean compAllLev) {
        this.compAllLev = compAllLev;
    }

    public int getRecReLev() {
        return this.recReLev;
    }

    public void setRecReLev(int recReLev) {
        this.recReLev = recReLev;
    }
}

