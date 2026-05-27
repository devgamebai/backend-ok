/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

public enum PokerTourState {
    Onschedule(1, "Onschedule"),
    TourStarted(2, "TourStarted"),
    RegisterClosed(3, "RegisterClosed"),
    TourEnded(4, "TourEnded"),
    TourCanceled(5, "TourCanceled"),
    TourNotReady(6, "TourNotReady");
    
    private int id;
    private String name;

    private PokerTourState(int id, String name) {
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

    public static PokerTourState getById(int id) {
        for (PokerTourState e : PokerTourState.values()) {
            if (e.getId() != id) continue;
            return e;
        }
        return null;
    }

    public static PokerTourState getByName(String name) {
        for (PokerTourState e : PokerTourState.values()) {
            if (!e.getName().equals(name)) continue;
            return e;
        }
        return null;
    }
}

