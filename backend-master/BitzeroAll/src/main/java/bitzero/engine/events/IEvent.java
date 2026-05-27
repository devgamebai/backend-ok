package bitzero.engine.events;

public interface IEvent {
     Object getTarget();

     void setTarget(Object var1);

     String getName();

     void setName(String var1);

     Object getParameter(String var1);

     void setParameter(String var1, Object var2);
}
