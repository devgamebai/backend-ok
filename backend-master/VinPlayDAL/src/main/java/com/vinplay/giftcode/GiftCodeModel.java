/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.giftcode;

import java.sql.Timestamp;

public class GiftCodeModel {
    public int id;
    public String giftcode;
    public int type;
    public int money;
    public int time_used = 0;
    public int max_use;
    public Timestamp from;
    public Timestamp exprired;
    public Timestamp created_at;
    public String created_by = "";
    public int event = 0;
    public String user_name = "";
    public long bundle_id;

    public GiftCodeModel() {
    }

    public GiftCodeModel(int id, String giftcode, int type, int money, int time_used, int max_use, Timestamp from, Timestamp exprired, Timestamp created_at, String created_by, int event, String user_name) {
        this.id = id;
        this.giftcode = giftcode;
        this.type = type;
        this.money = money;
        this.time_used = time_used;
        this.max_use = max_use;
        this.from = from;
        this.exprired = exprired;
        this.created_at = created_at;
        this.created_by = created_by;
        this.event = event;
        this.user_name = user_name;
    }
}

