package bitzero.server.entities.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SFSObjectLite extends SFSObject {
     public static SFSObject newInstance() {
          return new SFSObjectLite();
     }

     public Byte getByte(String key) {
          Integer i = super.getInt(key);
          return i != null ? i.byteValue() : null;
     }

     public Short getShort(String key) {
          Integer i = super.getInt(key);
          return i != null ? i.shortValue() : null;
     }

     public Float getFloat(String key) {
          Double d = super.getDouble(key);
          return d != null ? d.floatValue() : null;
     }

     public Collection getBoolArray(String key) {
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

     public Collection getShortArray(String key) {
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

     public Collection getIntArray(String key) {
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

     public Collection getFloatArray(String key) {
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

     public Collection getDoubleArray(String key) {
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

     public Collection getUtfStringArray(String key) {
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
