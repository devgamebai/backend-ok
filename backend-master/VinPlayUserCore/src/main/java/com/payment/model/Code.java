/*
 * Decompiled with CFR 0.152.
 */
package com.payment.model;

public enum Code {
    SUCCESS(0),
    NOT_SUCCESS(1),
    ERROR(3);

    private final int value;

    private Code(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}

