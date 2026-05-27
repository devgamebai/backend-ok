package bitzero.util.datacontroller.business.membase;

import bitzero.util.datacontroller.business.DataControllerException;
import bitzero.util.datacontroller.business.IDataController;
import com.vinplay.dal.service.CacheService;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HazelCastController implements IDataController {
     private CacheService cacheService = new CacheServiceImpl();

     public Object get(String name) throws DataControllerException {
          try {
               return this.cacheService.getObject(name);
          } catch (KeyNotFoundException var3) {
               return null;
          } catch (Exception var4) {
               throw new DataControllerException(var4);
          }
     }

     public Map multiget(List keys) throws DataControllerException {
          try {
               Set keysUnique = new HashSet();
               keysUnique.addAll(keys);
               return this.cacheService.getBulk(keysUnique);
          } catch (Exception var3) {
               throw new DataControllerException(var3);
          }
     }

     public void set(String name, Object data) throws DataControllerException {
          try {
               this.cacheService.setObject(name, data);
          } catch (Exception var4) {
               throw new DataControllerException(var4);
          }
     }

     public void add(String name, Object data) throws DataControllerException {
          try {
               this.cacheService.setObject(name, data);
          } catch (Exception var4) {
               throw new DataControllerException(var4);
          }
     }

     public void delete(String name) throws DataControllerException {
          try {
               this.cacheService.removeObject(name);
          } catch (Exception var3) {
               throw new DataControllerException(var3);
          }
     }

     public Object getCache(String name) throws DataControllerException {
          try {
               return this.cacheService.getObject(name);
          } catch (KeyNotFoundException var3) {
               return null;
          } catch (Exception var4) {
               throw new DataControllerException(var4);
          }
     }

     public void setCache(String name, int expire, Object data) throws DataControllerException {
          try {
               this.cacheService.setObject(name, expire, data);
          } catch (Exception var5) {
               throw new DataControllerException(var5);
          }
     }

     public void deleteCache(String name) throws DataControllerException {
          try {
               this.cacheService.removeObject(name);
          } catch (Exception var3) {
               throw new DataControllerException(var3);
          }
     }
}
