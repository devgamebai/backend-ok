/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ThanhDuTXModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    public String username;
    public int number;
    public int maxNumber;
    public long totalValue;
    public boolean valid = false;
    public long currentReferenceId;
    public List<Long> references = new ArrayList<Long>();
    public Date date;

    public ThanhDuTXModel(String username) {
        this.username = username;
        this.number = 1;
        this.totalValue = 0L;
        this.valid = false;
        this.maxNumber = 0;
        this.date = new Date();
    }

    public void clear() {
        this.number = 0;
        this.valid = false;
        this.totalValue = 0L;
        this.references.clear();
        this.currentReferenceId = -1L;
        this.date = new Date();
    }

    public void addReference(long referenceId) {
        this.currentReferenceId = referenceId;
        this.references.add(referenceId);
    }

    public String getReferences() {
        StringBuilder builder = new StringBuilder();
        if (this.references.size() > 0) {
            builder.append(this.references.get(0));
            for (int i = 1; i < this.references.size(); ++i) {
                builder.append(",");
                builder.append(this.references.get(i));
            }
        }
        return builder.toString();
    }

    public void parseReferences(String str) {
        if (str.length() > 0) {
            String[] arr;
            for (String s : arr = str.split(",")) {
                this.references.add(Long.parseLong(s));
            }
        }
    }

    public boolean playOnToday() {
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        Date today = new Date();
        return df.format(this.date).equalsIgnoreCase(df.format(today));
    }
}

