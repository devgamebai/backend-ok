package bitzero.server.controllers.admin.helper;

import java.lang.reflect.Field;
import org.json.JSONObject;

public class JBit {
     private Object runObject(Object obj, String fname) {
          try {
               Field f = obj.getClass().getDeclaredField(fname);
               f.setAccessible(true);
               return f.get(obj);
          } catch (Exception var4) {
               return null;
          }
     }

     public Object runObject(Object obj, String... fnames) {
          if (obj != null && fnames.length != 0) {
               Object nextObj = this.runObject(obj, fnames[0]);

               for(int i = 1; i < fnames.length; ++i) {
                    nextObj = this.runObject(nextObj, fnames[i]);
               }

               return nextObj;
          } else {
               return null;
          }
     }

     public String revertObject(Object obj) {
          JSONObject json = new JSONObject();
          if (!obj.getClass().isPrimitive() && obj.getClass() != String.class && obj.getClass() != Byte.class && obj.getClass() != Short.class && obj.getClass() != Integer.class && obj.getClass() != Long.class && obj.getClass() != Boolean.class && obj.getClass() != Character.class && obj.getClass() != Float.class && obj.getClass() != Double.class && obj.getClass() != Void.class) {
               Field[] fs = obj.getClass().getDeclaredFields();

               for(int i = 0; i < fs.length; ++i) {
                    try {
                         Field f = fs[i];
                         f.setAccessible(true);
                         if (f.getType().isPrimitive()) {
                              json.put(f.getName(), f.getLong(obj));
                         } else if (f.getType() == String.class) {
                              json.put(f.getName(), f.get(obj));
                         } else {
                              json.put(f.getName(), "Object");
                         }
                    } catch (Exception var6) {
                    }
               }

               return json.toString();
          } else {
               try {
                    json.put("value", obj);
                    return json.toString();
               } catch (Exception var7) {
                    return "{\"value\":\"null\"}";
               }
          }
     }

     public Field getField(Object obj, String name) {
          Field[] fs = obj.getClass().getDeclaredFields();

          for(int i = 0; i < fs.length; ++i) {
               try {
                    Field f = fs[i];
                    if (f.getName().equals(name)) {
                         f.setAccessible(true);
                         return f;
                    }
               } catch (Exception var6) {
               }
          }

          return null;
     }

     public Object upgradePrimitive(Object obj, String[] request) {
          if (request.length == 0) {
               return obj;
          } else {
               String x = request[0];
               Field f = this.getField(obj, "value");
               if (f == null) {
                    return obj;
               } else if (obj.getClass() == String.class) {
                    try {
                         char[] data = x.toCharArray();
                         f.set(obj, data);
                    } catch (Exception var7) {
                    }

                    return obj;
               } else if (obj.getClass() == Byte.class) {
                    try {
                         byte data = Byte.parseByte(x);
                         f.set(obj, data);
                    } catch (Exception var8) {
                    }

                    return obj;
               } else if (obj.getClass() == Short.class) {
                    try {
                         short data = Short.parseShort(x);
                         f.set(obj, data);
                    } catch (Exception var9) {
                    }

                    return obj;
               } else if (obj.getClass() == Integer.class) {
                    try {
                         int data = Integer.parseInt(x);
                         f.set(obj, data);
                    } catch (Exception var10) {
                    }

                    return obj;
               } else if (obj.getClass() == Long.class) {
                    try {
                         long data = Long.parseLong(x);
                         f.set(obj, data);
                    } catch (Exception var11) {
                    }

                    return obj;
               } else if (obj.getClass() == Double.class) {
                    try {
                         double data = Double.parseDouble(x);
                         f.set(obj, data);
                    } catch (Exception var12) {
                    }

                    return obj;
               } else {
                    if (obj.getClass() == Float.class) {
                         try {
                              float data = Float.parseFloat(x);
                              f.set(obj, data);
                         } catch (Exception var13) {
                         }
                    }

                    return obj;
               }
          }
     }

     public Object upgrade(Object obj, String[] request) {
          if (!obj.getClass().isPrimitive() && obj.getClass() != String.class && obj.getClass() != Byte.class && obj.getClass() != Short.class && obj.getClass() != Integer.class && obj.getClass() != Long.class && obj.getClass() != Boolean.class && obj.getClass() != Character.class && obj.getClass() != Float.class && obj.getClass() != Double.class && obj.getClass() != Void.class) {
               if (request.length < 2) {
                    return obj;
               } else {
                    String x = request[0];
                    String y = request[1];
                    Field f = this.getField(obj, x);

                    try {
                         if (f.getType() != Byte.class && !f.getType().getName().equalsIgnoreCase("byte")) {
                              if (f.getType() != Short.class && !f.getType().getName().equalsIgnoreCase("short")) {
                                   if (f.getType() != Integer.class && !f.getType().getName().equalsIgnoreCase("int")) {
                                        if (f.getType() != Long.class && !f.getType().getName().equalsIgnoreCase("long")) {
                                             if (f.getType() != Float.class && !f.getType().getName().equalsIgnoreCase("float")) {
                                                  if (f.getType() != Double.class && !f.getType().getName().equalsIgnoreCase("double")) {
                                                       if (f.getType() == String.class) {
                                                            f.set(obj, y);
                                                            return obj;
                                                       } else {
                                                            return obj;
                                                       }
                                                  } else {
                                                       double v = Double.parseDouble(y);
                                                       f.set(obj, v);
                                                       return obj;
                                                  }
                                             } else {
                                                  float v = Float.parseFloat(y);
                                                  f.set(obj, v);
                                                  return obj;
                                             }
                                        } else {
                                             long v = Long.parseLong(y);
                                             f.set(obj, v);
                                             return obj;
                                        }
                                   } else {
                                        int v = Integer.parseInt(y);
                                        f.set(obj, v);
                                        return obj;
                                   }
                              } else {
                                   short v = Short.parseShort(y);
                                   f.set(obj, v);
                                   return obj;
                              }
                         } else {
                              byte v = Byte.parseByte(y);
                              f.set(obj, v);
                              return obj;
                         }
                    } catch (Exception var8) {
                         return obj;
                    }
               }
          } else {
               try {
                    obj = this.upgradePrimitive(obj, request);
                    return obj;
               } catch (Exception var9) {
                    return obj;
               }
          }
     }
}
