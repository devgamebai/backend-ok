package bitzero.server.util;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BinaryHelper {
     public static final byte CODE_SHORT_TYPE = 1;
     public static final byte CODE_INT_TYPE = 2;
     public static final byte CODE_CHAR_TYPE = 3;
     public static final byte CODE_FLOAT_TYPE = 4;
     public static final byte CODE_BOOLEAN_TYPE = 5;
     public static final byte CODE_STRING_TYPE = 6;
     public static final byte CODE_BYTE_TYPE = 7;
     public static final byte CODE_DOUBLE_TYPE = 8;
     public static final byte CODE_LONG_TYPE = 9;
     public static final byte CODE_SHORT_ARR_TYPE = 11;
     public static final byte CODE_INT_ARR_TYPE = 22;
     public static final byte CODE_CHAR_ARR_TYPE = 33;
     public static final byte CODE_FLOAT_ARR_TYPE = 44;
     public static final byte CODE_BOOLEAN_ARR_TYPE = 55;
     public static final byte CODE_STRING_ARR_TYPE = 66;
     public static final byte CODE_BYTE_ARR_TYPE = 77;
     public static final byte CODE_DOUBLE_ARR_TYPE = 88;
     public static final byte CODE_LONG_ARR_TYPE = 99;
     public static final byte CODE_OBJECT_TYPE = 100;
     public static final byte CODE_OBJECT_ARR_TYPE = 101;
     public static final byte CODE_NULL = -1;
     public static final short MAX_BYTE_FOR_STR = 50;
     private static Charset charset = Charset.forName("UTF-8");
     public static final int MAX_BUFFER_SIZE = 5012;

     public static byte findClassCodeType(Class c) {
          if (c == Short.class) {
               return 1;
          } else if (c == Integer.class) {
               return 2;
          } else if (c == Character.class) {
               return 3;
          } else if (c == Float.class) {
               return 4;
          } else if (c == Boolean.class) {
               return 5;
          } else if (c == String.class) {
               return 6;
          } else {
               return (byte)(c == Byte.class ? 7 : -1);
          }
     }

     public static byte findClassCode(Object c) {
          if (c instanceof Short) {
               return 1;
          } else if (c instanceof Integer) {
               return 2;
          } else if (c instanceof Character) {
               return 3;
          } else if (c instanceof Float) {
               return 4;
          } else if (c instanceof Double) {
               return 8;
          } else if (c instanceof Long) {
               return 9;
          } else if (c instanceof Boolean) {
               return 5;
          } else if (c instanceof String) {
               return 6;
          } else if (c instanceof Byte) {
               return 7;
          } else if (c instanceof short[]) {
               return 11;
          } else if (c instanceof int[]) {
               return 22;
          } else if (c instanceof char[]) {
               return 33;
          } else if (c instanceof float[]) {
               return 44;
          } else if (c instanceof boolean[]) {
               return 55;
          } else if (c instanceof String[]) {
               return 66;
          } else if (c instanceof Byte[]) {
               return 77;
          } else if (c instanceof double[]) {
               return 88;
          } else if (c instanceof long[]) {
               return 99;
          } else if (c instanceof Object[]) {
               return 101;
          } else {
               return (byte)(c instanceof Object ? 100 : -1);
          }
     }

     public static byte[] toByte(Object o) {
          Field[] fields = o.getClass().getFields();
          ByteBuffer buffer = ByteBuffer.allocateDirect(5012);
          int len = 0;
          Field[] var6 = fields;
          int var7 = fields.length;

          label68:
          for(int var8 = 0; var8 < var7; ++var8) {
               Field f = var6[var8];

               try {
                    Object value = f.get(o);
                    byte code = findClassCode(value);
                    Integer length;
                    int var17;
                    switch(code) {
                    case 1:
                         buffer.putShort((Short)value);
                         break;
                    case 2:
                         buffer.putInt((Integer)value);
                    case 3:
                    case 33:
                    default:
                         break;
                    case 4:
                         buffer.putFloat((Float)value);
                         break;
                    case 5:
                         buffer.put(toByte((Object)((Boolean)value)));
                         break;
                    case 6:
                         String s = (String)value;
                         byte[] temp = toByte(s);
                         length = temp.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(temp);
                         break;
                    case 7:
                         buffer.put((Byte)value);
                         break;
                    case 8:
                         buffer.putDouble((Double)value);
                         break;
                    case 9:
                         buffer.putDouble((double)(Long)value);
                         break;
                    case 11:
                         short[] shorts = (short[])((short[])value);
                         length = shorts.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(toByte(shorts));
                         break;
                    case 22:
                         int[] ints = (int[])((int[])value);
                         length = ints.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(toByte(ints));
                         break;
                    case 44:
                         float[] floats = (float[])((float[])value);
                         length = floats.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(toByte(floats));
                         break;
                    case 55:
                         boolean[] bools = (boolean[])((boolean[])value);
                         length = bools.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(toByte(bools));
                         break;
                    case 66:
                         String[] arrStr = (String[])((String[])value);
                         length = arrStr.length;
                         buffer.putShort(length.shortValue());
                         int i = 0;

                         while(true) {
                              if (i >= arrStr.length) {
                                   continue label68;
                              }

                              String str = arrStr[i];
                              byte[] tempB = toByte(str);
                              length = tempB.length;
                              buffer.putShort(length.shortValue());
                              if (length > 0) {
                                   buffer.put(tempB);
                              }

                              ++i;
                         }
                    case 77:
                         byte[] byteArr = (byte[])((byte[])value);
                         length = byteArr.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(byteArr);
                         break;
                    case 88:
                         double[] doubles = (double[])((double[])value);
                         length = doubles.length;
                         buffer.putShort(length.shortValue());
                         double[] var26 = doubles;
                         int var27 = doubles.length;
                         var17 = 0;

                         while(true) {
                              if (var17 >= var27) {
                                   continue label68;
                              }

                              double db = var26[var17];
                              buffer.putDouble(db);
                              ++var17;
                         }
                    case 99:
                         long[] longs = (long[])((long[])value);
                         length = longs.length;
                         buffer.putShort(length.shortValue());
                         long[] var16 = longs;
                         var17 = longs.length;

                         for(int var18 = 0; var18 < var17; ++var18) {
                              long db = var16[var18];
                              buffer.putDouble((double)db);
                         }
                    }
               } catch (IllegalAccessException var24) {
               }
          }

          byte[] ret = new byte[buffer.position()];
          buffer.rewind();
          buffer.get(ret);
          return ret;
     }

     public static byte[] toByte(short value) {
          byte[] b = new byte[]{(byte)(value >>> 8), (byte)value};
          return b;
     }

     public static byte[] toByte(short[] value) {
          byte[] b = new byte[value.length * 2];

          for(int i = 0; i < value.length; ++i) {
               System.arraycopy(toByte(value[i]), 0, b, i * 2, 2);
          }

          return b;
     }

     public static short toShort(byte[] b) {
          short value = 0;
           value = (short)(value + (b[0] << 8));
          value = (short)(value + (b[1] & 255));
          return value;
     }

     public static short[] toShortArray(byte[] b) {
          short[] shorts = new short[b.length / 2];

          for(int i = 0; i < shorts.length; ++i) {
               shorts[i] = toShort(new byte[]{b[i * 2], b[i * 2 + 1]});
          }

          return shorts;
     }

     public static byte[] toByte(int value) {
          byte[] b = new byte[]{(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
          return b;
     }

     public static byte[] toByte(int[] value) {
          byte[] b = new byte[value.length * 4];

          for(int i = 0; i < value.length; ++i) {
               System.arraycopy(toByte(value[i]), 0, b, i * 4, 4);
          }

          return b;
     }

     public static int toInt(byte[] b) {
          int value = 0;
           value = value + (b[0] << 24);
          value += (b[1] & 255) << 16;
          value += (b[2] & 255) << 8;
          value += b[3] & 255;
          return value;
     }

     public static int[] toIntArray(byte[] b) {
          int[] ints = new int[b.length / 4];

          for(int i = 0; i < ints.length; ++i) {
               ints[i] = toInt(new byte[]{b[i * 4], b[i * 4 + 1], b[i * 4 + 2], b[i * 4 + 3]});
          }

          return ints;
     }

     public static byte[] toByte(float value) {
          return toByte(Float.floatToIntBits(value));
     }

     public static byte[] toByte(float[] value) {
          byte[] b = new byte[value.length * 4];

          for(int i = 0; i < value.length; ++i) {
               System.arraycopy(toByte(value[i]), 0, b, i * 4, 4);
          }

          return b;
     }

     public static float toFloat(byte[] b) {
          return Float.intBitsToFloat(toInt(b));
     }

     public static float[] toFloatArray(byte[] b) {
          float[] floats = new float[b.length / 4];

          for(int i = 0; i < floats.length; ++i) {
               floats[i] = toFloat(new byte[]{b[i * 4], b[i * 4 + 1], b[i * 4 + 2], b[i * 4 + 3]});
          }

          return floats;
     }

     public static byte[] toByte(boolean value) {
          return new byte[]{(byte)(value ? 1 : 0)};
     }

     public static byte[] toByte(boolean[] value) {
          byte[] b = new byte[value.length];

          for(int i = 0; i < value.length; ++i) {
               b[i] = (byte)(value[i] ? 1 : 0);
          }

          return b;
     }

     public static void toObject(byte[] data, Object o, Object info) {
          Field[] fields = info.getClass().getFields();
          ByteBuffer buffer = ByteBuffer.wrap(data);
          Field[] var8 = fields;
          int var9 = fields.length;

          for(int var10 = 0; var10 < var9; ++var10) {
               Field f = var8[var10];

               try {
                    Object value = f.get(info);
                    byte code = findClassCode(value);
                    byte[] b;
                    short length;
                    switch(code) {
                    case 1:
                         f.setShort(o, buffer.getShort());
                         break;
                    case 2:
                         f.setInt(o, buffer.getInt());
                    case 3:
                    case 33:
                    default:
                         break;
                    case 4:
                         f.setFloat(o, buffer.getFloat());
                         break;
                    case 5:
                         f.setBoolean(o, toBool(buffer.get()));
                         break;
                    case 6:
                         length = buffer.getShort();
                         b = new byte[length];
                         buffer.get(b);
                         f.set(o, toString(b));
                         break;
                    case 7:
                         f.setByte(o, buffer.get());
                         break;
                    case 8:
                         f.setLong(o, (long)buffer.getDouble());
                         break;
                    case 9:
                         f.setLong(o, buffer.getLong());
                         break;
                    case 11:
                         length = buffer.getShort();
                         short[] tempSh = new short[length];

                         for(int i = 0; i < tempSh.length; ++i) {
                              tempSh[i] = (short)buffer.get();
                         }

                         f.set(o, tempSh);
                         break;
                    case 22:
                         length = buffer.getShort();
                         int[] temp = new int[length];

                         for(int i = 0; i < temp.length; ++i) {
                              temp[i] = buffer.getInt();
                         }

                         f.set(o, temp);
                         break;
                    case 44:
                         length = buffer.getShort();
                         float[] tempF = new float[length];

                         for(int i = 0; i < tempF.length; ++i) {
                              tempF[i] = buffer.getFloat();
                         }

                         f.set(o, tempF);
                         break;
                    case 55:
                         length = buffer.getShort();
                         boolean[] tempB = new boolean[length];

                         for(int i = 0; i < tempB.length; ++i) {
                              tempB[i] = toBool(buffer.get());
                         }

                         f.set(o, tempB);
                         break;
                    case 66:
                         length = buffer.getShort();
                         String[] arrString = new String[length];

                         for(int i = 0; i < arrString.length; ++i) {
                              short lenStr = buffer.getShort();
                              if (lenStr > 0) {
                                   b = new byte[lenStr];
                                   buffer.get(b);
                                   arrString[i] = toString(b);
                              } else {
                                   arrString[i] = "";
                              }
                         }

                         f.set(o, arrString);
                         break;
                    case 77:
                         length = buffer.getShort();
                         byte[] tempBa = new byte[length];

                         for(int i = 0; i < tempBa.length; ++i) {
                              tempBa[i] = buffer.get();
                         }

                         f.set(o, tempBa);
                         break;
                    case 88:
                         length = buffer.getShort();
                         long[] tempD = new long[length];

                         for(int i = 0; i < tempD.length; ++i) {
                              tempD[i] = (long)buffer.getDouble();
                         }

                         f.set(o, tempD);
                         break;
                    case 99:
                         length = buffer.getShort();
                         double[] tempL = new double[length];

                         for(int i = 0; i < tempL.length; ++i) {
                              tempL[i] = (double)buffer.getLong();
                         }

                         f.set(o, tempL);
                    }
               } catch (IllegalAccessException var23) {
               }
          }

     }

     public static boolean toBool(byte b) {
          return b != 0;
     }

     public static boolean[] toBoolArray(byte[] b) {
          boolean[] bools = new boolean[b.length];

          for(int i = 0; i < b.length; ++i) {
               bools[i] = b[i] != 0;
          }

          return bools;
     }

     public static byte[] toByte(char value) {
          return toByte((short)value);
     }

     public static byte[] toByte(char[] value) {
          byte[] b = new byte[value.length * 2];

          for(int i = 0; i < value.length; ++i) {
               System.arraycopy(toByte((short)value[i]), 0, b, i * 2, 2);
          }

          return b;
     }

     public static char toChar(byte[] b) {
          return (char)toShort(b);
     }

     public static char[] toCharArray(byte[] b) {
          char[] chars = new char[b.length / 2];

          for(int i = 0; i < chars.length; ++i) {
               chars[i] = (char)toShort(new byte[]{b[i * 2], b[i * 2 + 1]});
          }

          return chars;
     }

     public static byte[] toByte(String value) {
          return value.getBytes(charset);
     }

     public static byte[] toByte(String[] value) {
          byte[] b = new byte[value.length * 50];

          for(int i = 0; i < value.length; ++i) {
               byte[] temp = toByte(value[i]);
               System.arraycopy(temp, 0, b, 50 * i, temp.length);
          }

          return b;
     }

     public static String toString(byte[] b) {
          int len = b.length;

          for(int i = 0; i < b.length - 1; ++i) {
               if (b[i] == 0 && b[i + 1] == 0) {
                    len = i;
                    break;
               }
          }

          return new String(b, 0, len, charset);
     }

     public static String[] toStringArray(byte[] b) {
          String[] arrStr = new String[b.length / 50];

          for(int i = 0; i < arrStr.length; ++i) {
               byte[] temp = new byte[50];
               System.arraycopy(b, i * 50, temp, 0, 50);
               arrStr[i] = toString(temp);
          }

          return arrStr;
     }

     public static void main(String[] args) {
     }

     public static List getDeclaredFields(Object o, int modifier) {
          List fieldInfos = new ArrayList();
          Field[] fields = o.getClass().getDeclaredFields();

          for(int i = 0; i < fields.length; ++i) {
               Field f = fields[i];
               if (f.getModifiers() == modifier) {
                    fieldInfos.add(f);
               }
          }

          return fieldInfos;
     }

     public static List getFields(Object o, int modifier) {
          List fieldInfos = new ArrayList();
          Field[] fields = o.getClass().getFields();

          for(int i = 0; i < fields.length; ++i) {
               Field f = fields[i];
               if (f.getModifiers() == modifier) {
                    fieldInfos.add(f);
               }
          }

          return fieldInfos;
     }

     public static int sizeOf(Object o) {
          int ret = 1;
          List declaredFields = getFields(o, 1);
           ret = 0;
          Iterator var6 = declaredFields.iterator();

          label59:
          while(var6.hasNext()) {
               Field f = (Field)var6.next();

               try {
                    Object value = f.get(o);
                    byte code = findClassCode(value);
                    int length;
                    byte[] tempByte;
                    switch(code) {
                    case 1:
                         ret += 2;
                         break;
                    case 2:
                         ret += 4;
                    case 3:
                    case 33:
                    default:
                         break;
                    case 4:
                         ret += 4;
                         break;
                    case 5:
                         ++ret;
                         break;
                    case 6:
                         String s = (String)value;
                         tempByte = toByte(s);
                         ret += 2 + tempByte.length;
                         break;
                    case 7:
                         ++ret;
                         break;
                    case 8:
                         ret += 8;
                         break;
                    case 9:
                         ret += 8;
                         break;
                    case 11:
                         short[] shorts = (short[])((short[])value);
                         ret += 2 + 2 * shorts.length;
                         break;
                    case 22:
                         int[] ints = (int[])((int[])value);
                         ret += 2 + 4 * ints.length;
                         break;
                    case 44:
                         float[] floats = (float[])((float[])value);
                         ret += 2 + 4 * floats.length;
                         break;
                    case 55:
                         boolean[] bools = (boolean[])((boolean[])value);
                         ret += 2 + bools.length;
                         break;
                    case 66:
                         String[] arrStr = (String[])((String[])value);
                         length = arrStr.length;
                         ret += 2;
                         int i = 0;

                         while(true) {
                              if (i >= arrStr.length) {
                                   continue label59;
                              }

                              String str = arrStr[i];
                              tempByte = toByte(str);
                              length = tempByte.length;
                              if (length > 0) {
                                   ret += 2 + tempByte.length;
                              }

                              ++i;
                         }
                    case 77:
                         byte[] bas = (byte[])((byte[])value);
                         ret += 2 + 1 * bas.length;
                         break;
                    case 88:
                         double[] doubles = (double[])((double[])value);
                         ret += 2 + 8 * doubles.length;
                         break;
                    case 99:
                         long[] longs = (long[])((long[])value);
                         ret += 2 + 8 * longs.length;
                         break;
                    case 100:
                         byte[] tempObject = toByte(value);
                         ret += 2 + tempObject.length;
                         break;
                    case 101:
                         Object[] arrObject = (Object[])((Object[])value);
                         length = arrObject.length;
                         ret += 2;

                         for(int j = 0; j < arrObject.length; ++j) {
                              tempByte = toByte(arrObject[j]);
                              ret += 2 + tempByte.length;
                         }
                    }
               } catch (IllegalAccessException var21) {
               }
          }

          return ret;
     }
}
