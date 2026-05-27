/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models;

import java.io.Serializable;

public class UserValueModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    public int id;
    public String nick_name;
    public long deposit;
    public long withdraw;

    public UserValueModel() {
    }

    public UserValueModel(String nick_name) {
        this.nick_name = nick_name;
        this.deposit = 0L;
        this.withdraw = 0L;
    }
}

