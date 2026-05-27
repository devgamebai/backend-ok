/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.cache.AgentDSModel;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TopDSModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, AgentDSModel> results = new HashMap<String, AgentDSModel>();

    public Map<String, AgentDSModel> getResults() {
        return this.results;
    }

    public void setResults(Map<String, AgentDSModel> results) {
        this.results = results;
    }
}

