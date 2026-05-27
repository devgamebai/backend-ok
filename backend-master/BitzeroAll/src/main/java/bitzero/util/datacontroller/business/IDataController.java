package bitzero.util.datacontroller.business;

import java.util.List;
import java.util.Map;

public interface IDataController {
     Object get(String var1) throws DataControllerException;

     Map multiget(List var1) throws DataControllerException;

     void set(String var1, Object var2) throws DataControllerException;

     void add(String var1, Object var2) throws DataControllerException;

     void delete(String var1) throws DataControllerException;

     Object getCache(String var1) throws DataControllerException;

     void setCache(String var1, int var2, Object var3) throws DataControllerException;

     void deleteCache(String var1) throws DataControllerException;
}
