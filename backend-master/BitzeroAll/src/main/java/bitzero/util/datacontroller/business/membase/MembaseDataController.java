package bitzero.util.datacontroller.business.membase;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataControllerException;
import bitzero.util.datacontroller.business.IDataController;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;

public class MembaseDataController implements IDataController {
     static final Boolean NEW_DATABASE = ConfigHandle.instance().get("odservers") != null;
     MemcachedClient cacheCli;
     MemcachedClient dataCli;
     MemcachedClient new_dataCli;

     public MembaseDataController() {
          try {
               ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
               cfb.setOpQueueMaxBlockTime(ConfigHandle.instance().getLong("opsBlockTime"));
               cfb.setOpTimeout(ConfigHandle.instance().getLong("opsTimeOut"));
               this.new_dataCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses(ConfigHandle.instance().get("dservers")));
               this.cacheCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses(ConfigHandle.instance().get("cservers")));
               if (NEW_DATABASE) {
                    this.dataCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses(ConfigHandle.instance().get("odservers")));
               }
          } catch (IOException var2) {
               CommonHandle.writeErrLog((Throwable)var2);
          }

     }

     public Object get(String name) {
          Object obj = this.new_dataCli.get(name);
          return obj == null && NEW_DATABASE ? this.dataCli.get(name) : obj;
     }

     public Object getOldData(String name) {
          return this.dataCli.get(name);
     }

     public Object getNewData(String name) {
          return this.new_dataCli.get(name);
     }

     public Object getWithTranscode(String name) {
          return null;
     }

     public void set(String name, Object data) {
          this.new_dataCli.set(name, 0, data);
     }

     public void add(String name, Object data) {
          this.new_dataCli.add(name, 0, data);
     }

     public Object getCache(String name) {
          return this.cacheCli.get(name);
     }

     public void setCache(String name, int expire, Object data) {
          this.cacheCli.set(name, expire, data);
     }

     public Map multiget(List keys) {
          return this.new_dataCli.getBulk(keys);
     }

     public void delete(String name) throws DataControllerException {
          this.new_dataCli.delete(name);
     }

     public void deleteCache(String name) {
          this.cacheCli.delete(name);
     }
}
