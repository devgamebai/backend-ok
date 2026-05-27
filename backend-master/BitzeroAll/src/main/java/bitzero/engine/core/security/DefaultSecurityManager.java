package bitzero.engine.core.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSecurityManager implements ISecurityManager {
     private final List secureThreads = new ArrayList();
     private String name;
     private final Logger bootLogger = LoggerFactory.getLogger("bootLogger");

     public void init(Object o) {
          this.secureThreads.add(new EngineThread("bitzero.engine.controllers", ThreadComparisonType.STARTSWITH));
          this.bootLogger.info("Security Manager started");
     }

     public void destroy(Object o) {
          this.bootLogger.info("Security Manager stopped");
     }

     public String getName() {
          return this.name;
     }

     public void setName(String name) {
          this.name = name;
     }

     public void handleMessage(Object obj) {
     }

     public boolean isEngineThread(Thread thread) {
          boolean okay = false;
          String currThreadName = thread.getName();
          Iterator iterator = this.secureThreads.iterator();

          while(iterator.hasNext()) {
               IAllowedThread allowedThread = (IAllowedThread)iterator.next();
               if (allowedThread.getComparisonType() == ThreadComparisonType.STARTSWITH) {
                    if (currThreadName.startsWith(allowedThread.getName())) {
                         okay = true;
                         break;
                    }
               } else if (allowedThread.getComparisonType() == ThreadComparisonType.EXACT) {
                    if (currThreadName.equals(currThreadName)) {
                         okay = true;
                         break;
                    }
               } else if (allowedThread.getComparisonType() == ThreadComparisonType.ENDSWITH && currThreadName.endsWith(allowedThread.getName())) {
                    okay = true;
                    break;
               }
          }

          return okay;
     }
}
