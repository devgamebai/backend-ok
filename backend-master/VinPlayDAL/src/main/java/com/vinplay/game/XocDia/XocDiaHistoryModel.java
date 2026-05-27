/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.game.XocDia;

import com.vinplay.game.XocDia.XocDiaHistoryItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class XocDiaHistoryModel
implements Serializable {
    private static final long serialVersionUID = -8655119501137984381L;
    public List<XocDiaHistoryItem> data = new ArrayList<XocDiaHistoryItem>();

    public synchronized void add(XocDiaHistoryItem item) {
        this.data.add(item);
        if (this.data.size() > 100) {
            this.data.remove(0);
        }
    }
}

