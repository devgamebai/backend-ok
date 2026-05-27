/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ToiChonCaModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    public String username;
    public boolean valid;
    public short soCa;
    public short soCaHighScore;
    public short soVan;
    public long tongThang;
    public long tongDat;
    public long currentPhien;
    public List<Long> listPhien = new ArrayList<Long>();
    public Date date;

    // No-arg constructor required by Jackson when this model is read back from
    // Redis (cacheToiChonCa flipped to redis in Wave 2 batch E). Hazelcast's
    // built-in serializer didn't need one because it set fields directly via
    // reflection bypassing constructors. Without this, RedisCache.get returned
    // null and silently broke the Tôi Chọn Cá game state recovery.
    public ToiChonCaModel() {}

    public ToiChonCaModel(String username, long referenceId, long tienDat, long tienThang) {
        this.username = username;
        this.clear();
        this.soCa = 1;
        this.tongDat += tienDat;
        this.tongThang += tienThang;
        this.addNewPhien(referenceId);
        this.date = new Date();
    }

    public void clear() {
        this.valid = false;
        this.soCa = 0;
        this.soVan = 0;
        this.tongThang = 0L;
        this.tongDat = 0L;
        this.currentPhien = 0L;
        this.listPhien.clear();
        this.date = new Date();
    }

    public boolean addNewPhien(long referenceId) {
        if (this.currentPhien == referenceId) {
            return false;
        }
        this.soVan = (short)(this.soVan + 1);
        this.currentPhien = referenceId;
        this.listPhien.add(referenceId);
        return true;
    }

    public String getListPhien() {
        StringBuilder builder = new StringBuilder();
        if (this.listPhien.size() > 0) {
            builder.append(this.listPhien.get(0));
            for (int i = 1; i < this.listPhien.size(); ++i) {
                builder.append(",");
                builder.append(this.listPhien.get(i));
            }
        }
        return builder.toString();
    }

    public boolean playOnToday() {
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        Date today = new Date();
        return df.format(this.date).equalsIgnoreCase(df.format(today));
    }
}

