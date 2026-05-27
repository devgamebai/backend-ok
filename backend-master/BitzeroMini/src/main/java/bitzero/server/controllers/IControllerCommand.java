package bitzero.server.controllers;

import bitzero.engine.io.IRequest;

public interface IControllerCommand {
     boolean validate(IRequest var1) throws Exception;

     Object preProcess(IRequest var1) throws Exception;

     void execute(IRequest var1) throws Exception;

     void executeWebsocket(IRequest var1) throws Exception;
}
