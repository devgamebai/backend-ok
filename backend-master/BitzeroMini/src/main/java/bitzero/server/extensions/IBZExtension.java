package bitzero.server.extensions;

import bitzero.engine.sessions.ISession;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

public interface IBZExtension {
     void init();

     void destroy();

     void monitor();

     String getName();

     void setName(String var1);

     String getExtensionFileName();

     void setExtensionFileName(String var1);

     String getPropertiesFileName();

     void setPropertiesFileName(String var1) throws IOException;

     Properties getConfigProperties();

     boolean isActive();

     void setActive(boolean var1);

     void addEventListener(IBZEventType var1, IBZEventListener var2);

     void removeEventListener(IBZEventType var1, IBZEventListener var2);

     void handleClientRequest(short var1, User var2, DataCmd var3) throws BZException;

     void handleClientRequest(String var1, User var2, ISFSObject var3) throws BZException;

     void doLogin(short var1, ISession var2, DataCmd var3) throws BZException;

     void doLogin(ISession var1, ISFSObject var2) throws Exception;

     Object handleInternalMessage(String var1, Object var2);

     void send(BaseMsg var1, User var2);

     void send(BaseMsg var1, Collection var2);
}
