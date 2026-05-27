/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public enum PokerTourFreeCreateType {
    TopDay(1, "TopDay"),
    TopWeek(2, "TopWeek"),
    TopMonth(3, "TopMonth"),
    TopYear(4, "TopYear"),
    Code(5, "Code");
    
    private int id;
    private String name;

    private PokerTourFreeCreateType(int id, String name) {
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

    public static PokerTourFreeCreateType getById(int id) {
        for (PokerTourFreeCreateType e : PokerTourFreeCreateType.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static PokerTourFreeCreateType getByName(String name) {
        for (PokerTourFreeCreateType e : PokerTourFreeCreateType.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }
}

