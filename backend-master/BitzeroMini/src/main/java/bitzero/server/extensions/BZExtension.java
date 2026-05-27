package bitzero.server.extensions;

import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.exceptions.BZException;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.extensions.data.DataCmd;

public abstract class BZExtension extends BaseBZExtension {
     public static final String MULTIHANDLER_REQUEST_ID = "__[[REQUEST_ID]]__";
     public final IHandlerFactory handlerFactory = new BZHandlerFactory(this);

     public void destroy() {
          this.handlerFactory.clearAll();
          this.removeEventsForListener(this);
     }

     protected void addRequestHandler(short requestId, Class theClass) {
          if (!IClientRequestHandler.class.isAssignableFrom(theClass)) {
               throw new BZRuntimeException(String.format("Provided Request Handler does not implement IClientRequestHandler: %s, Cmd: %s", theClass, requestId));
          } else {
               this.handlerFactory.addHandler(requestId, theClass);
          }
     }

     protected void addEventHandler(IBZEventType eventType, Class theClass) {
          if (!IServerEventHandler.class.isAssignableFrom(theClass)) {
               throw new BZRuntimeException(String.format("Provided Event Handler does not implement IServerEventHandler: %s, Cmd: %s", theClass, eventType.toString()));
          } else {
               this.addEventListener(eventType, this);
               this.handlerFactory.addHandler(eventType.toString(), theClass);
          }
     }

     protected void removeRequestHandler(String requestId) {
          this.handlerFactory.removeHandler(requestId);
     }

     protected void removeEventHandler(IBZEventType eventType) {
          this.bz.getEventManager().removeEventListener(eventType, this);
          this.removeEventListener(eventType, this);
          this.handlerFactory.removeHandler(eventType.toString());
     }

     protected void clearAllHandlers() {
          this.handlerFactory.clearAll();
     }

     public void handleClientRequest(short requestId, User sender, DataCmd dataCmd) {
          try {
               IClientRequestHandler handler = (IClientRequestHandler)this.handlerFactory.findHandler(requestId);
               if (handler == null) {
                    throw new BZRuntimeException("Request handler not found: '" + requestId + "'. Make sure the handler is registered in your extension using addRequestHandler()");
               }

               dataCmd.setId(requestId);
               handler.handleClientRequest(sender, dataCmd);
          } catch (InstantiationException var5) {
               this.trace(ExtensionLogLevel.WARN, new Object[]{"Cannot instantiate handler class: " + var5});
          } catch (IllegalAccessException var6) {
               this.trace(ExtensionLogLevel.WARN, new Object[]{"Illegal access for handler class: " + var6});
          }

     }

     public void handleClientRequest(String cmdId, User user, ISFSObject objData) throws BZException {
     }

     public void handleServerEvent(IBZEvent event) throws Exception {
          String handlerId = event.getType().toString();

          try {
               IServerEventHandler handler = (IServerEventHandler)this.handlerFactory.findHandler(handlerId);
               if (handler == null) {
                    throw new BZRuntimeException("Event handler not found: '" + handlerId + "'. Make sure the handler is registered in your extension using addEventHandler()");
               }

               handler.handleServerEvent(event);
          } catch (IllegalAccessException var5) {
               this.trace(ExtensionLogLevel.WARN, new Object[]{"Illegal access for handler class: " + var5});
          } catch (InstantiationException var6) {
               this.trace(ExtensionLogLevel.WARN, new Object[]{"Cannot instantiate handler class: " + var6});
          }

     }
}
