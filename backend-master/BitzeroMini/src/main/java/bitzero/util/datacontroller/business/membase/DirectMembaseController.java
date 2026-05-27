package bitzero.util.datacontroller.business.membase;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataControllerException;
import bitzero.util.datacontroller.business.IDataController;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;

public class DirectMembaseController implements IDataController {
     private MemcachedClient dataCli;

     public DirectMembaseController(String game) {
          try {
               ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
               cfb.setOpQueueMaxBlockTime(ConfigHandle.instance().getLong("opsBlockTime"));
               cfb.setOpTimeout(ConfigHandle.instance().getLong("opsTimeOut"));
               this.dataCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses(ConfigHandle.instance().get(game)));
          } catch (IOException var3) {
               CommonHandle.writeErrLog((Throwable)var3);
          }

     }

     public Object get(String name) throws DataControllerException {
          return this.dataCli.get(name);
     }

     public void set(String name, Object data) throws DataControllerException {
          this.dataCli.set(name, 0, data);
     }

     public void add(String name, Object data) throws DataControllerException {
     }

     public Object getCache(String name) throws DataControllerException {
          return null;
     }

     public void setCache(String name, int expire, Object data) throws DataControllerException {
     }

     public Map multiget(List keys) {
          return Collections.emptyMap();
     }

     public void delete(String name) {
     }

     public void deleteCache(String name) {
     }

     public void shutdown() {
     }

     public long getCASValue(String name) {
          return 0L;
     }

     public CASValue getS(String name) {
          return null;
     }

     public CASResponse checkAndSet(String name, long casValue, Object data) {
          return null;
     }

     public Object getOldData(String name) {
          return this.dataCli.get(name);
     }

     public Object getNewData(String name) {
          return this.dataCli.get(name);
     }
}
