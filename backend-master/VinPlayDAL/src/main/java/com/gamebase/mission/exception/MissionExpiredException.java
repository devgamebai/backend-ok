/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.mission.exception;

public class MissionExpiredException
extends Exception {
    private static final long serialVersionUID = 1L;
    private final String code;

    public MissionExpiredException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}

