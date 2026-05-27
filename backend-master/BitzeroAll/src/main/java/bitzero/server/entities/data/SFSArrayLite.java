package bitzero.server.entities.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SFSArrayLite extends SFSArray {
     public static SFSArrayLite newInstance() {
          return new SFSArrayLite();
     }

     public Byte getByte(int index) {
          Integer i = super.getInt(index);
          return i != null ? i.byteValue() : null;
     }

     public Short getShort(int index) {
          Integer i = super.getInt(index);
          return i != null ? i.shortValue() : null;
     }

     public Float getFloat(int index) {
          Double d = super.getDouble(index);
          return d != null ? d.floatValue() : null;
     }

     public Collection getBoolArray(int key) {
          ISFSArray arr = this.getSFSArray(key);
          if (arr == null) {
               return null;
          } else {
               List data = new ArrayList();

               for(int i = 0; i < arr.size(); ++i) {
                    data.add(arr.getBool(i));
               }

               return data;
          }
     }

     public Collection getShortArray(int key) {
          ISFSArray arr = this.getSFSArray(key);
          if (arr == null) {
               return null;
          } else {
               List data = new ArrayList();

               for(int i = 0; i < arr.size(); ++i) {
                    data.add(arr.getInt(i).shortValue());
               }

               return data;
          }
     }

     public Collection getIntArray(int key) {
          ISFSArray arr = this.getSFSArray(key);
          if (arr == null) {
               return null;
          } else {
               List data = new ArrayList();

               for(int i = 0; i < arr.size(); ++i) {
                    data.add(arr.getInt(i));
               }

               return data;
          }
     }

     public Collection getFloatArray(int key) {
          ISFSArray arr = this.getSFSArray(key);
          if (arr == null) {
               return null;
          } else {
               List data = new ArrayList();

               for(int i = 0; i < arr.size(); ++i) {
                    data.add(arr.getDouble(i).floatValue());
               }

               return data;
          }
     }

     public Collection getDoubleArray(int key) {
          ISFSArray arr = this.getSFSArray(key);
          if (arr == null) {
               return null;
          } else {
               List data = new ArrayList();

               for(int i = 0; i < arr.size(); ++i) {
                    data.add(arr.getDouble(i));
               }

               return data;
          }
     }

     public Collection getUtfStringArray(int key) {
          ISFSArray arr = this.getSFSArray(key);
          if (arr == null) {
               return null;
          } else {
               List data = new ArrayList();

               for(int i = 0; i < arr.size(); ++i) {
                    data.add(arr.getUtfString(i));
               }

               return data;
          }
     }
}
