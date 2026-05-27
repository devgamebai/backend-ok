/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TopGalaxyModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<TopGalaxy> results = new ArrayList<TopGalaxy>();

    public List<TopGalaxy> getResults() {
        return this.results;
    }

    public List<TopGalaxy> getResults(int page, int num) {
        int start = (page - 1) * num;
        int end = page * num < this.results.size() ? page * num : this.results.size();
        int n = end;
        if (end > start && start >= 0) {
            return this.results.subList(start, end);
        }
        return new ArrayList<TopGalaxy>();
    }

    public void setResults(List<TopGalaxy> results) {
        this.results = results;
    }

    public void put(TopGalaxy newEntry) {
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

