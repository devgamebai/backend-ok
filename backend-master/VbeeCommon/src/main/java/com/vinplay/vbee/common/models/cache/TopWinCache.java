/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.minigame.TopWin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TopWinCache
implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<TopWin> result = new ArrayList<TopWin>();

    public List<TopWin> getResult() {
        return this.result;
    }

    public void setResult(List<TopWin> result) {
        this.result = result;
    }
}

