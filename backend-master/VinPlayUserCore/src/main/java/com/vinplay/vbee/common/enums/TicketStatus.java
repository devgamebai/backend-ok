/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.enums;

public enum TicketStatus {
    FINISHED(0, "vtt", "Viettel", 0),
    WAITING(1, "vnp", "Vinaphone", 0),
    RUNNING(2, "vms", "Mobifone", 0),
    WON(3, "vnm", "VietNamMobile", 0),
    LOSE(4, "bee", "BeeLine", 0),
    DRAW(5, "gate", "Gate", 1),
    REJECT(6, "zing", "Zing", 1),
    VOID(7, "vcoin", "Vcoin", 1),
    HAFT_WON(8, "sfone", "SFone", 0),
    HAFT_LOSE(9, "gtel", "GMobile", 0),
    REFUND(10, "garena", "Garena", 1);

    private int id;
    private String value;
    private String name;
    private int type;

    private TicketStatus(int id, String value, String name, int type) {
        this.id = id;
        this.value = value;
        this.name = name;
        this.type = type;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static TicketStatus getProviderById(int id) {
        for (TicketStatus e : TicketStatus.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static TicketStatus getProviderByName(String name) {
        for (TicketStatus e : TicketStatus.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }

    public static TicketStatus getProviderByValue(String value) {
        for (TicketStatus e : TicketStatus.values()) {
            if (!e.getValue().equals(value)) continue;
            return e;
        }
        return null;
    }
}

