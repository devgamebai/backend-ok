package bitzero.server.core;

public interface IBZEventDispatcher {
     void addEventListener(IBZEventType var1, IBZEventListener var2);

     boolean hasEventListener(IBZEventType var1);

     void removeEventListener(IBZEventType var1, IBZEventListener var2);

     void dispatchEvent(IBZEvent var1);
}
