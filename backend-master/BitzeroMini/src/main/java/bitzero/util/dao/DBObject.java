package bitzero.util.dao;

import bitzero.util.config.bean.ConstantMercury;
import bitzero.util.datacontroller.business.DataController;
import java.io.Serializable;

public class DBObject implements IDBObject, Serializable {
     static final long serialVersionUID = 1L;
     private static final String SHARED_KEY = "shared";
     public static final String SEPERATOR = "_";
     private int uId = -1;

     public void save() throws Exception {
          StringBuilder builder = new StringBuilder(ConstantMercury.PREFIX_SNSGAME_GENERAL);
          builder.append(this.uId).append("_").append(this.getClass().getSimpleName());
          DataController.getController().set(builder.toString(), this);
     }

     public static Object load(int uId, Class c) throws Exception {
          StringBuilder builder = new StringBuilder(ConstantMercury.PREFIX_SNSGAME_GENERAL);
          builder.append(uId).append("_").append(c.getSimpleName());
          return DataController.getController().get(builder.toString());
     }
}
