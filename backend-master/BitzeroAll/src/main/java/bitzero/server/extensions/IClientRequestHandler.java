package bitzero.server.extensions;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;

public interface IClientRequestHandler {
     void handleClientRequest(User var1, DataCmd var2);

     void setParentExtension(BZExtension var1);

     BZExtension getParentExtension();

     void init();
}
