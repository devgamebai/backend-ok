package bitzero.server.extensions;

import bitzero.server.core.IBZEvent;
import bitzero.server.exceptions.BZException;

public interface IServerEventHandler {
     void handleServerEvent(IBZEvent var1) throws BZException;

     void setParentExtension(BZExtension var1);

     BZExtension getParentExtension();
}
