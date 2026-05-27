/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.entities;

import java.io.Serializable;

public class BannerModel
implements Serializable {
    private Integer id;
    private String title;
    private Integer status;
    private String image_path;
    private String url;
    private Integer index;
    private int eventId;
    private String actionType;

    public BannerModel() {
    }

    public BannerModel(Integer id, String title, Integer status, String image_path) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.image_path = image_path;
    }

    public BannerModel(String title, Integer status, String image_path) {
        this.title = title;
        this.status = status;
        this.image_path = image_path;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getImage_path() {
        return this.image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }

    public Integer getIndex() {
        return this.index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public int getEventId() {
        return this.eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getActionType() {
        return this.actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}

