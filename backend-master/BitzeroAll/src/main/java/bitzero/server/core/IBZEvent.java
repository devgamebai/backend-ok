package bitzero.server.core;

public interface IBZEvent {
     IBZEventType getType();

     Object getParameter(IBZEventParam var1);
}
