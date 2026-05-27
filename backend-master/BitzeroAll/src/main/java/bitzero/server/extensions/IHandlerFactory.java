package bitzero.server.extensions;

public interface IHandlerFactory {
     void addHandler(short var1, Class var2);

     void removeHandler(short var1);

     Object findHandler(short var1) throws InstantiationException, IllegalAccessException;

     void addHandler(String var1, Class var2);

     void removeHandler(String var1);

     Object findHandler(String var1) throws InstantiationException, IllegalAccessException;

     void clearAll();
}
