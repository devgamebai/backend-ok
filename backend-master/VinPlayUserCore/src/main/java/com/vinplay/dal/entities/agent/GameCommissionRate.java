/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.agent;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class GameCommissionRate
implements Serializable {
    private static final long serialVersionUID = 1L;
    private long id;
    private String nickName;
    private int gameId;
    private String gameName;
    private double commissionRate;
    private int status;
    private Timestamp createdDate;
    private Timestamp updatedDate;

    public GameCommissionRate() {
    }

    public GameCommissionRate(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.nickName = rs.getString("nick_name");
        this.gameId = rs.getInt("game_id");
        this.gameName = rs.getString("game_name");
        this.commissionRate = rs.getDouble("commission_rate");
        this.status = rs.getInt("status");
        this.createdDate = rs.getTimestamp("created_date");
        this.updatedDate = rs.getTimestamp("updated_date");
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public int getGameId() {
        return this.gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return this.gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public double getCommissionRate() {
        return this.commissionRate;
    }

    public void setCommissionRate(double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Timestamp getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getUpdatedDate() {
        return this.updatedDate;
    }

    public void setUpdatedDate(Timestamp updatedDate) {
        this.updatedDate = updatedDate;
    }
}

