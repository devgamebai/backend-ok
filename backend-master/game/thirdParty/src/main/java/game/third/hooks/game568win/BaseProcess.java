/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.game568win;

import game.third.usecase.core.hook.AbstractHookProcessor;
import game.third.usecase.game568win.entities.ProductType;
import game.third.usecase.game568win.service.Game568winService;
import game.third.usecase.game568win.service.impl.Game568winServiceBase;
import game.third.usecase.game568win.service.impl.Game568winServiceSBO;
import game.third.usecase.game568win.service.impl.Game568winServiceSeamless;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

public abstract class BaseProcess
extends AbstractHookProcessor<HttpServletRequest, String> {
    protected Game568winService getByProduct(int productType) {
        ProductType type = ProductType.getById(productType);
        switch (Objects.requireNonNull(type)) {
            case SBO_GAMES: 
            case SBO_LIVE_CASINO: {
                return new Game568winServiceSBO();
            }
            case SEAMLESS_GAME_PROVIDER: {
                return new Game568winServiceSeamless();
            }
        }
        return new Game568winServiceBase();
    }
}

