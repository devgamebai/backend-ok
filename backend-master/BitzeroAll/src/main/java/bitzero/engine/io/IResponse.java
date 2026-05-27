package bitzero.engine.io;

import bitzero.engine.data.TransportType;
import bitzero.engine.sessions.ISession;
import java.util.Collection;

public interface IResponse extends IEngineMessage {
     TransportType getTransportType();

     void setTransportType(TransportType var1);

     Object getTargetController();

     void setTargetController(Object var1);

     Collection getRecipients();

     void setRecipients(Collection var1);

     void setRecipients(ISession var1);

     boolean isTCP();

     boolean isUDP();

     void write();

     void write(int var1);
}
