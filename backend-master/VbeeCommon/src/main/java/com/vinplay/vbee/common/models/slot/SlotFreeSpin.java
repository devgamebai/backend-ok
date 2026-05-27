/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.slot;

import java.io.Serializable;

public class SlotFreeSpin
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nickName;
    private String lines;
    private int num;
    private int ratio;
    private String itemsWild = "";
    private int totalPrizes;

    public SlotFreeSpin() {
        this.num = 0;
        this.ratio = 0;
    }

    public SlotFreeSpin(String nickName, String lines, int num, int ratio) {
        this.nickName = nickName;
        this.lines = lines;
        this.num = num;
        this.ratio = ratio;
    }

    public void clear() {
        this.lines = "";
        this.num = 0;
        this.ratio = 0;
        this.itemsWild = "";
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getNum() {
        return this.num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getRatio() {
        return this.ratio;
    }

    public void setRatio(int ratio) {
        this.ratio = ratio;
    }

    public void changeNum(int num) {
        this.num += num;
    }

    public String getLines() {
        return this.lines;
    }

    public void setLines(String lines) {
        this.lines = lines;
    }

    public void addItemWild(int row, int col) {
        if (this.itemsWild.length() > 0) {
            this.itemsWild = this.itemsWild + ",";
        }
        this.itemsWild = this.itemsWild + row + "," + col;
    }

    public void setItemsWild(String itemsWild) {
        this.itemsWild = itemsWild;
    }

    public String getItemsWild() {
        return this.itemsWild;
    }

    public int getPrizes() {
        return this.totalPrizes;
    }

    public void addPrize(int prize) {
        this.totalPrizes += prize;
    }
}

