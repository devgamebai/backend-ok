package bitzero.util.datacontroller.business;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.CommonHandle;

public class DataController {
     private static IDataController _instance = null;

     public static IDataController getController() {
          Class var0 = DataController.class;
          synchronized(DataController.class) {
               if (_instance == null) {
                    try {
                         _instance = (IDataController)Class.forName(ConfigHandle.instance().get("dataProvider")).newInstance();
                    } catch (Exception var3) {
                         CommonHandle.writeErrLog((Throwable)var3);
                    }
               }
          }

          return _instance;
     }
}
