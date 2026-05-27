/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.cp;

import com.vinplay.vbee.common.cp.Param;

public interface BaseProcessor<T, R> {
    public R execute(Param<T> var1);
}

