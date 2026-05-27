package bitzero.server.entities.data;

import java.util.Collection;
import java.util.Iterator;

public interface ISFSArray {
     boolean contains(Object var1);

     Iterator iterator();

     Object getElementAt(int var1);

     SFSDataWrapper get(int var1);

     void removeElementAt(int var1);

     int size();

     byte[] toBinary();

     String toJson();

     String getHexDump();

     String getDump();

     String getDump(boolean var1);

     void addNull();

     void addBool(boolean var1);

     void addByte(byte var1);

     void addShort(short var1);

     void addInt(int var1);

     void addLong(long var1);

     void addFloat(float var1);

     void addDouble(double var1);

     void addUtfString(String var1);

     void addBoolArray(Collection var1);

     void addByteArray(byte[] var1);

     void addShortArray(Collection var1);

     void addIntArray(Collection var1);

     void addLongArray(Collection var1);

     void addFloatArray(Collection var1);

     void addDoubleArray(Collection var1);

     void addUtfStringArray(Collection var1);

     void addSFSArray(ISFSArray var1);

     void addSFSObject(ISFSObject var1);

     void addClass(Object var1);

     void add(SFSDataWrapper var1);

     boolean isNull(int var1);

     Boolean getBool(int var1);

     Byte getByte(int var1);

     Integer getUnsignedByte(int var1);

     Short getShort(int var1);

     Integer getInt(int var1);

     Long getLong(int var1);

     Float getFloat(int var1);

     Double getDouble(int var1);

     String getUtfString(int var1);

     Collection getBoolArray(int var1);

     byte[] getByteArray(int var1);

     Collection getUnsignedByteArray(int var1);

     Collection getShortArray(int var1);

     Collection getIntArray(int var1);

     Collection getLongArray(int var1);

     Collection getFloatArray(int var1);

     Collection getDoubleArray(int var1);

     Collection getUtfStringArray(int var1);

     Object getClass(int var1);

     ISFSArray getSFSArray(int var1);

     ISFSObject getSFSObject(int var1);
}
