package bitzero.engine.controllers;

import bitzero.engine.exceptions.RequestQueueFullException;
import bitzero.engine.io.IRequest;
import bitzero.engine.service.IService;

public interface IController extends IService {
     Object getId();

     void setId(Object var1);

     void enqueueRequest(IRequest var1) throws RequestQueueFullException;

     int getQueueSize();

     int getMaxQueueSize();

     void setMaxQueueSize(int var1);

     int getThreadPoolSize();

     void setThreadPoolSize(int var1);

     ControllerType getControllerType();
}
