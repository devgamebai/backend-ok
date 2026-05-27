/*
 * Decompiled with CFR 0.152.
 */
package game.Jetty.model;

public class UserOnline {
    private int id;
    private String name;
    private String ip_address;
    private String roomName;
    private boolean isMobile;
    private boolean isWeb;
    private short privilegeId;
    private long lastLoginTime;
    private int playerIdByRoomId;
    private int badWordsWarnings;
    private int floodWarnings;
    private boolean beingKicked;
    private boolean connected;
    private boolean joining;

    public UserOnline() {
    }

    public UserOnline(int id, String name, String ip_address, String roomName, boolean isMobile, boolean isWeb, short privilegeId, long lastLoginTime, int playerIdByRoomId, int badWordsWarnings, int floodWarnings, boolean beingKicked, boolean connected, boolean joining) {
        this.id = id;
        this.name = name;
        this.ip_address = ip_address;
        this.roomName = roomName;
        this.isMobile = isMobile;
        this.isWeb = isWeb;
        this.privilegeId = privilegeId;
        this.lastLoginTime = lastLoginTime;
        this.playerIdByRoomId = playerIdByRoomId;
        this.badWordsWarnings = badWordsWarnings;
        this.floodWarnings = floodWarnings;
        this.beingKicked = beingKicked;
        this.connected = connected;
        this.joining = joining;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getPrivilegeId() {
        return this.privilegeId;
    }

    public void setPrivilegeId(short privilegeId) {
        this.privilegeId = privilegeId;
    }

    public long getLastLoginTime() {
        return this.lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public int getPlayerIdByRoomId() {
        return this.playerIdByRoomId;
    }

    public void setPlayerIdByRoomId(int playerIdByRoomId) {
        this.playerIdByRoomId = playerIdByRoomId;
    }

    public int getBadWordsWarnings() {
        return this.badWordsWarnings;
    }

    public void setBadWordsWarnings(int badWordsWarnings) {
        this.badWordsWarnings = badWordsWarnings;
    }

    public int getFloodWarnings() {
        return this.floodWarnings;
    }

    public void setFloodWarnings(int floodWarnings) {
        this.floodWarnings = floodWarnings;
    }

    public boolean isBeingKicked() {
        return this.beingKicked;
    }

    public void setBeingKicked(boolean beingKicked) {
        this.beingKicked = beingKicked;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isJoining() {
        return this.joining;
    }

    public void setJoining(boolean joining) {
        this.joining = joining;
    }

    public String getIp_address() {
        return this.ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean isMobile() {
        return this.isMobile;
    }

    public void setMobile(boolean mobile) {
        this.isMobile = mobile;
    }

    public boolean isWeb() {
        return this.isWeb;
    }

    public void setWeb(boolean web) {
        this.isWeb = web;
    }
}

