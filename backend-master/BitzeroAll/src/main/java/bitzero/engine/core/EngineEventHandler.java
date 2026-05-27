package bitzero.engine.core;

import bitzero.engine.events.Event;
import bitzero.engine.events.IEvent;
import bitzero.engine.events.IEventListener;

public class EngineEventHandler extends AbstractMethodDispatcher implements IEventListener {
     BitZeroEngine engine = BitZeroEngine.getInstance();

     public EngineEventHandler() {
          this.registerMethods();
     }

     private void registerMethods() {
          this.registerMethod("sessionAdded", "onSessionAdded");
          this.registerMethod("sessionLost", "onSessionLost");
          this.registerMethod("packetDropped", "onPacketDropped");
     }

     public void handleEvent(IEvent event) {
          try {
               this.callMethod(event.getName(), new Object[]{event});
          } catch (Exception var3) {
               var3.printStackTrace();
          }

     }

     public void onSessionLost(Object o) {
          this.engine.dispatchEvent((Event)o);
     }

     public void onSessionAdded(Object o) {
          this.engine.dispatchEvent((Event)o);
     }

     public void onPacketDropped(Object o) {
          this.engine.dispatchEvent((Event)o);
     }
}
