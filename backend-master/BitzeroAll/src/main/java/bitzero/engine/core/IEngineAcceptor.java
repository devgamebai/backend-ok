package bitzero.engine.core;

import bitzero.engine.config.SocketConfig;
import bitzero.engine.core.security.IConnectionFilter;
import java.io.IOException;
import java.util.List;

public interface IEngineAcceptor {
     void bindSocket(SocketConfig var1) throws IOException;

     List getBoundSockets();

     void handleAcceptableConnections();

     IConnectionFilter getConnectionFilter();

     void setConnectionFilter(IConnectionFilter var1);
}
