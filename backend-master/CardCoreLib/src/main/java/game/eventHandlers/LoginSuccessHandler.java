/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BaseServerEventHandler
 *  bitzero.util.ExtensionUtility
 */
package game.eventHandlers;

import bitzero.server.core.BZEventParam;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventParam;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseServerEventHandler;
import bitzero.util.ExtensionUtility;
import game.entities.PlayerInfo;
import game.modules.player.entities.NormalMoneyInfo;
import game.modules.player.entities.VipMoneyInfo;

public class LoginSuccessHandler
extends BaseServerEventHandler {
    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        this.onLoginSuccess((User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER));
    }

    private void onLoginSuccess(User user) {
        ExtensionUtility.instance().sendLoginOK(user);
        PlayerInfo pInfo = PlayerInfo.getInfo(user);
        if (pInfo != null) {
            NormalMoneyInfo nmInfo = NormalMoneyInfo.getInfo(user);
            VipMoneyInfo vmInfo = VipMoneyInfo.getInfo(user);
            nmInfo.avatar = pInfo.avatarUrl;
            vmInfo.avatar = pInfo.avatarUrl;
            nmInfo.save();
            vmInfo.save();
        }
    }
}

