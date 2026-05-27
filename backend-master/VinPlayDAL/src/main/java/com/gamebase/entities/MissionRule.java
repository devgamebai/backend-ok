/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.entities;

import com.gamebase.entities.TypeRule;
import java.io.Serializable;

public class MissionRule
implements Serializable {
    private String id;
    private TypeRule type;
    private int game_id;
    private long point;

    public TypeRule getType() {
        return this.type;
    }

    public void setType(TypeRule type) {
        this.type = type;
    }

    public int getGame_id() {
        return this.game_id;
    }

    public void setGame_id(int game_id) {
        this.game_id = game_id;
    }

    public long getPoint() {
        return this.point;
    }

    public void setPoint(long point) {
        this.point = point;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

