package bitzero.engine.core;

public interface IMethodDispatcher {
     void registerMethod(String var1, String var2);

     void unregisterKey(String var1);

     void callMethod(String var1, Object[] var2) throws Exception;
}
