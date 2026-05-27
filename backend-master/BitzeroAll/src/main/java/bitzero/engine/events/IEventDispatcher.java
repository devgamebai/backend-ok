package bitzero.engine.events;

public interface IEventDispatcher {
     void addEventListener(String var1, IEventListener var2);

     boolean hasEventListener(String var1);

     void removeEventListener(String var1, IEventListener var2);

     void dispatchEvent(IEvent var1);
}
