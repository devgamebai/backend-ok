/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.gamebai.entities;

import com.vinplay.gamebai.entities.UserTourModel;
import java.util.ArrayList;
import java.util.List;

public class TopTourModel {
    private int stt = 0;
    private String nickname;
    private long totalMark;
    private List<UserTourModel> tours = new ArrayList<UserTourModel>();

    public TopTourModel(String nickname, UserTourModel model) {
        this.tours.add(model);
        this.nickname = nickname;
        this.totalMark = model.getMark();
    }

    public void addTour(UserTourModel model, int maxTour) {
        if (this.tours != null && this.tours.size() < maxTour) {
            this.tours.add(model);
            this.totalMark += (long)model.getMark();
        }
    }

    public int getStt() {
        return this.stt;
    }

    public void setStt(int stt) {
        this.stt = stt;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public long getTotalMark() {
        return this.totalMark;
    }

    public void setTotalMark(long totalMark) {
        this.totalMark = totalMark;
    }

    public List<UserTourModel> getTours() {
        return this.tours;
    }

    public void setTours(List<UserTourModel> tours) {
        this.tours = tours;
    }
}

