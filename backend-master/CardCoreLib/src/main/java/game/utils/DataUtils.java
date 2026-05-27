/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.datacontroller.business.DataController
 *  com.google.gson.Gson
 */
package game.utils;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataController;
import com.google.gson.Gson;
import java.lang.reflect.Type;

public class DataUtils {
    public static final Gson gson = new Gson();

    public static Object copyDataFromDB(String key, Class theClass) {
        try {
            String data = (String)DataController.getController().get(key);
            if (data != null) {
                return gson.fromJson(data, theClass);
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            return null;
        }
        return null;
    }

    public static void saveToDB(String key, Object object, Class theClass) {
        String data = "";
        try {
            data = gson.toJson(object, (Type)theClass);
            DataController.getController().set(key, (Object)data);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((String)data);
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public static void removeDB(String key) {
        try {
            DataController.getController().delete(key);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }
}

