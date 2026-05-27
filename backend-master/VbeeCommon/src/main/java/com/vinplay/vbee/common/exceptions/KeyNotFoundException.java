/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.exceptions;

public class KeyNotFoundException
extends Exception {
    private static final long serialVersionUID = 1L;

    public KeyNotFoundException() {
        super("Key not found");
    }

    public KeyNotFoundException(String key) {
        super("Key not found " + key);
    }
}

