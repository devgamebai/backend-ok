/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public enum PokerTourType {
    Daily(1, "Daily"),
    Vip(2, "Vip");
    
    private int id;
    private String name;

    private PokerTourType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static PokerTourType getById(int id) {
        for (PokerTourType e : PokerTourType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static PokerTourType getByName(String name) {
        for (PokerTourType e : PokerTourType.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }
}

