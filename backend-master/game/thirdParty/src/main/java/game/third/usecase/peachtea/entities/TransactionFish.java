/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.peachtea.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionFish {
    private String token;
    private String txId;
    private double totalBet;
    private double totalReward;
    private String timeStamp;
    private int gameId;
    private String fishType;
    private String nickname;

    public TransactionFish(String token, String txId, double totalBet, double totalReward, int gameId, String fishType, String nickname) {
        this.token = token;
        this.txId = txId;
        this.setTotalBet(totalBet);
        this.setTotalReward(totalReward);
        this.timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        this.gameId = gameId;
        this.fishType = fishType;
        this.nickname = nickname;
    }

    public TransactionFish() {
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTxId() {
        return this.txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public double getTotalBet() {
        return this.totalBet;
    }

    public void setTotalBet(double totalBet) {
        if (totalBet < 0.0) {
            throw new IllegalArgumentException("TotalBet cannot be negative");
        }
        this.totalBet = totalBet;
    }

    public double getTotalReward() {
        return this.totalReward;
    }

    public void setTotalReward(double totalReward) {
        if (totalReward < 0.0) {
            throw new IllegalArgumentException("TotalReward cannot be negative");
        }
        this.totalReward = totalReward;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getGameId() {
        return this.gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getFishType() {
        return this.fishType;
    }

    public void setFishType(String fishType) {
        this.fishType = fishType;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public double calculateNetAmount() {
        return this.totalReward - this.totalBet;
    }
}

