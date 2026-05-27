package bitzero.server.entities.data;

import bitzero.engine.util.ByteUtils;
import bitzero.server.protocol.serialization.DefaultObjectDumpFormatter;
import bitzero.server.protocol.serialization.DefaultSFSDataSerializer;
import bitzero.server.protocol.serialization.ISFSDataSerializer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SFSObject implements ISFSObject {
     private Map dataHolder = new ConcurrentHashMap();
     private ISFSDataSerializer serializer = DefaultSFSDataSerializer.getInstance();

     public static SFSObject newFromObject(Object o) {
          return (SFSObject)DefaultSFSDataSerializer.getInstance().pojo2sfs(o);
     }

     public static SFSObject newFromBinaryData(byte[] bytes) {
          return (SFSObject)DefaultSFSDataSerializer.getInstance().binary2object(bytes);
     }

     public static ISFSObject newFromJsonData(String jsonStr) {
          return DefaultSFSDataSerializer.getInstance().json2object(jsonStr);
     }

     public static SFSObject newFromResultSet(ResultSet rset) throws SQLException {
          return DefaultSFSDataSerializer.getInstance().resultSet2object(rset);
     }

     public static SFSObject newInstance() {
          return new SFSObject();
     }

     public Iterator iterator() {
          return this.dataHolder.entrySet().iterator();
     }

     public boolean containsKey(String key) {
          return this.dataHolder.containsKey(key);
     }

     public boolean removeElement(String key) {
          return this.dataHolder.remove(key) != null;
     }

     public int size() {
          return this.dataHolder.size();
     }

     public byte[] toBinary() {
          return this.serializer.object2binary(this);
     }

     public String toJson() {
          return this.serializer.object2json(this.flatten());
     }

     public String getDump() {
          return this.size() == 0 ? "[ Empty SFSObject ]" : DefaultObjectDumpFormatter.prettyPrintDump(this.dump());
     }

     public String getDump(boolean noFormat) {
          return !noFormat ? this.dump() : this.getDump();
     }

     private String dump() {
          StringBuilder buffer = new StringBuilder();
          buffer.append('{');

          for(Iterator var2 = this.getKeys().iterator(); var2.hasNext(); buffer.append(';')) {
               String key = (String)var2.next();
               SFSDataWrapper wrapper = this.get(key);
               buffer.append("(").append(wrapper.getTypeId().name().toLowerCase()).append(") ").append(key).append(": ");
               if (wrapper.getTypeId() == SFSDataType.SFS_OBJECT) {
                    buffer.append(((SFSObject)wrapper.getObject()).getDump(false));
               } else if (wrapper.getTypeId() == SFSDataType.SFS_ARRAY) {
                    buffer.append(((SFSArray)wrapper.getObject()).getDump(false));
               } else if (wrapper.getTypeId() == SFSDataType.BYTE_ARRAY) {
                    buffer.append(DefaultObjectDumpFormatter.prettyPrintByteArray((byte[])((byte[])wrapper.getObject())));
               } else if (wrapper.getTypeId() == SFSDataType.CLASS) {
                    buffer.append(wrapper.getObject().getClass().getName());
               } else {
                    buffer.append(wrapper.getObject());
               }
          }

          buffer.append('}');
          return buffer.toString();
     }

     public String getHexDump() {
          return ByteUtils.fullHexDump(this.toBinary());
     }

     public boolean isNull(String key) {
          SFSDataWrapper wrapper = (SFSDataWrapper)this.dataHolder.get(key);
          if (wrapper == null) {
               return false;
          } else {
               return wrapper.getTypeId() == SFSDataType.NULL;
          }
     }

     public SFSDataWrapper get(String key) {
          return (SFSDataWrapper)this.dataHolder.get(key);
     }

     public Boolean getBool(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Boolean)o.getObject();
     }

     public Collection getBoolArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public Byte getByte(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Byte)o.getObject();
     }

     public byte[] getByteArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (byte[])((byte[])o.getObject());
     }

     public Double getDouble(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Double)o.getObject();
     }

     public Collection getDoubleArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public Float getFloat(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Float)o.getObject();
     }

     public Collection getFloatArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public Integer getInt(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Integer)o.getObject();
     }

     public Collection getIntArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public Set getKeys() {
          return this.dataHolder.keySet();
     }

     public Long getLong(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Long)o.getObject();
     }

     public Collection getLongArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public ISFSArray getSFSArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (ISFSArray)o.getObject();
     }

     public ISFSObject getSFSObject(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (ISFSObject)o.getObject();
     }

     public Short getShort(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Short)o.getObject();
     }

     public Collection getShortArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public Integer getUnsignedByte(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : DefaultSFSDataSerializer.getInstance().getUnsignedByte((Byte)o.getObject());
     }

     public Collection getUnsignedByteArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          if (o == null) {
               return null;
          } else {
               DefaultSFSDataSerializer serializer = DefaultSFSDataSerializer.getInstance();
               Collection intCollection = new ArrayList();
               byte[] arrayOfByte;
               int j = (arrayOfByte = (byte[])((byte[])o.getObject())).length;

               for(int i = 0; i < j; ++i) {
                    byte b = arrayOfByte[i];
                    intCollection.add(serializer.getUnsignedByte(b));
               }

               return intCollection;
          }
     }

     public String getUtfString(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (String)o.getObject();
     }

     public Collection getUtfStringArray(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : (Collection)o.getObject();
     }

     public Object getClass(String key) {
          SFSDataWrapper o = (SFSDataWrapper)this.dataHolder.get(key);
          return o == null ? null : o.getObject();
     }

     public void putBool(String key, boolean value) {
          this.putObj(key, value, SFSDataType.BOOL);
     }

     public void putBoolArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.BOOL_ARRAY);
     }

     public void putByte(String key, byte value) {
          this.putObj(key, value, SFSDataType.BYTE);
     }

     public void putByteArray(String key, byte[] value) {
          this.putObj(key, value, SFSDataType.BYTE_ARRAY);
     }

     public void putDouble(String key, double value) {
          this.putObj(key, value, SFSDataType.DOUBLE);
     }

     public void putDoubleArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.DOUBLE_ARRAY);
     }

     public void putFloat(String key, float value) {
          this.putObj(key, value, SFSDataType.FLOAT);
     }

     public void putFloatArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.FLOAT_ARRAY);
     }

     public void putInt(String key, int value) {
          this.putObj(key, value, SFSDataType.INT);
     }

     public void putIntArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.INT_ARRAY);
     }

     public void putLong(String key, long value) {
          this.putObj(key, value, SFSDataType.LONG);
     }

     public void putLongArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.LONG_ARRAY);
     }

     public void putNull(String key) {
          this.dataHolder.put(key, new SFSDataWrapper(SFSDataType.NULL, (Object)null));
     }

     public void putSFSArray(String key, ISFSArray value) {
          this.putObj(key, value, SFSDataType.SFS_ARRAY);
     }

     public void putSFSObject(String key, ISFSObject value) {
          this.putObj(key, value, SFSDataType.SFS_OBJECT);
     }

     public void putShort(String key, short value) {
          this.putObj(key, value, SFSDataType.SHORT);
     }

     public void putShortArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.SHORT_ARRAY);
     }

     public void putUtfString(String key, String value) {
          this.putObj(key, value, SFSDataType.UTF_STRING);
     }

     public void putUtfStringArray(String key, Collection value) {
          this.putObj(key, value, SFSDataType.UTF_STRING_ARRAY);
     }

     public void put(String key, SFSDataWrapper wrappedObject) {
          this.putObj(key, wrappedObject, (SFSDataType)null);
     }

     public void putClass(String key, Object o) {
          this.putObj(key, o, SFSDataType.CLASS);
     }

     public String toString() {
          return "[SFSObject, size: " + this.size() + "]";
     }

     private void putObj(String key, Object value, SFSDataType typeId) {
          if (key == null) {
               throw new IllegalArgumentException("SFSObject requires a non-null key for a 'put' operation!");
          } else if (key.length() > 255) {
               throw new IllegalArgumentException("SFSObject keys must be less than 255 characters!");
          } else if (value == null) {
               throw new IllegalArgumentException("SFSObject requires a non-null value! If you need to add a null use the putNull() method.");
          } else {
               if (value instanceof SFSDataWrapper) {
                    this.dataHolder.put(key, (SFSDataWrapper)value);
               } else {
                    this.dataHolder.put(key, new SFSDataWrapper(typeId, value));
               }

          }
     }

     private Map flatten() {
          Map map = new HashMap();
          DefaultSFSDataSerializer.getInstance().flattenObject(map, this);
          return map;
     }
}
