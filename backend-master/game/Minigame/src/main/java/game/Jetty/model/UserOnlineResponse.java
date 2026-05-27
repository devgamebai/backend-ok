/*
 * Decompiled with CFR 0.152.
 */
package game.Jetty.model;

import game.Jetty.model.UserOnline;
import java.util.List;

public class UserOnlineResponse {
    private List<UserOnline> userOnlineList;
    private int totalRecord;

    public UserOnlineResponse(List<UserOnline> userOnlineList, int totalRecord) {
        this.userOnlineList = userOnlineList;
        this.totalRecord = totalRecord;
    }

    public UserOnlineResponse() {
    }

    public List<UserOnline> getUserOnlineList() {
        return this.userOnlineList;
    }

    public void setUserOnlineList(List<UserOnline> userOnlineList) {
        this.userOnlineList = userOnlineList;
    }

    public int getTotalRecord() {
        return this.totalRecord;
    }

    public void setTotalRecord(int totalRecord) {
        this.totalRecord = totalRecord;
    }
}

