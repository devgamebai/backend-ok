/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.core.hook;

import game.third.usecase.core.hook.Context;
import game.third.usecase.core.hook.HookProcessor;

public abstract class AbstractHookProcessor<T, R>
implements HookProcessor<T, R> {
    protected Context context;

    @Override
    public void context(Context context) {
        this.context = context;
    }
}

