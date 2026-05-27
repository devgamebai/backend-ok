package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.helper.ExtensionCmd;
import bitzero.server.controllers.admin.helper.JBit;
import bitzero.server.controllers.admin.utils.BaseAdminCommand;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.data.DataCmd;

public class CrossExtCommand extends BaseAdminCommand {
     public CrossExtCommand() {
          super(SystemRequest.CrossExtCommand);
     }

     public boolean validate(IRequest irequest) {
          return true;
     }

     public void handleRequest(ISession sender, DataCmd cmd) {
          ExtensionCmd data = new ExtensionCmd(sender, cmd);

          try {
               int command = Integer.parseInt(data.command);
               String[] args = data.params;
               BitZeroServer bz = BitZeroServer.getInstance();
               switch(command) {
               case 1:
               case 2:
                    JBit jbit = new JBit();
                    if (args.length < 1) {
                         throw new Exception("Invalid params");
                    }

                    short id = Short.parseShort(args[0]);
                    BZExtension bzExt = (BZExtension)bz.getExtensionManager().getMainExtension();
                    Object obj = bzExt.handlerFactory.findHandler(id);
                    if (obj == null) {
                         throw new Exception("Invalid id: " + id);
                    }

                    if (args.length == 1) {
                         data.sendReturn(jbit.revertObject(obj));
                    } else {
                         String[] fnames = new String[args.length - 1];
                         Object result = jbit.runObject(obj, fnames);
                         if (result == null) {
                              StringBuilder sb = new StringBuilder();

                              for(int i = 0; i < args.length; ++i) {
                                   sb.append(args[i]).append("|");
                              }

                              throw new Exception("Invalid sub params " + sb.toString());
                         }

                         data.sendReturn(jbit.revertObject(result));
                    }
                    break;
               case 1991:
                    bz.halt();
               }
          } catch (Exception var15) {
               String err = String.format("{'error':1,'msg':%s}", var15.toString());
               data.sendReturn(err);
          }
     }
}
