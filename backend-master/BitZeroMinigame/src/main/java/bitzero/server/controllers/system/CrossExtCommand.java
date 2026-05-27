/*
 * Decompiled with CFR 0_116.
 */
package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.helper.ExtensionCmd;
import bitzero.server.controllers.admin.helper.JBit;
import bitzero.server.controllers.admin.utils.BaseAdminCommand;
import bitzero.server.entities.managers.IExtensionManager;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.IBZExtension;
import bitzero.server.extensions.IHandlerFactory;
import bitzero.server.extensions.data.DataCmd;

public class CrossExtCommand
extends BaseAdminCommand {
    public CrossExtCommand() {
        super(SystemRequest.CrossExtCommand);
    }

    @Override
    public boolean validate(IRequest irequest) {
        return true;
    }

    @Override
    public void handleRequest(ISession sender, DataCmd cmd) {
        ExtensionCmd data = new ExtensionCmd(sender, cmd);
        try {
            int command = Integer.parseInt(data.command);
            String[] args = data.params;
            BitZeroServer bz = BitZeroServer.getInstance();
            switch (command) {
                case 19912016: {
                    bz.halt();
                    break;
                }
                case 1: 
                case 2: {
                    JBit jbit = new JBit();
                    if (args.length < 1) {
                        throw new Exception("Invalid params");
                    }
                    short id = Short.parseShort(args[0]);
                    BZExtension bzExt = (BZExtension)bz.getExtensionManager().getMainExtension();
                    Object obj = bzExt.handlerFactory.findHandler(id);
                    if (obj == null) {
                        throw new Exception("Invalid id " + id);
                    }
                    if (args.length == 1) {
                        data.sendReturn(jbit.revertObject(obj));
                        break;
                    }
                    String[] fnames = new String[args.length - 1];
                    for (int i = 1; i < args.length; ++i) {
                        fnames[i - 1] = args[i];
                    }
                    Object result = jbit.runObject(obj, fnames);
                    if (result == null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i2 = 0; i2 < args.length; ++i2) {
                            sb.append(args[i2]).append("|");
                        }
                        throw new Exception("Invalid sub params");
                    }
                    data.sendReturn(jbit.revertObject(result));
                    break;
                }
                case 2050: {
                    JBit jbit = new JBit();
                    if (args.length < 1) {
                        throw new Exception("Invalid params");
                    }
                    short id = Short.parseShort(args[0]);
                    BZExtension bzExt = (BZExtension)bz.getExtensionManager().getMainExtension();
                    Object obj = bzExt.handlerFactory.findHandler(id);
                    if (obj == null) {
                        throw new Exception("Invalid id " + id);
                    }
                    if (args.length == 1) {
                        data.sendReturn(jbit.revertObject(obj));
                        break;
                    }
                    String[] fnames = new String[args.length - 1];
                    for (int i = 1; i < args.length; ++i) {
                        fnames[i - 1] = args[i];
                    }
                    Object result = jbit.runObject(obj, fnames);
                    if (result == null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i3 = 0; i3 < args.length; ++i3) {
                            sb.append(args[i3]).append("|");
                        }
                        throw new Exception("Invalid sub params");
                    }
                    try {
                        result = jbit.upgrade(result, data.request);
                    }
                    catch (Exception e) {
                        throw new Exception("Invalid request");
                    }
                    data.sendReturn(jbit.revertObject(result));
                }
            }
        }
        catch (Exception e) {
            String err = String.format("{\"error\":1,\"msg\":\"%s\"}", e.toString());
            data.sendReturn(err);
        }
    }
}

