package bitzero.engine.io;

public interface IEngineMessage {
     Object getId();

     void setId(Object var1);

     Object getContent();

     void setContent(Object var1);

     Object getAttribute(String var1);

     void setAttribute(String var1, Object var2);
}
