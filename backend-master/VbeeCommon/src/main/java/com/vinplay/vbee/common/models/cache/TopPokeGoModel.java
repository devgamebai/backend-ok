/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TopPokeGoModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<TopPokeGo> results = new ArrayList<TopPokeGo>();

    public List<TopPokeGo> getResults() {
        return this.results;
    }

    public List<TopPokeGo> getResults(int page, int num) {
        int end;
        int start = (page - 1) * num;
        int n = end = page * num < this.results.size() ? page * num : this.results.size();
        if (end > start && start >= 0) {
            return this.results.subList(start, end);
        }
        return new ArrayList<TopPokeGo>();
    }

    public void setResults(List<TopPokeGo> results) {
        this.results = results;
    }

    public void put(TopPokeGo newEntry) {
        if (this.results.size() == 0) {
            this.results.add(newEntry);
        } else {
            this.results.add(0, newEntry);
        }
        if (this.results.size() > 100) {
            this.results.remove(this.results.size() - 1);
        }
    }
}

