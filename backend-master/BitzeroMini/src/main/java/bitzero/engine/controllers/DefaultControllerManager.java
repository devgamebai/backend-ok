package bitzero.engine.controllers;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultControllerManager implements IControllerManager {
     protected String name;
     protected ConcurrentMap controllers = new ConcurrentHashMap();

     public void init(Object o) {
          this.startAllControllers();
     }

     public void destroy(Object o) {
          this.shutDownAllControllers();
          this.controllers = null;
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          this.name = name;
     }

     public void handleMessage(Object obj) {
     }

     public void addController(Object id, IController controller) {
          this.controllers.putIfAbsent(id, controller);
     }

     public IController getControllerById(Object id) {
          return (IController)this.controllers.get(id);
     }

     public void removeController(Object id) {
          this.controllers.remove(id);
     }

     private synchronized void shutDownAllControllers() {
          Iterator iterator = this.controllers.values().iterator();

          while(iterator.hasNext()) {
               IController controller = (IController)iterator.next();
               controller.destroy((Object)null);
          }

     }

     private synchronized void startAllControllers() {
          Iterator iterator = this.controllers.values().iterator();

          while(iterator.hasNext()) {
               IController controller = (IController)iterator.next();
               controller.init((Object)null);
          }

     }
}
