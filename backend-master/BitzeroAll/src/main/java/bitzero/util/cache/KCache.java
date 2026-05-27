package bitzero.util.cache;

import java.util.Collection;
import java.util.Map;

public interface KCache {
     void put(Object var1, Object var2, int var3);

     Object get(Object var1);

     Map getAll(Collection var1);

     void clear();

     boolean remove(Object var1);

     Object removeAndGet(Object var1);

     int size();
}
