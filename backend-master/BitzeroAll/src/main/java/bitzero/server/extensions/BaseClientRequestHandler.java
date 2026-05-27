package bitzero.server.extensions;

import bitzero.server.api.IBZApi;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import java.util.List;

public abstract class BaseClientRequestHandler implements IClientRequestHandler, IBZEventListener {
     private BZExtension parentExtension;

     public void init() {
          this.parentExtension.trace(new Object[]{this.getClass().getName(), "initilazion"});
     }

     public void handleServerEvent(IBZEvent ibzevent) throws Exception {
     }

     public BZExtension getParentExtension() {
          return this.parentExtension;
     }

     public void setParentExtension(BZExtension ext) {
          this.parentExtension = ext;
     }

     protected IBZApi getApi() {
          return this.parentExtension.bzApi;
     }

     protected void send(BaseMsg msg, User recipient) {
          this.parentExtension.send(msg, recipient);
     }

     protected void send(BaseMsg msg, List recipients) {
          this.parentExtension.send(msg, recipients);
     }

     protected void trace(Object... args) {
          this.parentExtension.trace(args);
     }

     protected void trace(ExtensionLogLevel level, Object... args) {
          this.parentExtension.trace(level, args);
     }
}
