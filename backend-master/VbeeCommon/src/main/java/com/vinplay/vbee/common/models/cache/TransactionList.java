/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.response.LogMoneyUserResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TransactionList
implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<LogMoneyUserResponse> list = new ArrayList<LogMoneyUserResponse>();

    public List<LogMoneyUserResponse> getList() {
        return this.list;
    }

    public void setList(List<LogMoneyUserResponse> list) {
        this.list = list;
    }

    public int size() {
        return this.list.size();
    }

    public void add(LogMoneyUserResponse newEntry) {
        this.list.add(0, newEntry);
        while (this.size() > 65) {
            this.list.remove(this.size() - 1);
        }
    }

    public List<LogMoneyUserResponse> get(int page) {
        if (page < 1) {
            return new ArrayList<LogMoneyUserResponse>();
        }
        int start = (page - 1) * 13;
        int end = page * 13;
        int n = start = start >= 0 ? start : 0;
        if (start >= this.size()) {
            return new ArrayList<LogMoneyUserResponse>();
        }
        end = end <= this.size() ? end : this.size();
        return this.list.subList(start, end);
    }
}

