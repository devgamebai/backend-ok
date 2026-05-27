package bitzero.engine.core;

import bitzero.engine.events.IEvent;
import bitzero.engine.events.IEventListener;

public class EngineIOEventHandler implements IEventListener {
     private final BitZeroEngine engine = BitZeroEngine.getInstance();

     public void handleEvent(IEvent event) {
          this.engine.dispatchEvent(event);
     }
}
