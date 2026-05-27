/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BaseServerEventHandler
 *  com.vinplay.vbee.common.enums.Platform
 */
package game.eventHandlers;

import bitzero.server.core.BZEventParam;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventParam;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BaseServerEventHandler;
import com.vinplay.vbee.common.enums.Platform;

public class UserDisconnectHandler
extends BaseServerEventHandler {
    public synchronized void handleServerEvent(IBZEvent ibzevent) throws BZException {
        User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
        String pf = (String)user.getProperty("pf");
        Platform platform = Platform.find((String)pf);
        switch (platform) {
            case WEB: {
                --game.BaseGameExtension.ccuWeb;
                break;
            }
            case ANDROID: {
                --game.BaseGameExtension.ccuAD;
                break;
            }
            case IOS: {
                --game.BaseGameExtension.ccuIOS;
                break;
            }
            case WINPHONE: {
                --game.BaseGameExtension.ccuWP;
                break;
            }
            case FACEBOOK_APP: {
                --game.BaseGameExtension.ccuFB;
                break;
            }
            case DESKTOP: {
                --game.BaseGameExtension.ccuDT;
            }
        }
    }

}

