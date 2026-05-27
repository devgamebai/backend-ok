package bitzero.engine.core.security;

import bitzero.engine.service.IService;

public interface ISecurityManager extends IService {
     boolean isEngineThread(Thread var1);
}
