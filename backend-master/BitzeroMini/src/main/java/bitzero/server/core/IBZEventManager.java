package bitzero.server.core;

import bitzero.engine.service.IService;
import java.util.concurrent.Executor;

public interface IBZEventManager extends IBZEventDispatcher, IService {
     void setThreadPoolSize(int var1);

     void dispatchImmediateEvent(IBZEvent var1);

     Executor getThreadPool();
}
