package com.vinplay.vbee.common.models.rtp;

public class RtpExperiment {
    private long id;
    private String name;
    private String gameCode;
    private String bucketJson;
    private String status;
    private String startedAt;
    private String endedAt;
    private String winnerBucket;
    private String createdBy;
    private String createdAt;

    public RtpExperiment() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
    public String getBucketJson() { return bucketJson; }
    public void setBucketJson(String bucketJson) { this.bucketJson = bucketJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
    public String getWinnerBucket() { return winnerBucket; }
    public void setWinnerBucket(String winnerBucket) { this.winnerBucket = winnerBucket; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
