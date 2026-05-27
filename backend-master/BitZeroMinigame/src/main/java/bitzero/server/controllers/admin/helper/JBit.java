/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.json.JSONObject
 */
package bitzero.server.controllers.admin.helper;

import java.lang.reflect.Field;
import org.json.JSONObject;

public class JBit {
    private Object runObject(Object obj, String fname) {
        try {
            Field f = obj.getClass().getDeclaredField(fname);
            f.setAccessible(true);
            return f.get(obj);
        }
        catch (Exception e) {
            return null;
        }
    }

    public /* varargs */ Object runObject(Object obj, String ... fnames) {
        if (obj == null || fnames.length == 0) {
            return null;
        }
        Object nextObj = this.runObject(obj, fnames[0]);
        for (int i = 1; i < fnames.length; ++i) {
            nextObj = this.runObject(nextObj, fnames[i]);
        }
        return nextObj;
    }

    public String revertObject(Object obj) {
        JSONObject json = new JSONObject();
        if (obj.getClass().isPrimitive() || obj.getClass() == String.class || obj.getClass() == Byte.class || obj.getClass() == Short.class || obj.getClass() == Integer.class || obj.getClass() == Long.class || obj.getClass() == Boolean.class || obj.getClass() == Character.class || obj.getClass() == Float.class || obj.getClass() == Double.class || obj.getClass() == Void.class) {
            try {
                json.put("value", obj);
                return json.toString();
            }
            catch (Exception e) {
                return "{\"value\":\"null\"}";
            }
        }
        Field[] fs = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fs.length; ++i) {
            try {
                Field f = fs[i];
                f.setAccessible(true);
                if (f.getType().isPrimitive()) {
                    json.put(f.getName(), f.getLong(obj));
                    continue;
                }
                if (f.getType() == String.class) {
                    json.put(f.getName(), f.get(obj));
                    continue;
                }
                json.put(f.getName(), (Object)"Object");
                continue;
            }
            catch (Exception e) {
                // empty catch block
            }
        }
        return json.toString();
    }

    public Field getField(Object obj, String name) {
        Field[] fs = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fs.length; ++i) {
            try {
                Field f = fs[i];
                if (!f.getName().equals(name)) continue;
                f.setAccessible(true);
                return f;
            }
            catch (Exception e) {
                // empty catch block
            }
        }
        return null;
    }

    public Object upgradePrimitive(Object obj, String[] request) {
        if (request.length == 0) {
            return obj;
        }
        String x = request[0];
        Field f = this.getField(obj, "value");
        if (f == null) {
            return obj;
        }
        if (obj.getClass() == String.class) {
            try {
                char[] data = x.toCharArray();
                f.set(obj, data);
            }
            catch (Exception data) {
                // empty catch block
            }
            return obj;
        }
        if (obj.getClass() == Byte.class) {
            try {
                byte data = Byte.parseByte(x);
                f.set(obj, Byte.valueOf(data));
            }
            catch (Exception data) {
                // empty catch block
            }
            return obj;
        }
        if (obj.getClass() == Short.class) {
            try {
                short data = Short.parseShort(x);
                f.set(obj, data);
            }
            catch (Exception data) {
                // empty catch block
            }
            return obj;
        }
        if (obj.getClass() == Integer.class) {
            try {
                int data = Integer.parseInt(x);
                f.set(obj, data);
            }
            catch (Exception data) {
                // empty catch block
            }
            return obj;
        }
        if (obj.getClass() == Long.class) {
            try {
                long data = Long.parseLong(x);
                f.set(obj, data);
            }
            catch (Exception data) {
                // empty catch block
            }
            return obj;
        }
        if (obj.getClass() == Double.class) {
            try {
                double data = Double.parseDouble(x);
                f.set(obj, data);
            }
            catch (Exception data) {
                // empty catch block
            }
            return obj;
        }
        if (obj.getClass() == Float.class) {
            try {
                float data = Float.parseFloat(x);
                f.set(obj, Float.valueOf(data));
            }
            catch (Exception data) {
                // empty catch block
            }
        }
        return obj;
    }

    public Object upgrade(Object obj, String[] request) {
        if (obj.getClass().isPrimitive() || obj.getClass() == String.class || obj.getClass() == Byte.class || obj.getClass() == Short.class || obj.getClass() == Integer.class || obj.getClass() == Long.class || obj.getClass() == Boolean.class || obj.getClass() == Character.class || obj.getClass() == Float.class || obj.getClass() == Double.class || obj.getClass() == Void.class) {
            try {
                obj = this.upgradePrimitive(obj, request);
                return obj;
            }
            catch (Exception e) {
                return obj;
            }
        }
        if (request.length < 2) {
            return obj;
        }
        String x = request[0];
        String y = request[1];
        Field f = this.getField(obj, x);
        try {
            if (f.getType() == Byte.class || f.getType().getName().equalsIgnoreCase("byte")) {
                byte v = Byte.parseByte(y);
                f.set(obj, Byte.valueOf(v));
                return obj;
            }
            if (f.getType() == Short.class || f.getType().getName().equalsIgnoreCase("short")) {
                short v = Short.parseShort(y);
                f.set(obj, v);
                return obj;
            }
            if (f.getType() == Integer.class || f.getType().getName().equalsIgnoreCase("int")) {
                int v = Integer.parseInt(y);
                f.set(obj, v);
                return obj;
            }
            if (f.getType() == Long.class || f.getType().getName().equalsIgnoreCase("long")) {
                long v = Long.parseLong(y);
                f.set(obj, v);
                return obj;
            }
            if (f.getType() == Float.class || f.getType().getName().equalsIgnoreCase("float")) {
                float v = Float.parseFloat(y);
                f.set(obj, Float.valueOf(v));
                return obj;
            }
            if (f.getType() == Double.class || f.getType().getName().equalsIgnoreCase("double")) {
                double v = Double.parseDouble(y);
                f.set(obj, v);
                return obj;
            }
            if (f.getType() == String.class) {
                f.set(obj, y);
                return obj;
            }
        }
        catch (Exception e) {
            return obj;
        }
        return obj;
    }
}

