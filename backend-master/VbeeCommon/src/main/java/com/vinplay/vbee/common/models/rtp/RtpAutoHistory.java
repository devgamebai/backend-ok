package com.vinplay.vbee.common.models.rtp;

public class RtpAutoHistory {
    private long id;
    private int userId;
    private String nickName;
    private int policyId;
    private long triggerWin;
    private double appliedRtp;
    private String expiresAt;
    private String createdAt;

    public RtpAutoHistory() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public int getPolicyId() { return policyId; }
    public void setPolicyId(int policyId) { this.policyId = policyId; }
    public long getTriggerWin() { return triggerWin; }
    public void setTriggerWin(long triggerWin) { this.triggerWin = triggerWin; }
    public double getAppliedRtp() { return appliedRtp; }
    public void setAppliedRtp(double appliedRtp) { this.appliedRtp = appliedRtp; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
