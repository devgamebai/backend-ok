package bitzero.engine.controllers;

import bitzero.engine.service.IService;

public interface IControllerManager extends IService {
     IController getControllerById(Object var1);

     void addController(Object var1, IController var2);

     void removeController(Object var1);
}
