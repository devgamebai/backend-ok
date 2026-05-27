package bitzero.server.util.deadlock;

import bitzero.util.common.business.CommonHandle;

public class DefaultDeadlockListener implements ThreadDeadlockDetector.Listener {
     public void deadlockDetected(Thread[] threads) {
          CommonHandle.writeErrLogDebug("-------------------");
          CommonHandle.writeErrLogDebug("Deadlocked Threads:");
          Thread[] var2 = threads;
          int var3 = threads.length;

          for(int var4 = 0; var4 < var3; ++var4) {
               Thread thread = var2[var4];
               CommonHandle.writeErrLogDebug(thread);
               StackTraceElement[] var6 = thread.getStackTrace();
               int var7 = var6.length;

               for(int var8 = 0; var8 < var7; ++var8) {
                    StackTraceElement ste = var6[var8];
                    CommonHandle.writeErrLogDebug(ste);
               }

               CommonHandle.writeErrLogDebug("");
          }

     }
}
