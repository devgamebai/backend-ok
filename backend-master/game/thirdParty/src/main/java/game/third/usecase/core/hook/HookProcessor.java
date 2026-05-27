/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.core.hook;

import game.third.usecase.core.hook.Context;
import game.third.usecase.core.hook.Param;

public interface HookProcessor<T, R> {
    public void context(Context var1);

    public R execute(Param<T> var1);
}

