/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.safebox.core;

import com.vinplay.safebox.response.SafeBoxResponse;

public interface SafeBoxService {
    public SafeBoxResponse depositSafeBox(String var1, double var2);

    public SafeBoxResponse getSafeBox(String var1);

    public SafeBoxResponse withDraw(String var1, double var2, String var4);
}

