package bitzero.server.extensions.data;

import bitzero.server.BitZeroServer;
import bitzero.server.util.BinaryHelper;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import org.slf4j.LoggerFactory;

public class BaseMsg {
     public Byte Error = 0;
     protected short Id = 0;

     public BaseMsg(short type) {
          this.Id = type;
          this.Error = 0;
     }

     protected BaseMsg(short type, int error) {
          this.Id = type;
          this.Error = (byte)error;
     }

     public short getId() {
          return this.Id;
     }

     protected ByteBuffer makeBuffer() {
          ByteBuffer bf = ByteBuffer.allocate(BitZeroServer.getInstance().getConfigurator().getCoreSettings().maxPacketBufferSize);
          bf.put(this.Error);
          return bf;
     }

     protected byte[] packBuffer(ByteBuffer bf) {
          int pos = bf.position();
          byte[] result = new byte[pos];
          bf.flip();
          bf.get(result, 0, pos);
          return result;
     }

     protected void putStr(ByteBuffer bf, String value) {
          byte[] tempByte = BinaryHelper.toByte(value);
          Integer length = tempByte.length;
          bf.putShort(length.shortValue());
          bf.put(tempByte);
     }

     protected void putBoolean(ByteBuffer bf, Boolean b) {
          boolean tempB = b;
          bf.put(BinaryHelper.toByte(tempB));
     }

     protected void putLong(ByteBuffer bf, long value) {
          bf.putLong(value);
     }

     protected void putIntArray(ByteBuffer bf, int[] value) {
          int[] ints = (int[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          bf.put(BinaryHelper.toByte(ints));
     }

     protected void putLongArray(ByteBuffer bf, long[] value) {
          long[] longs = (long[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          long[] var5 = longs;
          int var6 = longs.length;

          for(int var7 = 0; var7 < var6; ++var7) {
               long db = var5[var7];
               bf.putLong(db);
          }

     }

     protected void putFloatArray(ByteBuffer bf, float[] value) {
          float[] longs = (float[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          bf.put(BinaryHelper.toByte(longs));
     }

     protected void putDoubleArray(ByteBuffer bf, double[] value) {
          double[] longs = (double[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          bf.put(BinaryHelper.toByte((Object)longs));
     }

     protected void putBooleanArray(ByteBuffer bf, boolean[] value) {
          boolean[] longs = (boolean[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          bf.put(BinaryHelper.toByte(longs));
     }

     protected void putShortArray(ByteBuffer bf, short[] value) {
          short[] longs = (short[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          bf.put(BinaryHelper.toByte(longs));
     }

     protected void putByteArray(ByteBuffer bf, byte[] value) {
          byte[] longs = (byte[])value;
          Integer length = value.length;
          bf.putShort(length.shortValue());
          bf.put(longs);
     }

     protected void putStringArray(ByteBuffer bf, String[] value) {
          String[] arrStr = (String[])value;
          Integer length = arrStr.length;
          bf.putShort(length.shortValue());

          for(int i = 0; i < arrStr.length; ++i) {
               String str = arrStr[i];
               byte[] tempByte = BinaryHelper.toByte(str);
               length = tempByte.length;
               bf.putShort(length.shortValue());
               if (length > 0) {
                    bf.put(tempByte);
               }
          }

     }

     public byte[] createData() {
          Field[] fields = new Field[this.getClass().getFields().length];

          try {
               fields[0] = this.getClass().getField("Error");
          } catch (NoSuchFieldException var25) {
               return null;
          }

          List declaredFields = BinaryHelper.getDeclaredFields(this, 1);

          for(int i = 0; i < declaredFields.size(); ++i) {
               fields[i + 1] = (Field)declaredFields.get(i);
          }

          ByteBuffer buffer = ByteBuffer.allocate(BinaryHelper.sizeOf(this));
          Field[] var7 = fields;
          int var8 = fields.length;

          label100:
          for(int var9 = 0; var9 < var8; ++var9) {
               Field f = var7[var9];

               try {
                    Object value = f.get(this);
                    byte code = BinaryHelper.findClassCode(value);
                    byte[] tempByte;
                    Integer length;
                    int var19;
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
                         boolean tempB = (Boolean)value;
                         buffer.put(BinaryHelper.toByte(tempB));
                         break;
                    case 6:
                         String s = (String)value;
                         tempByte = BinaryHelper.toByte(s);
                         length = tempByte.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(tempByte);
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
                         buffer.put(BinaryHelper.toByte(shorts));
                         break;
                    case 22:
                         int[] ints = (int[])((int[])value);
                         length = ints.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(BinaryHelper.toByte(ints));
                         break;
                    case 44:
                         float[] floats = (float[])((float[])value);
                         length = floats.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(BinaryHelper.toByte(floats));
                         break;
                    case 55:
                         boolean[] bools = (boolean[])((boolean[])value);
                         length = bools.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(BinaryHelper.toByte(bools));
                         break;
                    case 66:
                         String[] arrStr = (String[])((String[])value);
                         length = arrStr.length;
                         buffer.putShort(length.shortValue());
                         int i = 0;

                         while(true) {
                              if (i >= arrStr.length) {
                                   continue label100;
                              }

                              String str = arrStr[i];
                              tempByte = BinaryHelper.toByte(str);
                              length = tempByte.length;
                              buffer.putShort(length.shortValue());
                              if (length > 0) {
                                   buffer.put(tempByte);
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
                         double[] var30 = doubles;
                         int var31 = doubles.length;
                         var19 = 0;

                         while(true) {
                              if (var19 >= var31) {
                                   continue label100;
                              }

                              double db = var30[var19];
                              buffer.putDouble(db);
                              ++var19;
                         }
                    case 99:
                         long[] longs = (long[])((long[])value);
                         length = longs.length;
                         buffer.putShort(length.shortValue());
                         long[] var18 = longs;
                         var19 = longs.length;
                         int var20 = 0;

                         while(true) {
                              if (var20 >= var19) {
                                   continue label100;
                              }

                              long db = var18[var20];
                              buffer.putLong(db);
                              ++var20;
                         }
                    case 100:
                         byte[] tempObject = BinaryHelper.toByte(value);
                         length = tempObject.length;
                         buffer.putShort(length.shortValue());
                         buffer.put(tempObject);
                         break;
                    case 101:
                         Object[] arrObject = (Object[])((Object[])value);
                         length = arrObject.length;
                         buffer.putShort(length.shortValue());

                         for(int j = 0; j < arrObject.length; ++j) {
                              tempByte = BinaryHelper.toByte(arrObject[j]);
                              length = tempByte.length;
                              buffer.putShort(length.shortValue());
                              buffer.put(tempByte);
                         }
                    }
               } catch (IllegalAccessException var26) {
                    if (LoggerFactory.getLogger("request").isDebugEnabled()) {
                         var26.printStackTrace();
                    }
               } catch (Exception var27) {
                    if (LoggerFactory.getLogger("request").isDebugEnabled()) {
                         var27.printStackTrace();
                    }
               }
          }

          byte[] ret = new byte[buffer.position()];
          buffer.rewind();
          buffer.get(ret);
          return ret;
     }
}
