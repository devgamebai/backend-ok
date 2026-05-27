package bitzero.server.extensions.data;

import bitzero.server.util.ByteArray;

public class DataCmd {
     private short requestId = -1;
     private ByteArray _body;

     public DataCmd(byte[] databuffer) {
          this._body = new ByteArray(databuffer);
     }

     public short getId() {
          return this.requestId;
     }

     public void setId(short id) {
          this.requestId = id;
     }

     public byte[] getRawData() {
          return this._body.getBytes();
     }

     public String readString() {
          String s = "";

          try {
               s = this._body.readUTF();
          } catch (Exception var3) {
          }

          return s;
     }

     public byte readByte() {
          byte b = 0;

          try {
               b = this._body.readByte();
          } catch (Exception var3) {
          }

          return b;
     }

     public short readShort() {
          short b = 0;

          try {
               b = this._body.readShort();
          } catch (Exception var3) {
          }

          return b;
     }

     public int readInt() {
          int b = 0;

          try {
               b = this._body.readInt();
          } catch (Exception var3) {
          }

          return b;
     }

     public long readDouble() {
          long s = 0L;

          try {
               s = (long)this._body.readDouble();
          } catch (Exception var4) {
          }

          return s;
     }

     public long readLong() {
          long s = 0L;

          try {
               s = (long)this._body.readDouble();
          } catch (Exception var4) {
          }

          return s;
     }

     public boolean readBoolean() {
          boolean s = false;

          try {
               s = this._body.readBool();
          } catch (Exception var3) {
          }

          return s;
     }

     public byte[] readByteArray() {
          byte[] arrayInt = new byte[0];

          try {
               short lengthArray = this._body.readShort();
               arrayInt = this._body.readBytes(lengthArray);
          } catch (Exception var3) {
               var3.printStackTrace();
          }

          return arrayInt;
     }

     public int[] readIntArray() {
          int[] arrayInt = new int[0];

          try {
               short lengthArray = this._body.readShort();
               arrayInt = new int[lengthArray];

               for(int i = 0; i < lengthArray; ++i) {
                    arrayInt[i] = this._body.readInt();
               }
          } catch (Exception var4) {
               var4.printStackTrace();
          }

          return arrayInt;
     }

     public long[] readDoubleArray() {
          long[] arrayInt = new long[0];

          try {
               short lengthArray = this._body.readShort();
               arrayInt = new long[lengthArray];

               for(int i = 0; i < lengthArray; ++i) {
                    arrayInt[i] = (long)this._body.readDouble();
               }
          } catch (Exception var4) {
               var4.printStackTrace();
          }

          return arrayInt;
     }

     public long[] readLongArray() {
          return this.readDoubleArray();
     }

     public String[] readStringArray() {
          String[] arrayInt = new String[0];

          try {
               short lengthArray = this._body.readShort();
               arrayInt = new String[lengthArray];

               for(int i = 0; i < lengthArray; ++i) {
                    arrayInt[i] = this._body.readUTF();
               }
          } catch (Exception var4) {
               var4.printStackTrace();
          }

          return arrayInt;
     }

     public boolean[] readBooleanArray() {
          boolean[] arrayInt = new boolean[0];

          try {
               short lengthArray = this._body.readShort();
               arrayInt = new boolean[lengthArray];

               for(int i = 0; i < lengthArray; ++i) {
                    arrayInt[i] = this._body.readBool();
               }
          } catch (Exception var4) {
               var4.printStackTrace();
          }

          return arrayInt;
     }
}
