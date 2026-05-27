/*
 * Decompiled with CFR 0.152.
 */
package com.payment.core.hook;

import com.payment.core.hook.Context;
import com.payment.core.hook.Param;

public interface HookProcessor<T, R> {
    public void context(Context var1);

    public R execute(Param<T> var1);
}

