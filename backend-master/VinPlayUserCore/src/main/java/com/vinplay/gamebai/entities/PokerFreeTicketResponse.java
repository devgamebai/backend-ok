/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import com.vinplay.gamebai.entities.PokerFreeTicket;

public class PokerFreeTicketResponse {
    public PokerFreeTicket ticket;
    public int errorCode;
    public int timeBlock;
    public int fail;

    public PokerFreeTicketResponse(PokerFreeTicket ticket, int errorCode, int timeBlock, int fail) {
        this.ticket = ticket;
        this.errorCode = errorCode;
        this.timeBlock = timeBlock;
        this.fail = fail;
    }

    public PokerFreeTicketResponse() {
    }

    public PokerFreeTicket getTicket() {
        return this.ticket;
    }

    public void setTicket(PokerFreeTicket ticket) {
        this.ticket = ticket;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getTimeBlock() {
        return this.timeBlock;
    }

    public void setTimeBlock(int timeBlock) {
        this.timeBlock = timeBlock;
    }

    public int getFail() {
        return this.fail;
    }

    public void setFail(int fail) {
        this.fail = fail;
    }
}

