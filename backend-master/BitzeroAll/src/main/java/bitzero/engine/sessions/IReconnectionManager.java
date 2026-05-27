package bitzero.engine.sessions;

import bitzero.engine.exceptions.SessionReconnectionException;

public interface IReconnectionManager {
     ISession getReconnectableSession(String var1);

     ISession reconnectSession(ISession var1, String var2) throws SessionReconnectionException;

     void onSessionLost(ISession var1);

     ISessionManager getSessionManager();
}
