package bitzero.server.controllers.admin.utils;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class PerformanceMonitor {
     private long lastSystemTime = 0L;
     private long lastProcessCpuTime = 0L;
     OperatingSystemMXBean osMxBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();

     public synchronized double getCpuUsage() {
          if (this.lastSystemTime == 0L) {
               this.baselineCounters();
               return 0.0D;
          } else {
               long systemTime = System.nanoTime();
               long processCpuTime = this.osMxBean.getProcessCpuTime();
               double cpuUsage = (double)(processCpuTime - this.lastProcessCpuTime) / (double)(systemTime - this.lastSystemTime);
               this.lastSystemTime = systemTime;
               this.lastProcessCpuTime = processCpuTime;
               return cpuUsage / (double)this.osMxBean.getAvailableProcessors();
          }
     }

     private void baselineCounters() {
          this.lastSystemTime = System.nanoTime();
          this.lastProcessCpuTime = this.osMxBean.getProcessCpuTime();
     }
}
