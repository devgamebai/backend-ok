/*
 * Decompiled with CFR 0.152.
 */
package com.payment.core.hook;

import java.util.Set;

public class ProcessorInfo {
    protected String path;
    protected Set<String> whiteList;

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Set<String> getWhiteList() {
        return this.whiteList;
    }

    public void setWhiteList(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    public ProcessorInfo(String path, Set<String> whiteList) {
        this.path = path;
        this.whiteList = whiteList;
    }

    public ProcessorInfo() {
    }
}

