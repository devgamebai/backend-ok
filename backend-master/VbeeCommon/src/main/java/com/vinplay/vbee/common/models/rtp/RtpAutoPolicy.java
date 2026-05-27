package com.vinplay.vbee.common.models.rtp;

public class RtpAutoPolicy {
    private int id;
    private String policyName;
    private long maxWinAmount;
    private int timeWindowMin;
    private double actionRtpPct;
    private int actionDuration;
    private int isActive;
    private String createdAt;
    private String description;

    public RtpAutoPolicy() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public long getMaxWinAmount() { return maxWinAmount; }
    public void setMaxWinAmount(long maxWinAmount) { this.maxWinAmount = maxWinAmount; }
    public int getTimeWindowMin() { return timeWindowMin; }
    public void setTimeWindowMin(int timeWindowMin) { this.timeWindowMin = timeWindowMin; }
    public double getActionRtpPct() { return actionRtpPct; }
    public void setActionRtpPct(double actionRtpPct) { this.actionRtpPct = actionRtpPct; }
    public int getActionDuration() { return actionDuration; }
    public void setActionDuration(int actionDuration) { this.actionDuration = actionDuration; }
    public int getIsActive() { return isActive; }
    public void setIsActive(int isActive) { this.isActive = isActive; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
