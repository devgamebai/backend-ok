package com.vinplay.vbee.common.models.rtp;

public class GameRtpSchedule {
    private long id;
    private String gameCode;
    private String cronExpr;
    private double winRatePct;
    private int durationMin;
    private int active;
    private String createdBy;
    private String createdAt;
    private String lastFiredAt;
    private String description;

    public GameRtpSchedule() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
    public String getCronExpr() { return cronExpr; }
    public void setCronExpr(String cronExpr) { this.cronExpr = cronExpr; }
    public double getWinRatePct() { return winRatePct; }
    public void setWinRatePct(double winRatePct) { this.winRatePct = winRatePct; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
    public int getActive() { return active; }
    public void setActive(int active) { this.active = active; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(String lastFiredAt) { this.lastFiredAt = lastFiredAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
