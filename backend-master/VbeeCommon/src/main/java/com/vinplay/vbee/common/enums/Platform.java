/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.enums;

public enum Platform {
    WEB("web"),
    ANDROID("ad"),
    IOS("ios"),
    WINPHONE("wp"),
    FACEBOOK_APP("fb"),
    DESKTOP("dt"),
    OTHER("ot");
    
    private String name;

    private Platform(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Platform find(String name) {
        if (name == null) {
            return OTHER;
        }
        for (Platform pf : Platform.values()) {
            if (!pf.getName().equalsIgnoreCase(name)) continue;
            return pf;
        }
        return OTHER;
    }
}

