package bitzero.server.entities;

import bitzero.server.config.ConfigHandle;
import bitzero.server.entities.managers.IExtensionManager;
import bitzero.server.extensions.IBZExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionManager implements IExtensionManager {
     Map extensions;
     IBZExtension extMain = null;

     public IBZExtension getMainExtension() {
          return this.extMain;
     }

     public IBZExtension getAdminExtension() {
          return null;
     }

     private void initExtensions() {
          this.extensions = new HashMap();
          String mainExtPath = ConfigHandle.instance().get("extension.main.path");

          try {
               this.extMain = (IBZExtension)Class.forName(mainExtPath).newInstance();
          } catch (Exception var3) {
               var3.printStackTrace();
          }

          if (this.extMain != null) {
               this.extMain.init();
               this.extensions.put("main", this.extMain);
          }

     }

     public List getExtensions() {
          return null;
     }

     public int getExtensionsCount() {
          return 0;
     }

     public void init() {
          this.initExtensions();
     }

     public void destroy() {
          this.getMainExtension().destroy();
     }

     public void monitor() {
          this.getMainExtension().monitor();
     }

     public void activateAllExtensions() {
     }

     public void deactivateAllExtensions() {
     }

     public boolean isExtensionMonitorActive() {
          return false;
     }

     public void setExtensionMonitorActive(boolean flag) {
     }
}
