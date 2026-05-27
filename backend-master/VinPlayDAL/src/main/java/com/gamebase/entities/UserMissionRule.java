/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  scala.Serializable
 */
package com.gamebase.entities;

import com.gamebase.entities.TypeRule;
import scala.Serializable;

public class UserMissionRule
implements Serializable {
    private static final long serialVersionUID = -235632346L;
    private String id;
    private TypeRule type;
    private int game_id;
    private long point;
    private int progress;
    private long work;
    private boolean completed;

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

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getWork() {
        return this.work;
    }

    public void setWork(long work) {
        this.work = work;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

