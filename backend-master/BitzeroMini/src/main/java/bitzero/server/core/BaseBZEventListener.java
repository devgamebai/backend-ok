package bitzero.server.core;

public class BaseBZEventListener {
     private Object parentObject;

     public BaseBZEventListener() {
          this.parentObject = null;
     }

     public BaseBZEventListener(Object parentObject) {
          this.parentObject = parentObject;
     }

     public Object getParentObject() {
          return this.parentObject;
     }

     public void handleServerEvent(IBZEvent ibzevent) {
     }

     public String toString() {
          return this.parentObject != null ? this.parentObject.toString() : "{ Anonymous listener }";
     }
}
