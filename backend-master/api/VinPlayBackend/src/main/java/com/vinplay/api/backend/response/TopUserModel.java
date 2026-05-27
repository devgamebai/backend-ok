package com.vinplay.api.backend.response;

public class TopUserModel {
    public String nickname;
    public long value;

    public TopUserModel(String nickname, long value) {
        this.nickname = nickname;
        this.value = value;
    }
}
