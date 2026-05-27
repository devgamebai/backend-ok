/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.giftcode;

import com.vinplay.dal.entities.giftcode.GiftCodeModel;
import java.util.ArrayList;
import java.util.Date;

public class GiftCodeBundleModel {
    private Integer id;
    private String name;
    private String created_by;
    private Date created_at;
    private Date updated_at;
    private ArrayList<GiftCodeModel> items;

    public GiftCodeBundleModel() {
    }

    public GiftCodeBundleModel(Integer id, String name, String created_by, Date created_at, Date updated_at) {
        this.id = id;
        this.name = name;
        this.created_by = created_by;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.items = new ArrayList();
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreated_by() {
        return this.created_by;
    }

    public void setCreated_by(String created_by) {
        this.created_by = created_by;
    }

    public Date getCreated_at() {
        return this.created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return this.updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.created_at = updated_at;
    }

    public void setItems(ArrayList<GiftCodeModel> items) {
        this.items = items;
    }

    public ArrayList<GiftCodeModel> getItems() {
        return this.items;
    }
}

