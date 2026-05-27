/*
 * Decompiled with CFR 0_116.
 */
package bitzero.server.controllers.admin;

import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.utils.BaseAdminCommand;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.extensions.data.SimpleMsg;
import java.util.List;

public class CheckOnline
extends BaseAdminCommand {
    public CheckOnline() {
        super(SystemRequest.CheckOnline);
    }

    @Override
    public void handleRequest(ISession sender, DataCmd cmd) {
        List<User> users = this.bz.getUserManager().getUserByName(cmd.readString());
        if (users == null) {
            return;
        }
        User userToBan = users.get(0);
        SimpleMsg msg = new SimpleMsg(this.getId());
        if (userToBan == null) {
            msg.putString("offline");
        } else {
            msg.putString("online");
            if (userToBan.getZone() != null) {
                msg.putString(userToBan.getZone().getName());
            }
            if (userToBan.getJoinedRoom() != null) {
                msg.putString(userToBan.getJoinedRoom().toString());
            }
        }
        this.send(sender, msg);
    }
}

