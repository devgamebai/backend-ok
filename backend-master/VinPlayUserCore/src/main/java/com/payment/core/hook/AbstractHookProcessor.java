/*
 * Decompiled with CFR 0.152.
 */
package com.payment.core.hook;

import com.payment.core.hook.Context;
import com.payment.core.hook.HookProcessor;

public abstract class AbstractHookProcessor<T, R>
implements HookProcessor<T, R> {
    protected Context context;

    @Override
    public void context(Context context) {
        this.context = context;
    }
}

