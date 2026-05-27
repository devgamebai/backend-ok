package bitzero.server.entities.managers;

import bitzero.server.extensions.IBZExtension;
import java.util.List;

public interface IExtensionManager {
     int getExtensionsCount();

     List getExtensions();

     IBZExtension getMainExtension();

     IBZExtension getAdminExtension();

     void init();

     void destroy();

     void monitor();

     void activateAllExtensions();

     void deactivateAllExtensions();

     boolean isExtensionMonitorActive();

     void setExtensionMonitorActive(boolean var1);
}
