package com.vinplay.vbee.common.response;

public class UserLoginLogModel {
    private String username;
    private String device_name;
    private String ip;
    private String location;
    private String time_log;
    private String platform;
    private boolean is_abnormal;

    public UserLoginLogModel() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime_log() {
        return time_log;
    }

    public void setTime_log(String time_log) {
        this.time_log = time_log;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isIs_abnormal() {
        return is_abnormal;
    }

    public void setIs_abnormal(boolean is_abnormal) {
        this.is_abnormal = is_abnormal;
    }
}
